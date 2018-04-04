#!/bin/bash

case $1 in
  start)
    ./elasticsearch-*/bin/elasticsearch 1> sysout.txt 2> syserr.txt &
    echo $! > pid.txt
    ;;
  stop)
    pid=$(cat pid.txt)
    kill $pid
    ;;
  *)
    echo Unknown command: $1
esac

