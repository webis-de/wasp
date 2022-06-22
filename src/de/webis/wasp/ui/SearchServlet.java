package de.webis.wasp.ui;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import de.webis.wasp.index.Index;
import de.webis.wasp.index.Query;
import de.webis.wasp.index.Result;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet for the search service.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class SearchServlet
extends HttpServlet {
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTANTS
  /////////////////////////////////////////////////////////////////////////////

  private static final long serialVersionUID = -5259242888271066638L;

  /////////////////////////////////////////////////////////////////////////////
  // CONFIGURATION

  public static final String INIT_PARAMETER_INDEX_PORT = "index.port";

  public static final int DEFAULT_INDEX_PORT = Index.DEFAULT_PORT;
  
  public static final String INIT_PARAMETER_PAGE_SIZE = "page.size";

  public static final int DEFAULT_PAGE_SIZE = 10;
  
  public static final String INIT_PARAMETER_REPLAY_SERVER = "replay.server";

  public static final String DEFAULT_REPLAY_SERVER = "https://localhost:8002";
  
  public static final String INIT_PARAMETER_REPLAY_COLLECTION = "replay.collection";

  public static final String DEFAULT_REPLAY_COLLECTION = "archive";

  /////////////////////////////////////////////////////////////////////////////
  // REQUEST
  
  public static final String SERVLET_PATH = "search";
  
  public static final String REQUEST_PARAMETER_TERMS = "terms";
  
  public static final String REQUEST_PARAMETER_FROM = "from";
  
  public static final String REQUEST_PARAMETER_TO = "to";
  
  public static final String REQUEST_PARAMETER_TIMEZONE = "timezone";
  
  public static final String REQUEST_PARAMETER_PAGE_NUMBER = "page";

  /////////////////////////////////////////////////////////////////////////////
  // SESSION

  protected static final String SESSION_QUERY = "query";

  protected static final String SESSION_RESULTS = "results";
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////

  private final Mustache pageRenderer;
  
  private Index index;
  
  private int pageSize;

  private String replayServer;

  private String replayCollection;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTION
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a new servlet.
   */
  public SearchServlet() {
    final MustacheFactory factory = new DefaultMustacheFactory();
    this.pageRenderer = factory.compile(new InputStreamReader(
        SearchServlet.class.getResourceAsStream("search.mustache")),
        "search.mustache");
    this.index = null;
    this.pageSize = 0;
    this.replayServer = null;
    this.replayCollection = null;
  }
  
  @Override
  public void init(final ServletConfig config) throws ServletException {
    this.index = new Index(
        SearchServlet.getParameterValue(config,
            INIT_PARAMETER_INDEX_PORT, DEFAULT_INDEX_PORT));
    this.pageSize = SearchServlet.getParameterValue(config,
        INIT_PARAMETER_PAGE_SIZE, DEFAULT_PAGE_SIZE);
    this.replayServer =  SearchServlet.getParameterValue(config,
        INIT_PARAMETER_REPLAY_SERVER, DEFAULT_REPLAY_SERVER);
    this.replayCollection =  SearchServlet.getParameterValue(config,
        INIT_PARAMETER_REPLAY_COLLECTION, DEFAULT_REPLAY_COLLECTION);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // GETTERS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Gets the page renderer.
   * @return The renderer
   */
  public Mustache getPageRenderer() {
    return this.pageRenderer;
  }

  /**
   * Gets the index client.
   * @return The client
   */
  protected Index getIndex() {
    return this.index;
  }

  /**
   * Gets the page size to render.
   * @return The page size
   */
  public int getPageSize() {
    return this.pageSize;
  }

  /**
   * Gets the address (including protocol and host) of the replay server. 
   * @return The URI
   */
  public String getReplayServer() {
    return this.replayServer;
  }

  /**
   * Gets the name of the collection to replay from.
   * @return The name
   */
  public String getReplayCollection() {
    return this.replayCollection;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////

  @Override
  protected void doGet(
      final HttpServletRequest request, final HttpServletResponse response)
  throws ServletException, IOException {
    final UiPage page = this.getPage(request);

    response.setContentType("text/html");
    this.getPageRenderer().execute(response.getWriter(), page);
  };
  
  /////////////////////////////////////////////////////////////////////////////
  // HELPERS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Gets an implementation of the search page model for rendering.
   * @param request The request to the servlet
   * @return The page model
   * @throws IOException On searching the index
   */
  protected UiPage getPage(final HttpServletRequest request) 
  throws IOException {
    final int pageSize = this.getPageSize();

    final Query query = SearchServlet.getQuery(request);
    final TimeZone timezone = SearchServlet.getClientTimeZone(request);
    if (query == null) {
      return new UiPage(
          this.getReplayServer(), this.getReplayCollection(),
          request.getLocale(), timezone);
    } else {
      final List<Result> results = this.getResults(request, query);
      final int numResults = results.size();
      final int numPages = (numResults - 1) / pageSize + 1;
      final int pageNumber = SearchServlet.getPageNumber(request);
      final int fromResult = Math.min((pageNumber - 1) * pageSize, numResults);
      final int toResult = Math.min(pageNumber * pageSize, numResults);
      final List<Result> paginatedResults =
          results.subList(fromResult, toResult);

      return new UiPage(
          this.getReplayServer(), this.getReplayCollection(),
          query, paginatedResults, pageNumber, numPages,
          request.getLocale(), timezone);
    }
  }

  /**
   * Gets the results for the specified query.
   * @param request The request to the servlet
   * @param query The query
   * @return The results for the query
   * @throws IOException On searching the index
   */
  protected List<Result> getResults(
      final HttpServletRequest request, final Query query)
  throws IOException {
    final HttpSession session = request.getSession();
    synchronized (session) {
      @SuppressWarnings("unchecked")
      List<Result> results =
          (List<Result>) session.getAttribute(SESSION_RESULTS);
      if (results == null) {
        results = this.getIndex().search(query);
        session.setAttribute(SESSION_RESULTS, results);
      }
      return results;
    }
  }

  /**
   * Gets the query for a request.
   * @param request The request to the servlet
   * @return The query or <code>null</code> for none
   */
  protected static Query getQuery(final HttpServletRequest request) {
    final String terms = request.getParameter(REQUEST_PARAMETER_TERMS);
    if (terms == null) { return null; }

    final TimeZone timezone = SearchServlet.getClientTimeZone(request);
    final Instant from = SearchServlet.parseInstant(
        request.getParameter(REQUEST_PARAMETER_FROM), timezone);
    final Instant to = SearchServlet.parseInstant(
        request.getParameter(REQUEST_PARAMETER_TO), timezone);
    final Query query = new Query(terms, from, to);

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

  /**
   * Gets the page number for a request.
   * @param request The request to the servlet
   * @return The page number (1 by default)
   */
  protected static int getPageNumber(final HttpServletRequest request) {
    final String pageNumberString =
        request.getParameter(REQUEST_PARAMETER_PAGE_NUMBER);
    if (pageNumberString == null) {
      return 1;
    } else {
      return Integer.parseInt(pageNumberString);
    }
  }

  /**
   * Gets the time zone of the browser.
   * @param request The request to the servlet
   * @return The guessed time zone
   */
  protected static TimeZone getClientTimeZone(
      final HttpServletRequest request) {
    final String value = request.getParameter(REQUEST_PARAMETER_TIMEZONE);
    if (value == null) {
      return TimeZone.getDefault();
    } else {
      return TimeZone.getTimeZone(value);
    } 
  }

  /**
   * Parses an instant from a request parameter.
   * @param value The parameter value (may be <code>null</code>)
   * @param timeZone The time zone of the browser
   * @return The instant or <code>null</code> for none
   */
  protected static Instant parseInstant(
      final String value, final TimeZone timeZone) {
    if (value == null || value.isEmpty()) {
      return null;
    } else {
      return Instant.from(UiPage.UiInstant.DATE_TIME_PICKER_FORMATTER
          .withZone(timeZone.toZoneId()).parse(value));
    }
  }

  /**
   * Gets the value for a parameter.
   * @param config The servlet configuration
   * @param parameter The parameter name
   * @return The value
   * @throws NoSuchElementException If no value is provided
   */
  protected static String getParameterValue(final ServletConfig config,
      final String parameter) {
    return SearchServlet.getParameterValue(config, parameter, null);
  }

  /**
   * Gets the value for a parameter.
   * @param config The servlet configuration
   * @param parameter The parameter name
   * @param defaultValue The default value or <code>null</code> for none
   * @return The value (may be the default)
   * @throws NoSuchElementException If no value and no default value is provided
   */
  protected static String getParameterValue(final ServletConfig config,
      final String parameter, final String defaultValue) {
    final String value = config.getInitParameter(parameter);
    if (value == null) {
      if (defaultValue == null) {
        throw new NoSuchElementException(parameter);
      } else {
        return defaultValue;
      }
    } else {
      return value;
    }
  }

  /**
   * Gets the value for a parameter as integer.
   * @param config The servlet configuration
   * @param parameter The parameter name
   * @param defaultValue The default value
   * @return The value (may be the default)
   */
  protected static int getParameterValue(final ServletConfig config,
      final String parameter, final int defaultValue) {
    final String value = config.getInitParameter(parameter);
    if (value == null) {
      return defaultValue;
    } else {
      return Integer.parseInt(value);
    }
  }

}
