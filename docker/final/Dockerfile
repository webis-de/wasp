FROM webis/wasp:base-0.0.1

RUN mkdir -p /home/user/srv/warc-indexer /home/user/srv/search-service

COPY elasticsearch/* /home/user/srv/elasticsearch/

COPY warcprox/* /home/user/srv/warcprox/

COPY pywb/config.yaml /home/user/srv/pywb/
COPY pywb/run.sh /home/user/srv/pywb/
COPY pywb/templates/* /home/user/srv/pywb/templates/

COPY warc-indexer/* /home/user/srv/warc-indexer

COPY search-service/* /home/user/srv/search-service

COPY *.jar /home/user/srv/
COPY control.sh /home/user/

WORKDIR /home/user

EXPOSE 8001
EXPOSE 8002
EXPOSE 8003

CMD ["./control.sh","start"]

