package de.webis.warc.ui;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.ResourceCollection;

public class SearchService extends Thread {
  
  public static final int DEFAULT_PORT = 8003;
  
  protected final ServletHolder servletHolder;
  
  protected final int port;
  
  public SearchService(final int port) {
    this.servletHolder = new ServletHolder(SearchServlet.class);
    this.port = port;
  }
  
  @Override
  public void run() {
    final ServletContextHandler servletHandler = new ServletContextHandler();
    servletHandler.setContextPath("/");
    servletHandler.setSessionHandler(new SessionHandler());

    // Search Servlet
    servletHandler.addServlet(
        this.servletHolder, "/" + SearchServlet.SERVLET_PATH);
    
    // Serve files from resources/static/
    servletHandler.setBaseResource(new ResourceCollection(
        this.getClass().getClassLoader().getResource("static")
        .toExternalForm()));
    final ServletHolder resourcesServlet =
        new ServletHolder("static-embedded", DefaultServlet.class);
    resourcesServlet.setInitParameter("dirAllowed", "true");
    servletHandler.addServlet(resourcesServlet, "/");

    // Start server
    final Server server = new Server(this.port);
    server.setHandler(servletHandler);
    try {
      server.start();
      server.join();
    } catch (final Exception exception) {
      throw new RuntimeException(exception);
    }
  }
  
  protected void setInitParameter(final String parameter, final String value) {
    this.servletHolder.setInitParameter(parameter, value);
  }
  
  public static void main(final String[] args) {
    final int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
    final SearchService service = new SearchService(port);
    service.run();
  }

}
