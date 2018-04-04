#!/bin/bash

elasticsearch_port=${ELASTICSEARCH_PORT:=9200}

case $1 in
  start)
    if [ -e initialize-sysout.txt ];then
      echo "Restarting"
    else
      echo "Waiting for elasticsearch to start"
      until grep -q "started" /home/user/srv/elasticsearch/sysout.txt;do
        sleep 1
      done
      echo "Should work now!"
      java -cp ../*.jar de.webis.warc.index.Index $elasticsearch_port 1> initialize-sysout.txt 2> initialize-syserr.txt
    fi

    java -cp ../*.jar de.webis.warc.index.WarcIndexer /home/user/srv/warcprox/archive $elasticsearch_port 1> sysout.txt 2> syserr.txt &
    echo $! > pid.txt
    ;;
  stop)
    pid=$(cat pid.txt)
    kill $pid
    ;;
  *)
    echo Unknown command: $1
esac


