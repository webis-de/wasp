WASP Development
================

```
sudo docker build -t ghcr.io/webis-de/wasp:dev .
sudo docker run -p 127.0.0.1:8001:8001 -p 127.0.0.1:8002:8002 --name wasp ghcr.io/webis-de/wasp:dev
sudo docker cp wasp:/home/user/app/pywb/proxy-certs/pywb-ca.pem .
```

