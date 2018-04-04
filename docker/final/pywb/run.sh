#!/bin/bash

pywb_port=${PYWB_PORT:=8002}
pywb_index_interval=${PYWB_INDEX_INTERVAL:=5}

case $1 in
  start)
    source env/bin/activate
    wayback --port $pywb_port --autoindex --auto-interval $pywb_index_interval 1> sysout.txt 2> syserr.txt &
    echo $! > pid.txt
    ;;
  stop)
    pid=$(cat pid.txt)
    kill $pid
    ;;
  *)
    echo Unknown command: $1
esac


