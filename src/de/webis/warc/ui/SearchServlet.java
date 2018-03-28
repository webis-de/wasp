package de.webis.warc.ui;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.joda.time.Instant;

import de.webis.warc.index.Index;
import de.webis.warc.index.Query;
import de.webis.warc.index.ResultPages;

public class SearchServlet extends HttpServlet {

  private static final long serialVersionUID = -5259242888271066638L;
  
  public static final String INIT_PARAMETER_INDEX_PORT = "index.port";

  public static final int DEFAULT_INDEX_PORT = Index.DEFAULT_PORT;
  
  public static final String INIT_PARAMETER_PAGE_SIZE = "index.port";

  public static final int DEFAULT_PAGE_SIZE = 10;
  
  public static final String INIT_PARAMETER_REPLAY_PORT = "replay.port";

  public static final int DEFAULT_REPLAY_PORT = 8002;
  
  public static final String INIT_PARAMETER_REPLAY_COLLECTION = "replay.collection";

  public static final String DEFAULT_REPLAY_COLLECTION = "archive";
  
  public static final String REQUEST_PARAMETER_TERMS = "terms";
  
  public static final String REQUEST_PARAMETER_FROM = "from";
  
  public static final String REQUEST_PARAMETER_TO = "to";
  
  public static final String REQUEST_PARAMETER_PAGE_NUMBER = "page";

  protected static final String SESSION_QUERY = "query";

  protected static final String SESSION_RESULTS = "results";
  
  protected Index index;
  
  protected int pageSize;
  
  protected ResultPageRenderer renderer;
  
  public SearchServlet() {
    this.index = null;
    this.pageSize = 0;
    this.renderer = null;
  }
  
  @Override
  public void init(final ServletConfig config) throws ServletException {
    this.index = new Index(
        SearchServlet.getParameter(config,
            INIT_PARAMETER_INDEX_PORT, DEFAULT_INDEX_PORT));
    this.pageSize = SearchServlet.getParameter(config,
        INIT_PARAMETER_PAGE_SIZE, DEFAULT_PAGE_SIZE);
    this.renderer = new ResultPageRenderer(
        SearchServlet.getParameter(config,
            INIT_PARAMETER_REPLAY_PORT, DEFAULT_REPLAY_PORT),
        SearchServlet.getParameter(config,
            INIT_PARAMETER_REPLAY_COLLECTION, DEFAULT_REPLAY_COLLECTION));
  }
  
  protected void doGet(
      final HttpServletRequest request, final HttpServletResponse response)
  throws ServletException, IOException {
    response.setContentType("text/html");
    final Query query = this.getQuery(request);
    if (query == null) {
      this.renderer.render(response.getWriter());
    } else {
      final ResultPages results = this.getResults(request, query);
      final int pageNumber = this.getPageNumber(request);
      this.renderer.render(response.getWriter(),
          query, results.getPage(pageNumber), pageNumber);
    }
  };
  
  protected Query getQuery(final HttpServletRequest request) {
    final String terms = request.getParameter(REQUEST_PARAMETER_TERMS);
    if (terms == null) { return null; }

    final Query query = Query.of(terms);
    final String from = request.getParameter(REQUEST_PARAMETER_FROM);
    if (from != null) { query.from(Instant.parse(from)); }
    final String to = request.getParameter(REQUEST_PARAMETER_TO);
    if (to != null) { query.to(Instant.parse(to)); }

    final HttpSession session = request.getSession();
    synchronized (session) {
      final Query oldQuery = (Query) session.getAttribute(SESSION_QUERY);
      if (query == null || !query.equals(oldQuery)) {
        session.setAttribute(SESSION_QUERY, query);
        session.removeAttribute(SESSION_RESULTS);
      }
      return query;
    }
  }
  
  protected ResultPages getResults(
      final HttpServletRequest request, final Query query)
  throws IOException {
    final HttpSession session = request.getSession();
    synchronized (session) {
      ResultPages results = (ResultPages) session.getAttribute(SESSION_RESULTS);
      if (results == null) {
        results =  this.index.query(query, this.pageSize);
        session.setAttribute(SESSION_RESULTS, results);
      }
      return results;
    }
  }
  
  protected int getPageNumber(final HttpServletRequest request) {
    final String pageNumberString =
        request.getParameter(REQUEST_PARAMETER_PAGE_NUMBER);
    if (pageNumberString == null) {
      return 1;
    } else {
      return Integer.parseInt(pageNumberString);
    }
  }
  
  protected static String getParameter(final ServletConfig config,
      final String parameter) {
    return SearchServlet.getParameter(config, parameter, null);
  }
  
  protected static String getParameter(final ServletConfig config,
      final String parameter, final String defaultValue) {
    final String value = config.getInitParameter(parameter);
    if (value == null) {
      if (defaultValue == null) {
        throw new NullPointerException(parameter);
      } else {
        return defaultValue;
      }
    } else {
      return value;
    }
  }
  
  protected static int getParameter(final ServletConfig config,
      final String parameter, final int defaultValue) {
    final String value = config.getInitParameter(parameter);
    if (value == null) {
      return defaultValue;
    } else {
      return Integer.parseInt(value);
    }
  }

}
