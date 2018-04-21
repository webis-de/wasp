# WASP



## Quickstart
You need to have [Docker](https://docker.io) installed on your system. You can then immediately start WASP like this:
```
docker run -p 8001:8001 -p 8002:8002 -p 8003:8003 --name wasp -it webis/wasp:0.2.0
```
Depending on your Docker setup, you might need to run this command as an administrator or add ```sudo``` in front of it.

This will start the container attached to the current terminal. You can use CTRL-C to stop the container.

As long as the container is running, you should already see the search interface in your browser:

  http://localhost:8003/search



Next, you will have to change the proxy settings of your browser to route the requests and responses through it. How this can be done depends on the browser. I think it is easiest in Firefox. Chrome uses just the system settings. In both cases you should open the settings and search for proxy. Chrome will just open the system dialog for setting a proxy. You need the following proxy for HTTP and HTTPS:

  localhost:8001

And have "localhost" as an exception (often so by default). 


## Docker
A pre-build Docker image of WASP is available on [dockerhub](https://hub.docker.com/r/webis/wasp/).
