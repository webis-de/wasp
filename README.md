# WASP
WASP, is a fully functional prototype of a personal web archive and search system.

## Quickstart
You need to have [Docker](https://docker.io) installed on your system. You can then immediately start WASP like this:
```
docker run -p 8001:8001 -p 8002:8002 -p 8003:8003 --name wasp -d webis/wasp:0.2.0
```
Depending on your Docker setup, you might need to run this command as an administrator or add ```sudo``` in front of it.

After a few seconds, you should already see the search interface in your browser at (http://localhost:8003/search)

Next, you will have to change the proxy settings of your browser to route the requests and responses through it. How this can be done depends on the browser ([Chrome](https://support.mozilla.org/en-US/kb/connection-settings-firefox), [Edge](https://www.rogers.com/customer/support/article/set-up-proxy-settings-in-microsoft-edge), [Firefox](https://support.mozilla.org/en-US/kb/connection-settings-firefox), [Opera](https://customers.trustedproxies.com/knowledgebase.php?action=displayarticle&id=40), [Safari](https://support.apple.com/kb/PH21420?locale=en_US)). However, also consider using a tailored proxy switching plugin for your browser, which allows you to disable/enable WASP easily. In all cases, you need to specify (localhost:8001) as proxy for HTTP and HTTPS and have "localhost" as an exception (often so by default).

Now it is time to clear your browser's cache (or use incognito mode). Otherwise you will be disappointed when some elements are missing in the archive.

You are already set up to use WASP for HTTP connections. For HTTP*S* connections, you have to trust WASP to identify foreign web pages for you. This can be done either by trusting the certificate of WASP's [warcprox](https://github.com/internetarchive/warcprox) instance in your browser (you can get it with ```docker cp wasp:/home/user/srv/warcprox/warcprox-ca.pem .```, then look at your browser's help page on how to import it), or by disabling the security check in your browser (e.g., you can start Chrome with *--ignore-certificate-errors*). See additional instructions for [firefox on windows 10](ffwin10.md) if needed.

NOTE: Above instructions for HTTPS disable some security features of your browser. Do this for testing the technology, but do *not* use WASP for web pages where you are concerned about security or privacy.

And that's it! Now all your traffic should be archived, indexed, and searchable immediately.

You can then stop the container using ```docker stop wasp``` and start it again with ```docker start wasp```. Note that your archive is stored in the container. If you remove the container, your archive is gone.


## Docker
A pre-build Docker image of WASP is available on [dockerhub](https://hub.docker.com/r/webis/wasp/).


## Troubleshooting
  - The search page never retrieves results/does not show new results!
      - WASP currently uses the default Elastic Search settings which turn the index read-only once your disk reaches 95% of used space. Since the index is (if you did not reconfigure that) stored on your root partition, you might need to clean up there.
