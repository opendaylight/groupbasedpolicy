#!/usr/bin/python

from subprocess import check_output


def call_dpctl():
	cmd="ovs-dpctl dump-flows"
	listcmd=cmd.split()
	return check_output(listcmd)

if __name__ == "__main__" :
	flows=call_dpctl().split("recirc_id")
	print
	print "Flows from dpctl:"
	print 
	for flow in flows:
		if flow != "": print "recirc_id"+flow



