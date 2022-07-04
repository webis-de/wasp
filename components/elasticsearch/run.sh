#!/bin/bash

case $1 in
  start)
    ./elasticsearch-*/bin/elasticsearch 1> elastic.log 2>&1 &
    echo $! > pid.txt
    ;;
  stop)
    pid=$(cat pid.txt)
    kill $pid
    ;;
  *)
    echo Unknown command: $1
esac

