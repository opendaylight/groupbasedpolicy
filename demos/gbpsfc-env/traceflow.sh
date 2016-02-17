STR=$1

TRACE=${STR%", packets"*}

ovs-appctl ofproto/trace $TRACE
