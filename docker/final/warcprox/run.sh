#!/bin/bash

warcprox_port=${WARCPROX_PORT:=8001}
warc_size=${WARC_SIZE:=10000000}

case $1 in
  start)
    source env/bin/activate
    warcprox --port $warcprox_port --address 0.0.0.0 --cacert warcprox-ca.pem --certs-dir warcprox-ca --dir archive --gzip --prefix archive --no-warc-open-suffix --size $warc_size 1> sysout.txt 2> syserr.txt &
    echo $! > pid.txt
    ;;
  stop)
    pid=$(cat pid.txt)
    kill $pid
    ;;
  *)
    echo Unknown command: $1
esac


