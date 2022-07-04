# WASP
Personal web archive and search.

## Quickstart
- Install [Docker](https://docker.io)
- `docker run -p 8001:8001 -p 8002:8002 -p 8003:8003 --name wasp -d ghcr.io/webis-de/wasp:0.3.0 # might require 'sudo'`
- Change your proxy to `localhost:8001` for HTTP+HTTPS with `localhost` for an exception (often by default); consider installing a proxy switching plugin
- Clear browser cache

- Open [http://localhost:8003/search] in your browser

You are already set up to use WASP for HTTP connections. For HTTP*S* connections, you have to trust WASP to identify foreign web pages for you. This can be done either by trusting the certificate of WASP's [warcprox](https://github.com/internetarchive/warcprox) instance in your browser (you can get it with ```docker cp wasp:/home/user/srv/warcprox/warcprox-ca.pem .```, then look at your browser's help page on how to import it), or by disabling the security check in your browser (e.g., you can start Chrome with *--ignore-certificate-errors*). See additional instructions for [firefox on windows 10](#wasp-under-windows) if needed.

NOTE: Above instructions for HTTPS disable some security features of your browser. Do this for testing the technology, but do *not* use WASP for web pages where you are concerned about security or privacy.

And that's it! Now all your traffic should be archived, indexed, and searchable immediately.

You can then stop the container using ```docker stop wasp``` and start it again with ```docker start wasp```. Note that your archive is stored in the container. If you remove the container, your archive is gone.


## Docker
A pre-build Docker image of WASP is available on [dockerhub](https://hub.docker.com/r/webis/wasp/).


## Troubleshooting
  - The search page never retrieves results/does not show new results!
      - WASP currently uses the default Elastic Search settings which turn the index read-only once your disk reaches 95% of used space. Since the index is (if you did not reconfigure that) stored on your root partition, you might need to clean up there.


### WASP under Windows
Under Windows 10 Pro, the Docker container works fine.
_Students and acedemic staff may be able to purchase a cheap license (free for students) to Windows 10 for Education, that includes the same native support for Docker containers._

Installing the certificate `warcprox-ca.pem` does not work correctly in Firefox when you simply open the file.
You need to explicitly add the certificate in the _Authorities_ tab of the certificates panel (so __not__ in the _Your Certificates_ tab).

After doing that, you are __ready to WASP__ under Windows 10.
