FROM openjdk:17.0.2-slim AS compiler
RUN mkdir /app
RUN apt update && apt install -y --no-install-recommends \
  maven
WORKDIR /app/
COPY pom.xml /app/
RUN mvn clean compile assembly:single
COPY resources /app/resources/
COPY src /app/src/
RUN mvn clean compile assembly:single


FROM openjdk:17.0.2-slim
EXPOSE 8001
EXPOSE 8002

RUN apt update && apt install -y --no-install-recommends \
    curl \
    python3-pip \
    redis-server
RUN pip3 install virtualenv


RUN useradd -ms /bin/bash user
USER user


RUN mkdir -p /home/user/app/redis /home/user/app/elasticsearch /home/user/app/search-service /home/user/app/warc-indexer /home/user/app/pywb
WORKDIR /home/user/app/pywb
RUN virtualenv env \
  && /bin/bash -c "source env/bin/activate && pip3 install pywb==2.6.7 && wb-manager init wasp"
WORKDIR /home/user/app/elasticsearch
RUN curl -L -O https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.2.3-linux-x86_64.tar.gz \
  && tar xzf elasticsearch-*.tar.gz \
  && rm elasticsearch-*.tar.gz \
  && sed -i 's/^#path\.data.*/path.data: \/home\/user\/app\/elasticsearch\/index/' elasticsearch-*/config/elasticsearch.yml \
  && sed -i 's/^#path\.logs.*/path.logs: \/home\/user\/app\/elasticsearch\/logs/' elasticsearch-*/config/elasticsearch.yml \
  && echo "xpack.security.enabled: false" | tee -a elasticsearch-*/config/elasticsearch.yml


WORKDIR /home/user/app
COPY app/control.sh /home/user/app/
COPY app/redis/ /home/user/app/redis
COPY app/elasticsearch/ /home/user/app/elasticsearch
COPY app/search-service/ /home/user/app/search-service
COPY app/warc-indexer/ /home/user/app/warc-indexer
COPY app/pywb/ /home/user/app/pywb
COPY --from=compiler /app/target/*-jar-with-dependencies.jar /home/user/app/
CMD ["./control.sh","start"]


