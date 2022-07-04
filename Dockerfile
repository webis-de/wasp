FROM openjdk:17.0.2-slim AS compiler
RUN mkdir /app
RUN apt update && apt install -y --no-install-recommends \
  maven
COPY pom.xml /app/
WORKDIR /app/
RUN mvn clean compile assembly:single


FROM openjdk:17.0.2-slim
EXPOSE 8001
EXPOSE 8002

RUN apt update && apt install -y --no-install-recommends \
    curl \
    python3-pip
RUN pip3 install virtualenv


RUN useradd -ms /bin/bash user
USER user


RUN mkdir -p /home/user/app/pywb /home/user/app/elasticsearch /home/user/app/warc-indexer /home/user/app/search-service
WORKDIR /home/user/app/pywb
RUN virtualenv env
RUN /bin/bash -c "source env/bin/activate && pip3 install pywb==2.6.7 && wb-manager init archive"
WORKDIR /home/user/app/elasticsearch
RUN curl -L -O https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-8.2.3-linux-x86_64.tar.gz
RUN tar xzf elasticsearch-*.tar.gz
RUN rm elasticsearch-*.tar.gz
RUN sed -i 's/^#path\.data.*/path.data: \/home\/user\/app\/elasticsearch\/index/' elasticsearch-*/config/elasticsearch.yml
RUN sed -i 's/^#path\.logs.*/path.logs: \/home\/user\/app\/elasticsearch\/logs/' elasticsearch-*/config/elasticsearch.yml


WORKDIR /home/user/app
COPY components/* /home/user/app/
COPY --from=compiler /app/target/*-jar-with-dependencies.jar /home/user/app/
CMD ["./control.sh","start"]


