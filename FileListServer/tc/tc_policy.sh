#!/bin/bash
TC=/sbin/tc
INTERFACE_1=lo
PORT_1=5000
FILE_1=policy1
INTERFACE_2=lo
PORT_2=5001
FILE_2=policy2
INTERFACE_3=eth0
PORT_3=5000
FILE_3=policy1
INTERFACE_4=eth0
PORT_4=5001
FILE_4=policy2

parsePolicyFile () {
  device=$1
  filename=$2
  classId=$3
  if [ -z "$filename" ] || [ -z "$classId" ];then
    echo "filename and classid paramters required"
  else
    latestLoss="0%";
    latestDelay="0ms";
    while read -r line; do
      if [[ $line == \#* ]];then
        read -r line
      else
        keys=($line)
        comm=${keys[0]}
        value=${keys[1]}
	case $comm in
	  rate)
	    echo "setting rate on $device $classId $value"
  	    $TC class change dev $device parent 1: classid 1:$classId htb rate $value 
	    ;;
	  loss)
 	    latestLoss=$value;
	    echo "setting loss on $device $classId $value"
  	    $TC qdisc change dev $device parent 1:$classId netem loss $latestLoss delay $latestDelay
	    ;;
	  delay)
	    latestDelay=$value;
	    echo "setting delay on $device $classId $value"
  	    $TC qdisc change dev $device parent 1:$classId netem loss $latestLoss delay $latestDelay
	    ;;
	   wait)
	    echo "waiting for $device $value seconds"
	    sleep $value
	    ;;
	esac
      fi
    done < "$filename"
  fi
}

policyLoop () {
  device=$1
  filename=$2
  classId=$3
  while true; do
    parsePolicyFile $device $filename $classId
  done
}

currentIfNo=1
while [[ -v INTERFACE_$currentIfNo ]]; do
  interface=INTERFACE_$currentIfNo 
  interface="${!interface}"
  $TC qdisc del dev $interface root 1>/dev/null 2>&1
  $TC qdisc add dev $interface root handle 1: htb
  ((currentIfNo++))
done 

currentIfNo=1
while [[ -v PORT_$currentIfNo ]]; do
  interface=INTERFACE_$currentIfNo 
  interface="${!interface}"
  port=PORT_$currentIfNo 
  port="${!port}"
  file=FILE_$currentIfNo 
  file="${!file}"

  $TC class add dev $interface parent 1: classid 1:$currentIfNo htb rate 1024Mbps
  $TC qdisc add dev $interface parent 1:$currentIfNo netem loss 0%
  $TC filter add dev $interface protocol ip prio 1 u32 match ip sport $port 0xffff flowid 1:$currentIfNo
  policyLoop $interface $file $currentIfNo & 
  ((currentIfNo++))
done

wait
$TC qdisc del dev $INTERFACE_1 root 1>/dev/null 2>&1
$TC qdisc del dev $INTERFACE_2 root 1>/dev/null 2>&1
