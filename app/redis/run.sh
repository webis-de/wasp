#!/bin/bash

case $1 in
  start)
    redis-server 1> redis.log 2>&1 &
    echo $! > pid.txt
    ;;
  stop)
    pid=$(cat pid.txt)
    kill $pid
    ;;
  *)
    echo Unknown command: $1
esac

