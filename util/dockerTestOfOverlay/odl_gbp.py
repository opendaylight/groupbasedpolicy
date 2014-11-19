
import requests,json
import uuid
import sys
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

def initialize_tenant(tenant):
    # All tenants must have unique ID
    if not tenant.has_key('id'):
        print "No ID, initializing"
        tenant['id']=str(uuid.uuid4())

    # If the tenant has already been initialised, we must assume that the stored copy in 
    # tenants dict is more up to date.
    if tenant['id'] in tenants:
        return tenants[tenant['id']]

    # Dictionary items that must exist
    data = {
        "l3-context": [],
        "l2-bridge-domain": [],
        "l2-flood-domain": [],
        "subnet": [],
        "endpoint-group": [],
        "contract": [],
        "subject-feature-instances": {}
    }

    # This merges the base data dictionary with the passed tenant dictionary, and assumes that 
    # over-riding anything in data with tenant is preferred, if not, order must be reversed
    mergedData = dict(data.items() + tenant.items())
    tenants[mergedData['id']] = mergedData
    return mergedData

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
        "subject-feature-instances": {}
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

def get_contract(tenantId, pgroupId, cgroupId, contract):
    tenant = get_tenant(tenantId)
    pgroup = get_epg(tenantId, pgroupId)
    cgroup = get_epg(tenantId, cgroupId)

    if not contract.has_key('id'):
        contract['id']=str(uuid.uuid4())
    # tenant's contract construct has no idea of "name" so creating a copy of the contract dict,
    # removing name altogether, and using that
    data=dict(contract)
    del data['name']

    if not tenant["contract"]:
        tenant["contract"].append(data)
    else:
        contractExists=False
        for tenantContract in tenant["contract"]:
            if tenantContract["id"] == data["id"]: contractExists=True
        if not contractExists:
            tenant["contract"].append(data)
    name="{}-{}-{}".format(pgroupId, cgroupId, data['id'])
    if not cgroup["consumer-named-selector"]:
        cgroup["consumer-named-selector"].append({"name": name, "contract": [data['id']]})
    else:
        contractExists=False
        for consumerContract in cgroup["consumer-named-selector"]:
            if consumerContract["contract"][0] == data["id"]:
                contractExists=True
        if not contractExists:
            cgroup["consumer-named-selector"].append({"name": name,"contract": [data['id']]})
    if not pgroup["provider-named-selector"]:
        pgroup["provider-named-selector"].append({"name": name,"contract": [data['id']]})
    else:
        contractExists=False
        for providerContract in pgroup["provider-named-selector"]:
            if providerContract["contract"][0] == data["id"]:
                contractExists=True
        if not contractExists:
            pgroup["provider-named-selector"].append({"name": name,"contract": [data['id']]})
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
