/ust/sbin/tc class add dev eth6 parent 1: classid 1:1 htb rate 1024Mbps
/usr/sbin/tc class change dev lo parent 1: classid 1:2 htb rate 10Kbps
