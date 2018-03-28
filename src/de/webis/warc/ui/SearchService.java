package de.webis.warc.ui;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
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
    servletHandler.addServlet(this.servletHolder, "/search");
    servletHandler.setSessionHandler(new SessionHandler());

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
    final SearchService service = new SearchService(8003);
    service.setInitParameter(SearchServlet.INIT_PARAMETER_REPLAY_PORT, "8002");
    service.run();
  }

}
