#!/bin/bash

elasticsearch_port=${ELASTICSEARCH_PORT:=9200} # TODO: not used yet
search_port=${SEARCH_PORT:=8002}

case $1 in
  start)
    java -cp ../*.jar de.webis.wasp.SearchService $search_port 1> wasp-search.log 2>&1 &
    echo $! > pid.txt
    ;;
  stop)
    pid=$(pid.txt)
    kill $pid
    ;;
  *)
    echo Unknown command: $1
esac


