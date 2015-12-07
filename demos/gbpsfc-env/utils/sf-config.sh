#!/usr/bin/env bash

set -e
hostnum=${HOSTNAME#"gbpsfc"}
sw="sw$hostnum"
sudo ovs-vsctl add-br $sw
ovs-vsctl add-port $sw sfc-tun$hostnum -- set interface sfc-tun$hostnum type=vxlan options:remote_ip=flow options:dst_port=6633 options:key=flow options:nsi=flow options:nsp=flow options:"nshc1"=flow options:"nshc2"=flow options:"nshc3"=flow options:"nshc4"=flow

sudo ovs-ofctl add-flow $sw "priority=1000,nsp=1,nsi=255 actions=move:NXM_NX_NSH_C1[]->NXM_NX_NSH_C1[],move:NXM_NX_NSH_C2[]->NXM_NX_NSH_C2[],move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],load:0xC0A83247->NXM_NX_TUN_IPV4_DST[],set_nsi:254,set_nsp:1,IN_PORT" -OOpenFlow13
sudo ovs-ofctl add-flow $sw "priority=1000,nsp=1,nsi=254 actions=move:NXM_NX_NSH_C1[]->NXM_NX_NSH_C1[],move:NXM_NX_NSH_C2[]->NXM_NX_NSH_C2[],move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],load:0xC0A83249->NXM_NX_TUN_IPV4_DST[],set_nsi:253,set_nsp:1,IN_PORT" -OOpenFlow13

sudo ovs-ofctl add-flow $sw "priority=1000,nsp=2,nsi=255 actions=move:NXM_NX_NSH_C1[]->NXM_NX_NSH_C1[],move:NXM_NX_NSH_C2[]->NXM_NX_NSH_C2[],move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],load:0xC0A83249->NXM_NX_TUN_IPV4_DST[],set_nsi:254,set_nsp:2,IN_PORT" -OOpenFlow13
sudo ovs-ofctl add-flow $sw "priority=1000,nsp=2,nsi=254 actions=move:NXM_NX_NSH_C1[]->NXM_NX_NSH_C1[],move:NXM_NX_NSH_C2[]->NXM_NX_NSH_C2[],move:NXM_NX_TUN_ID[0..31]->NXM_NX_TUN_ID[0..31],load:0xC0A83247->NXM_NX_TUN_IPV4_DST[],set_nsi:253,set_nsp:2,IN_PORT" -OOpenFlow13
