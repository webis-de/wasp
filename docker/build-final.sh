#!/bin/bash

source_dir=$(dirname $0)

pushd $source_dir/..
mvn clean compile assembly:single
popd

cp $source_dir/../target/personal-web-archive-*-jar-with-dependencies.jar $source_dir/final/

pushd $source_dir
sudo docker build --tag wasp final
popd

