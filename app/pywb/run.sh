#!/bin/bash

pywb_port=${PYWB_PORT:=8001}

case $1 in
  start)
    source env/bin/activate
    wayback --port $pywb_port --record 1> pywb.log 2>&1 &
    echo $! > pid.txt
    ;;
  stop)
    pid=$(cat pid.txt)
    kill $pid
    ;;
  *)
    echo Unknown command: $1
esac


