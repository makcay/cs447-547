TC=/usr/sbin/tc
INTERFACE1=eth6
INTERFACE2=eth3
killall tc_policy.sh
killall sleep
$TC qdisc del dev $INTERFACE1 root 1>/dev/null 2>&1
$TC qdisc del dev $INTERFACE2 root 1>/dev/null 2>&1
$TC qdisc del dev lo root 1>/dev/null 2>&1
