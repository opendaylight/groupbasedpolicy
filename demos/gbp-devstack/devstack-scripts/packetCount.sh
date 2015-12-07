watch -n 1 -d "sudo ovs-ofctl dump-flows br-int -OOpenFlow13 | grep -v n_packets=0"
