
import requests,json
from requests.auth import HTTPBasicAuth

USERNAME='admin'
PASSWORD='admin'
REGISTER_EP_URL="http://%s:8181/restconf/operations/endpoint:register-endpoint"
REGISTER_TENANTS_URL="http://%s:8181/restconf/config/policy:tenants"
REGISTER_NODES_URL="http://%s:8181/restconf/config/opendaylight-inventory:nodes"

endpointGroups = {}

def get_epg(tenantId, epgId):
    k = "{}|{}".format(tenantId,epgId)
    if k in endpointGroups:
        return endpointGroups[k]
    tenant = get_tenant(tenantId);
    data = {
        "id": epgId,
        "consumer-named-selector": [],
        "provider-named-selector": []
    }
    tenant["endpoint-group"].append(data)
    endpointGroups[k] = data
    return data

tenants = {}

def get_tenant(tenantId):
    if tenantId in tenants: 
        return tenants[tenantId]
    data = {
        "id": tenantId,
        "l3-context": [],
        "l2-bridge-domain": [],
        "l2-flood-domain": [],
        "subnet": [],
        "endpoint-group": [],
        "contract": [],
        "subject-feature-instances": {
            "classifier-instance": [
                {"name": "http-dest",
                "classifier-definition-id": "4250ab32-e8b8-445a-aebb-e1bd2cdd291f",
                "parameter-value": [
                    {"name": "type",
                     "string-value": "TCP"}, 
                    {"name": "destport",
                     "int-value": "80"}
                ]},
                {"name": "http-src",
                "classifier-definition-id": "4250ab32-e8b8-445a-aebb-e1bd2cdd291f",
                "parameter-value": [
                    {"name": "type",
                     "string-value": "TCP"}, 
                    {"name": "sourceport",
                     "int-value": "80"}
                ]},
                {"name": "icmp",
                "classifier-definition-id": "79c6fdb2-1e1a-4832-af57-c65baf5c2335",
                "parameter-value": [
                    {"name": "proto",
                     "int-value": "1"}
                ]},
            ]
        }
    }
    tenants[tenantId] = data
    return data

subnets = {}

def get_fd(tenantId, fdId, parent):
    tenant = get_tenant(tenantId)
    data = {"id": fdId, 
            "parent": parent}
    tenant["l2-flood-domain"].append(data)
    return data

def get_bd(tenantId, bdId, parent):
    tenant = get_tenant(tenantId)
    data = {"id": bdId, 
            "parent": parent}
    tenant["l2-bridge-domain"].append(data)
    return data

def get_l3c(tenantId, l3cId):
    tenant = get_tenant(tenantId)
    data = {"id": l3cId}
    tenant["l3-context"].append(data)
    return data

def get_subnet(tenantId, subnetId, parent, prefix, router):
    k = "{}|{}".format(tenantId, subnetId)
    if k in subnets:
        return subnets[k]
    tenant = get_tenant(tenantId)
    data = {"id": subnetId, 
            "parent": parent,
            "ip-prefix": prefix,
            "virtual-router-ip": router}
    tenant["subnet"].append(data)
    return data

endpoints = []

def get_ep(tenantId, groupId, l3ctx, ip, l2ctx, mac, sw, port):
    group = get_epg(tenantId, groupId)
    data = {"tenant": tenantId,
            "endpoint-group": groupId,
            "l2-context": l2ctx, 
            "mac-address": mac, 
            "l3-address": [{"l3-context": l3ctx,
                            "ip-address": ip}], 
            "ofoverlay:node-id": "openflow:{}".format(sw), 
            "ofoverlay:node-connector-id": "openflow:{}:{}".format(sw, port)
        }
    endpoints.append(data)
    return data

nodes = []

def get_node_config(sw, tun_ip):
    data = {
        "id": "openflow:{}".format(sw),
        "ofoverlay:tunnel-ip": tun_ip
    }
    nodes.append(data)
    return data

def get_contract(tenantId, pgroupId, cgroupId, contractId):
    tenant = get_tenant(tenantId)
    pgroup = get_epg(tenantId, pgroupId)
    cgroup = get_epg(tenantId, cgroupId)
    data = {
        "id": contractId,
        "subject": [{"name": "allow-http-subject",
                     "rule": [
                         {"name": "allow-http-rule",
                          "classifier-ref": [
                              {"name": "http-dest",
                               "direction": "in"},
                              {"name": "http-src",
                               "direction": "out"}
                          ]}
                     ]},
                    {"name": "allow-icmp-subject",
                     "rule": [
                         {"name": "allow-icmp-rule",
                          "classifier-ref": [
                              {"name": "icmp"}
                          ]}
                     ]}],
        "clause": [{"name": "allow-http-clause",
                    "subject-refs": ["allow-http-subject", 
                                     "allow-icmp-subject"]}]
    }
    tenant["contract"].append(data)
    cgroup["consumer-named-selector"].append({
        "name": "{}-{}-{}".format(pgroupId, cgroupId, contractId),
        "contract": [contractId]
    })
    pgroup["provider-named-selector"].append({
        "name": "{}-{}-{}".format(pgroupId, cgroupId, contractId),
        "contract": [contractId]
    })

    return data

def post(url, data):
    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    print "POST %s" % url
    print json.dumps(data, indent=4, sort_keys=True)
    r = requests.post(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    print r.text
    r.raise_for_status()

def put(url, data):
    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    print "PUT %s" % url
    print json.dumps(data, indent=4, sort_keys=True)
    r = requests.put(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    print r.text
    r.raise_for_status()

def register_tenants(contHost):
    data = {"policy:tenants": {"tenant": tenants.values()}}
    put(REGISTER_TENANTS_URL % contHost, data)

def register_eps(contHost):
    for ep in endpoints:
       data = {"input": ep}
       post(REGISTER_EP_URL % contHost, data)

def register_nodes(contHost):
    data = {"opendaylight-inventory:nodes": {"node": nodes}}
    put(REGISTER_NODES_URL % contHost, data)
