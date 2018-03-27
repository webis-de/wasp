package de.webis.warc.ui;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class SearchService extends Thread {
  
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
    servletHandler.addServlet(this.servletHolder, "/");

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

}
