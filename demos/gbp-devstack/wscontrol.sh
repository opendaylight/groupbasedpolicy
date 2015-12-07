wireshark -k -i <(vagrant ssh -c "sudo dumpcap -P -i any -w - -f 'not tcp port 22'" -- -ntt)
