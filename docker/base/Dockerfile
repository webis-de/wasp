FROM openjdk:9.0-jre-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    python-pip

RUN pip install virtualenv

RUN useradd -ms /bin/bash user
USER user

RUN mkdir -p /home/user/srv/warcprox /home/user/srv/pywb /home/user/srv/elasticsearch

WORKDIR /home/user/srv/warcprox
RUN virtualenv env
RUN /bin/bash -c "source env/bin/activate && pip install warcprox==2.3"

WORKDIR /home/user/srv/pywb
RUN virtualenv env
RUN /bin/bash -c "source env/bin/activate && pip install pywb==2.0.2 && wb-manager init archive"
WORKDIR /home/user/srv/pywb/collections/archive
RUN rmdir archive
RUN ln -s /home/user/srv/warcprox/archive

WORKDIR /home/user/srv/elasticsearch
RUN curl -L -O https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.2.3.tar.gz
RUN tar xzf elasticsearch-*.tar.gz
RUN rm elasticsearch-*.tar.gz
RUN sed -i 's/^#path\.data.*/path.data: \/home\/user\/srv\/elasticsearch\/index/' elasticsearch-*/config/elasticsearch.yml
RUN sed -i 's/^#path\.logs.*/path.logs: \/home\/user\/srv\/elasticsearch\/logs/' elasticsearch-*/config/elasticsearch.yml

WORKDIR /home/user/srv

