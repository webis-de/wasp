#!/bin/bash

services="elasticsearch warcprox pywb warc-indexer search-service"

for service in $services;do
  pushd srv/$service
  ./run.sh $1
  popd
done

tail -f srv/*/*.txt

