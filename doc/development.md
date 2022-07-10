WASP Development
================

```
sudo docker build -t ghcr.io/webis-de/wasp:dev .
sudo docker run -p 127.0.0.1:8001:8001 -p 127.0.0.1:8002:8002 --name wasp ghcr.io/webis-de/wasp:dev
sudo docker cp wasp:/home/user/app/pywb/proxy-certs/pywb-ca.pem .
```


WARC/1.0
WARC-Type: response
WARC-Record-ID: <urn:uuid:6c7606bc-009d-11ed-858a-0242ac110002>
WARC-Target-URI: http://example.org/
WARC-Date: 2022-07-10T22:12:44Z
WARC-IP-Address: 93.184.216.34
Content-Type: application/http; msgtype=response
Content-Length: 1011
WARC-Payload-Digest: sha1:WJM2KPM4GF3QK2BISVUH2ASX64NOUY7L
WARC-Block-Digest: sha1:AHTT7POCPQGX5HISIEJRSWCAJNRIW2SS


WARC/1.0
WARC-Type: revisit
WARC-Record-ID: <urn:uuid:89c31605-c393-44bd-b6d7-9180f05ff6df>
WARC-Target-URI: http://example.org/
WARC-Date: 2022-07-10T22:12:56Z
WARC-Profile: http://netpreserve.org/warc/1.0/revisit/identical-payload-digest
WARC-Refers-To-Target-URI: http://example.org/
WARC-Refers-To-Date: 2022-07-10T22:12:44Z
WARC-Payload-Digest: sha1:WJM2KPM4GF3QK2BISVUH2ASX64NOUY7L
WARC-Block-Digest: sha1:POVSAFA5NOXOKFZSM4ZVDBIYOTSG2YTS
Content-Type: application/http; msgtype=response
Content-Length: 380

