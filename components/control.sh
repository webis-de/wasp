#!/bin/bash

services="elasticsearch pywb warc-indexer search-service"

for service in $services;do
  pushd app/$service
  ./run.sh $1
  popd
done

tail -f app/*/*.log

