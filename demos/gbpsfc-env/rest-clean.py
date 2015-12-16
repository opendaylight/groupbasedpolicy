#!/usr/bin/python
import argparse
import requests,json
from requests.auth import HTTPBasicAuth
from subprocess import call
import time
import sys
import os


DEFAULT_PORT='8181'


USERNAME='admin'
PASSWORD='admin'


OPER_NODES='/restconf/operational/opendaylight-inventory:nodes/'
CONF_TENANT='/restconf/config/policy:tenants'

def get(host, port, uri):
    url='http://'+host+":"+port+uri
    r = requests.get(url, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    return r

def rest_delete(host, port, uri, debug=False):
    '''Perform a DELETE rest operation, using the URL and data provided'''
    url='http://'+host+":"+port+uri
    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "DELETE %s" % url
    r = requests.delete(url, headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
    r.raise_for_status()

def post(host, port, uri, data, debug=False):
    '''Perform a POST rest operation, using the URL and data provided'''
    url='http://'+host+":"+port+uri
    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "POST %s" % url
        print json.dumps(data, indent=4, sort_keys=True)
    r = requests.post(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
    r.raise_for_status()

def get_service_functions_uri():
    return "/restconf/config/service-function:service-functions"

def get_service_function_forwarders_uri():
    return "/restconf/config/service-function-forwarder:service-function-forwarders"

def get_service_function_chains_uri():
    return "/restconf/config/service-function-chain:service-function-chains/"

def get_service_function_paths_uri():
    return "/restconf/config/service-function-path:service-function-paths/"

def get_tenant_uri():
    return "/restconf/config/policy:tenants/policy:tenant/tenant-red"

def get_tunnel_uri():
    return "/restconf/config/opendaylight-inventory:nodes"

def get_endpoint_uri():
    return "/restconf/operations/endpoint:unregister-endpoint"

def get_topology_uri():
    return "/restconf/config/network-topology:network-topology/topology/ovsdb:1"

if __name__ == "__main__":
    # Launch main menu


    # Some sensible defaults
    controller=os.environ.get('ODL')
    if controller == None:
        sys.exit("No controller set.")
    else:
        print "Contacting controller at %s" % controller

    resp=get(controller,DEFAULT_PORT,'/restconf/operational/endpoint:endpoints')
    resp_json=json.loads(resp.text)
    l2_eps=resp_json['endpoints']['endpoint']
    l3_eps=resp_json['endpoints']['endpoint-l3']

    print "deleting service function paths"
    rest_delete(controller, DEFAULT_PORT, get_service_function_paths_uri(), True)

    print "deleting service function chains"
    rest_delete(controller, DEFAULT_PORT, get_service_function_chains_uri(), True)

    print "deleting service functions"
    rest_delete(controller, DEFAULT_PORT, get_service_functions_uri(), True)

    print "deleting service function forwarders"
    rest_delete(controller, DEFAULT_PORT, get_service_function_forwarders_uri(), True)

    print "deleting tunnel"
    rest_delete(controller, DEFAULT_PORT, get_tunnel_uri(), True)

    print "deleting tenant"
    rest_delete(controller, DEFAULT_PORT, get_tenant_uri(), True)

    print "unregistering L2 endpoints"
    for endpoint in l2_eps:
        data={ "input": { "l2": [ { "l2-context": endpoint['l2-context'] ,"mac-address": endpoint['mac-address'] } ] } }
        post(controller, DEFAULT_PORT, get_endpoint_uri(),data,True)

    print "unregistering L3 endpoints"
    for endpoint in l3_eps:
        data={ "input": { "l3": [ { "l3-context": endpoint['l3-context'] ,"ip-address": endpoint['ip-address'] } ] } }
        post(controller, DEFAULT_PORT, get_endpoint_uri(),data,True)

    print "topology removed check"
    resp=get(controller, DEFAULT_PORT, get_topology_uri())
    topology_json=json.loads(resp.text)
    if resp.status_code == requests.codes.ok:
        print "WARNING: Topology %s has not been removed! Removing now..." % get_topology_uri()
        rest_delete(controller, DEFAULT_PORT, get_topology_uri(), True)
