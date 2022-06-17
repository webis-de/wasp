package de.webis.warc.ui;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.TimeZone;

import de.webis.warc.index.Index;
import de.webis.warc.index.Query;
import de.webis.warc.index.Result;
import de.webis.warc.index.ResultsFetcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class SearchServlet extends HttpServlet {

  private static final long serialVersionUID = -5259242888271066638L;
  
  public static final String SERVLET_PATH = "search";
  
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
  
  public static final String REQUEST_PARAMETER_TIMEZONE = "timezone";
  
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
    final TimeZone timezone = this.getClientTimezone(request);
    if (query == null) {
      this.renderer.render(
          response.getWriter(), request.getLocale(), timezone);
    } else {
      final ResultsFetcher results = this.getResults(request, query);
      final int pageNumber = this.getPageNumber(request);
      final List<Result> resultList = results.fetch(pageNumber);
      final boolean isLastPage = resultList.size() < this.pageSize;
      this.renderer.render(response.getWriter(),
          query, resultList, pageNumber, isLastPage,
          request.getLocale(), timezone);
    }
  };
  
  protected Query getQuery(final HttpServletRequest request) {
    final String terms = request.getParameter(REQUEST_PARAMETER_TERMS);
    if (terms == null) { return null; }

    final TimeZone timezone = this.getClientTimezone(request);
    final Query query = Query.of(terms);
    final Instant from = this.parseInstantFromGet(
        request.getParameter(REQUEST_PARAMETER_FROM), timezone);
    if (from != null) { query.from(from); }
    final Instant to = this.parseInstantFromGet(
        request.getParameter(REQUEST_PARAMETER_TO), timezone);
    if (to != null) { query.to(to); }

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
  
  protected TimeZone getClientTimezone(
      final HttpServletRequest request) {
    final String value = request.getParameter(REQUEST_PARAMETER_TIMEZONE);
    if (value == null) {
      return TimeZone.getDefault();
    } else {
      return TimeZone.getTimeZone(value);
    } 
  }
  
  protected Instant parseInstantFromGet(
      final String getParameter, final TimeZone timezone) {
    if (getParameter == null || getParameter.isEmpty()) {
      return null;
    } else {
      return Instant.from(ResultPageRenderer.DATE_TIME_PICKER_FORMATTER
          .withZone(timezone.toZoneId()).parse(getParameter));
    }
  }
  
  protected ResultsFetcher getResults(
      final HttpServletRequest request, final Query query)
  throws IOException {
    final HttpSession session = request.getSession();
    synchronized (session) {
      ResultsFetcher results = (ResultsFetcher) session.getAttribute(SESSION_RESULTS);
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
