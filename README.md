# WASP
Personal web archive and search as a container.


## Quickstart
- Install [Docker](https://docker.io) (Windows 10: use Pro or Education)
- `docker run -p 127.0.0.1:8001:8001 -p 127.0.0.1:8002:8002 --name wasp -d ghcr.io/webis-de/wasp:0.4.2`
- Configure your browser to use `localhost:8001` as proxy HTTP+HTTPS (exception: `localhost`)
- Clear browser cache or otherwise ensure your browser actually requests the web pages
- Visit [http://example.org](http://example.org)
- Visit [http://localhost:8002/search](http://localhost:8002/search) and search for "example"


## HTTPS
For HTTPS you have to trust the certificate of your personal WASP instance (generated on the first run).
- `docker cp wasp:/home/user/app/pywb/proxy-certs/pywb-ca.pem .`
- Configure your browser to trust this certificate **as an authority** to identify web pages


## Other commands
- `docker stop wasp`
- `docker start wasp`


## Troubleshooting
  - The search page never retrieves results/does not show new results!
      - WASP currently uses the default Elastic Search settings which turn the index read-only once your disk reaches 95% of used space. Since the index is (if you did not reconfigure that) stored on your root partition, you might need to clean up there.

