#!/bin/bash
TC=/usr/sbin/tc
INTERFACE=lo
PORT_1=5000
FILE_1=policy1
PORT_2=5001
FILE_2=policy2

parsePolicyFile () {
  filename=$1
  classId=$2
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
  	    $TC class change dev $INTERFACE parent 1: classid 1:$classId htb rate $value 
	    echo "seting rate on $classId $value"
	    ;;
	  loss)
 	    latestLoss=$value;
  	    $TC qdisc change dev $INTERFACE parent 1:$classId netem loss $latestLoss delay $latestDelay
	    echo "seting loss on $classId $value"
	    ;;
	  delay)
	    latestDelay=$value;
  	    $TC qdisc change dev $INTERFACE parent 1:$classId netem loss $latestLoss delay $latestDelay
	    echo "seting delay on $classId $value"
	    ;;
	   wait)
	    sleep $value
	    echo "waiting for $value seconds"
	    ;;
	esac
      fi
    done < "$filename"
  fi
}

$TC qdisc del dev $INTERFACE root 1>/dev/null 2>&1
$TC qdisc add dev $INTERFACE root handle 1: htb

currentIfNo=1
while [[ -v PORT_$currentIfNo ]]; do
  port=PORT_$currentIfNo 
  port="${!port}"
  file=FILE_$currentIfNo 
  file="${!file}"

  $TC class add dev $INTERFACE parent 1: classid 1:$currentIfNo htb rate 1024Mbps
  $TC qdisc add dev $INTERFACE parent 1:$currentIfNo netem loss 0%
  $TC filter add dev $INTERFACE protocol ip prio 1 u32 match ip sport $port 0xffff flowid 1:$currentIfNo
  parsePolicyFile $file $currentIfNo & 
  ((currentIfNo++))
done

wait
$TC qdisc del dev $INTERFACE root 1>/dev/null 2>&1
