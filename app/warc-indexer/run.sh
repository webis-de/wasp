#!/bin/bash

elasticsearch_port=${ELASTICSEARCH_PORT:=9200}

case $1 in
  start)
    if [ -e init.log ];then
      echo "Restarting"
    else
      echo "Waiting for elasticsearch to start"
      until grep -q "AllocationService.*current.health=.GREEN" /home/user/app/elasticsearch/elastic.log;do
        sleep 1
      done
      echo "Should work now!"
      java -cp ../*.jar de.webis.wasp.index.Index $elasticsearch_port 1> init.log 2>&1
    fi

    java -cp ../*.jar de.webis.wasp.WarcIndexingService /home/user/app/pywb/collections/wasp/archive $elasticsearch_port 1> indexer.log 2>&1 &
    echo $! > pid.txt
    ;;
  stop)
    pid=$(cat pid.txt)
    kill $pid
    ;;
  *)
    echo Unknown command: $1
esac


