[ "$1" ] || {
    echo "Syntax:"
    echo "flowCount <bridge> [table#]"
    echo "Usage: flowCount <bridge>: shows flow count for entire switch."
    echo "Usage: flowcount <bridge> <table#>: shows groups for switch and flows and flow count for particular table."
    exit 1
}

if [ "$2" ]
then
	clear 
        echo "GROUPS:";sudo ovs-ofctl dump-groups $1 -OOpenFlow13; echo;echo "FLOWS:";sudo ovs-ofctl dump-flows $1 -OOpenFlow13 table=$2 --rsort=priority
	echo
	printf "Flow count: "
	sudo ovs-ofctl dump-flows $1 -OOpenFlow13 table=$2 | wc -l
else
        clear
        printf "No table entered. $1 flow count: "
        sudo ovs-ofctl dump-flows $1 -OOpenFlow13 | wc -l
	#echo "Expected single-node: 54 double-node: 62"
	printf "\nTable0: "; sudo ovs-ofctl dump-flows $1 -OOpenFlow13 table=0| wc -l
        printf "\nTable1: "; sudo ovs-ofctl dump-flows $1 -OOpenFlow13 table=1| wc -l
        printf "\nTable2: "; sudo ovs-ofctl dump-flows $1 -OOpenFlow13 table=2| wc -l
        printf "\nTable3: "; sudo ovs-ofctl dump-flows $1 -OOpenFlow13 table=3| wc -l
fi

