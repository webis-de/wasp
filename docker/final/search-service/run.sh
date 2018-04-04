#!/bin/bash

elasticsearch_port=${ELASTICSEARCH_PORT:=9200} # TODO: not used yet
search_port=${SEARCH_PORT:=8003}

case $1 in
  start)
    java -cp ../*.jar de.webis.warc.ui.SearchService $search_port 1> sysout.txt 2> syserr.txt &
    echo $! > pid.txt
    ;;
  stop)
    pid=$(pid.txt)
    kill $pid
    ;;
  *)
    echo Unknown command: $1
esac


