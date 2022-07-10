#!/bin/bash

services="redis elasticsearch pywb warc-indexer search-service"

for service in $services;do
  pushd $service
  ./run.sh $1
  popd
done

tail -f */*.log

