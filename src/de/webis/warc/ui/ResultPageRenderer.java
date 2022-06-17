package de.webis.warc.ui;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import de.webis.warc.index.Query;
import de.webis.warc.index.Result;

/**
 * Renders the user interface for the search service.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class ResultPageRenderer {
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTANTS
  /////////////////////////////////////////////////////////////////////////////
  
  protected static final DateTimeFormatter REPLAY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  
  protected static final DateTimeFormatter DATE_TIME_PICKER_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  
  protected static final int MAX_URI_DISPLAY_LENGTH = 60;
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final int replayPort;
  
  protected final String replayCollection;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Creates a new renderer.
   * <p>
   * It uses {@link SearchServlet#DEFAULT_REPLAY_PORT} and
   * {@link SearchServlet#DEFAULT_REPLAY_COLLECTION} for the corresponding
   * parameters.
   * </p>
   * @see #ResultPageRenderer(int, String)
   */
  public ResultPageRenderer() {
    this(
        SearchServlet.DEFAULT_REPLAY_PORT,
        SearchServlet.DEFAULT_REPLAY_COLLECTION);
  }
  
  /**
   * Creates a new renderer that refers to the replay server at given port.
   * @param replayPort The port at which the replay server listens
   * @param replayCollection The collection of the replay server to use
   */
  public ResultPageRenderer(
      final int replayPort, final String replayCollection) {
    if (replayCollection == null) {
      throw new NullPointerException("collection");
    }
    this.replayPort = replayPort;
    this.replayCollection = replayCollection;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Render the search page without a specific query.
   * @param output Where to write the page to
   * @param locale Locale of the user client
   * @param timezone Time zone of the user client
   */
  public void render(final PrintWriter output,
      final Locale locale, final TimeZone timezone) {
    this.renderHeader(output, locale, null);
    this.renderQueryBox(output, null, timezone);
    this.renderFooter(output);
  }

  /**
   * Render the search page with results for a specific query.
   * @param output Where to write the page to
   * @param query The query that produced the results
   * @param page The results for the query and the current result page number
   * @param pageNumber Number of the current page in the result list
   * @param isLastPage Whether the current page is the last page in the result
   * list
   * @param locale Locale of the user client
   * @param timezone Time zone of the user client
   */
  public void render(final PrintWriter output,
      final Query query,
      final List<Result> page, final int pageNumber, final boolean isLastPage,
      final Locale locale, final TimeZone timezone) {
    this.renderHeader(output, locale, query);
    this.renderQueryBox(output, query, timezone);
    this.renderQueryConfirmation(output, query, pageNumber, timezone);
    output.append("<ol class='results'>\n");
    for (final Result result : page) {
      this.renderResult(output, result, timezone);
    }
    output.append("</ol>\n");
    this.renderPagination(output, query, pageNumber, isLastPage, timezone);
    this.renderFooter(output);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // HELPERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected void renderHeader(final PrintWriter output,
      final Locale locale, final Query query) {
    final String title = this.getTitle(query);
    output.append(String.format(
        "<!DOCTYPE html>\n" +
        "<html lang='en'>\n" +
        "<head data-accept-language='%s'>\n" +
        "  <meta charset='utf-8'/>\n" +
        "  <title>%s</title>\n" +
        "  <link rel='stylesheet' href='css/bootstrap.min.css'/>\n" +
        "  <link rel='stylesheet' href='css/bootstrap-datetimepicker.min.css'/>\n" +
        "  <link rel='stylesheet' href='css/search.css'/>\n" +
        "</head>\n" +
        "<body>\n" +
        "<nav class='navbar navbar-default'>\n" +
        "  <div class='container'>\n" +
        "    <div class='navbar-header'>\n" +
        "      <a class='navbar-brand' href='#'>Web Archive Search Personalized</a>\n" +
        "    </div>\n" +
        "  </div>\n" +
        "</nav>\n" +
        "<div class='container'>\n",
        locale, title
    ));
  }
  
  protected void renderFooter(final PrintWriter output) {
    output.append(
        "</div>\n" +
        "<script type='text/javascript' src='js/jquery.min.js'></script>\n" +
        "<script type='text/javascript' src='js/moment.min.js'></script>\n" +
        "<script type='text/javascript' src='js/bootstrap.min.js'></script>\n" +
        "<script type='text/javascript' src='js/bootstrap-datetimepicker.min.js'></script>\n" +
        "<script type='text/javascript' src='js/search.js'></script>\n" +
        "</body>\n"
    );
    output.flush();
  }
  
  protected void renderQueryBox(final PrintWriter output,
      final Query query, final TimeZone timezone) {
    String terms = "";
    String from = "";
    String to = "";
    if (query != null) {
      terms = query.getTerms();
      if (query.getFrom() != null) {
        from = this.instantToTimePickerFormat(query.getFrom(), timezone);
      }
      if (query.getTo() != null) {
        to = this.instantToTimePickerFormat(query.getTo(), timezone);
      }
    }
    
    output.append(String.format(
        "<form method='GET' class='form-group'>\n" +
        "  <div class='datetime-form'>\n" +
        "    <div class='input-group date'>\n" +
        "      <span class='input-group-btn' data-button-target-name='from'>\n" +
        "        <span class='btn btn-primary disabled'>From:</span>\n" +
        "        <button type='button' class='btn btn-default' data-time-offset=''>beginning</button>\n" +
        "        <button type='button' class='btn btn-default' data-time-offset='months' data-time-offset-amount='1'>month ago</button>\n" +
        "        <button type='button' class='btn btn-default' data-time-offset='weeks' data-time-offset-amount='1'>week ago</button>\n" +
        "        <button type='button' class='btn btn-default' data-time-offset='days' data-time-offset-amount='1'>day ago</button>\n" +
        "      </span>\n" +
        "      <input type='text' class='form-control' name='from' value='%s' placeholder='beginning'>\n" +
        "      <span class='input-group-addon'>\n" +
        "        <span class='glyphicon glyphicon-calendar'></span>\n" +
        "      </span>\n" +
        "    </div>\n" +
        "    <div class='input-group date'>\n" +
        "      <span class='input-group-btn' data-button-target-name='to'>\n" +
        "        <span class='btn btn-primary disabled'>Until:</span>\n" +
        "        <button type='button' class='btn btn-default' data-time-offset='months' data-time-offset-amount='1'>month ago</button>\n" +
        "        <button type='button' class='btn btn-default' data-time-offset='weeks' data-time-offset-amount='1'>week ago</button>\n" +
        "        <button type='button' class='btn btn-default' data-time-offset='days' data-time-offset-amount='1'>day ago</button>\n" +
        "        <button type='button' class='btn btn-default' data-time-offset=''>now</button>\n" +
        "      </span>\n" +
        "      <input type='text' class='form-control' name='to' value='%s' placeholder='now'>\n" +
        "      <span class='input-group-addon'>\n" +
        "        <span class='glyphicon glyphicon-calendar'></span>\n" +
        "      </span>\n" +
        "    </div>\n" +
        "  </div>\n" +
        "  <div class='input-group'>\n" +
        "    <input type='text' class='form-control' name='terms' placeholder='Query' value='%s'/>\n" +
        "    <span class='input-group-btn'>\n" +
        "      <button type='submit' class='btn btn-primary'>Go!</button>\n" +
        "    </span>\n" +
        "  </div>\n" +
        "  <input type='hidden' name='timezone'/>\n" +
        "</form>\n",
        from , to, terms
    ));
  }
  
  protected void renderQueryConfirmation(
      final PrintWriter output, final Query query,
      final int pageNumber, final TimeZone timezone) {
    output.append(String.format(
        "<div class='current-query'>\n" +
        "  Page <span class='page-number'>%d</span>\n" +
        "  for '<em class='query'>%s</em>'\n" +
        "  from %s\n" +
        "  until %s\n" +
        "</div>\n",
        pageNumber,
        StringEscapeUtils.escapeHtml4(query.getTerms()),
        query.getFrom() == null ? "beginning"
            : this.getHtmlTime(query.getFrom(), timezone),
        query.getTo() == null ? "now"
            : this.getHtmlTime(query.getTo(), timezone)));
  }
  
  protected void renderResult(
      final PrintWriter output, final Result result,
      final TimeZone timezone) {
    final String title = StringEscapeUtils.escapeHtml4(result.getTitle());
    final String replayUri = this.getReplayUri(result);
    final String liveUri = result.getUri();

    output.append(String.format(
        "<li class='result'>\n" +
        "  <div class='links'>\n" +
        "    <a href='%s' class='title'>%s</a>\n" +
        "    <a href='%s' class='archive'>archive</a>\n" +
        "    <a href='%s' class='live'>live</a>\n" +
        "  <span class='meta'><span class='uri'>%s</span> %s</span>\n" +
        "  <span class='snippet'>%s</span>\n" +
        "</li>\n",
        replayUri, title, replayUri, liveUri,
        this.getDisplayUri(result.getUri()),
        this.getHtmlTime(result.getInstant(), timezone),
        this.processSnippet(result.getSnippet())));
  }
  
  protected String getDisplayUri(final String uri) {
    final String htmlEscaped =
        StringEscapeUtils.escapeHtml4(uri);
    if (htmlEscaped.length() <= MAX_URI_DISPLAY_LENGTH) {
      return htmlEscaped;
    } else {
      final int splitIndex = (MAX_URI_DISPLAY_LENGTH - 3) / 2;
      return htmlEscaped.substring(0, splitIndex) + "..." + 
          htmlEscaped.substring(htmlEscaped.length() - splitIndex);
    }
  }
  
  protected String processSnippet(final String snippet) {
    final String htmlEscaped =
        StringEscapeUtils.escapeHtml4(snippet);
    final Pattern highlightStartPattern = Pattern.compile("&lt;em&gt;");
    final String startUnescaped =
        highlightStartPattern.matcher(htmlEscaped).replaceAll(
            "<em class='query'>");
    final Pattern highlightEndPattern = Pattern.compile("&lt;/em&gt;");
    return highlightEndPattern.matcher(startUnescaped).replaceAll("</em>");
  }
  
  protected void renderPagination(
      final PrintWriter output, final Query query,
      final int pageNumber, final boolean isLastPage,
      final TimeZone timezone) {
    final String hrefPrefix = this.getPaginationHrefPrefix(query, timezone);

    output.append(
        "<nav class='footer'>\n" +
        "  <ul class='pagination'>\n"
    );
    output.append(String.format(
        "    <li class='%s'><a href='%s'>&laquo;</a></li>\n",
        pageNumber == 1 ? "disabled" : "",
        pageNumber == 1 ? "#" : hrefPrefix + (pageNumber - 1)));
    for (int p = 1; p < pageNumber; ++p) {
      output.append(String.format(
          "    <li class='page'><a href='%s'>%d</a></li>\n",
          hrefPrefix + p, p));
    }
    output.append(String.format(
        "    <li class='page active'><a href='#'>%d</a></li>\n", pageNumber));
    if (!isLastPage) {
      output.append(String.format(
          "    <li class='page'><a href='%s'>%d</a></li>\n",
          hrefPrefix + (pageNumber + 1), pageNumber + 1));
    }
    output.append(String.format(
        "    <li class='%s'><a href='%s'>&raquo;</a></li>\n",
        isLastPage ? "disabled" : "",
        isLastPage ? "#" : hrefPrefix + (pageNumber + 1)));
    output.append(
        "  </ul>\n" +
        "</nav>"
    );
  }
  
  protected String getTitle(final Query query) {
    final StringBuilder titleBuilder = new StringBuilder();
    titleBuilder.append("Web Archive Search Personalized");
    if (query != null) {
      titleBuilder.append(": ").append(query.getTerms());
    }
    return titleBuilder.toString();
  }
  
  protected String getPaginationHrefPrefix(
      final Query query, final TimeZone timezone) {
    final StringBuilder hrefPrefixBuilder = new StringBuilder();
    try {
      hrefPrefixBuilder.append('?')
        .append(SearchServlet.REQUEST_PARAMETER_TERMS).append('=')
        .append(URLEncoder.encode(query.getTerms(), "UTF-8"));
      if (query.getFrom() != null) {
        hrefPrefixBuilder.append('&')
          .append(SearchServlet.REQUEST_PARAMETER_FROM).append('=')
          .append(this.instantToTimePickerFormat(query.getFrom(), timezone));
      }
      if (query.getTo() != null) {
        hrefPrefixBuilder.append('&')
          .append(SearchServlet.REQUEST_PARAMETER_TO).append('=')
          .append(this.instantToTimePickerFormat(query.getTo(), timezone));
      }
      hrefPrefixBuilder.append("&page=");
      return hrefPrefixBuilder.toString();
    } catch (final UnsupportedEncodingException exception) {
      throw new RuntimeException(exception);
    }
  }
  
  protected String getReplayUri(final Result result) {
    return String.format(
        "http://localhost:%d/%s/%s/%s",
        this.replayPort, this.replayCollection,
        this.getReplayTime(result.getInstant()), result.getUri());
  }
  
  protected String getReplayTime(final Instant instant) {
    return REPLAY_FORMATTER.format(instant);
  }
  
  protected String getHtmlTime(
      final Instant instant, final TimeZone timezone) {
    return String.format("<time datetime='%s'>%s</time>",
        instant, this.instantToTimePickerFormat(instant, timezone));
  }
  
  protected String instantToTimePickerFormat(
      final Instant instant, final TimeZone timezone) {
    return DATE_TIME_PICKER_FORMATTER.format(instant.plus(
          timezone.getOffset(instant.toEpochMilli()), ChronoUnit.MILLIS));
  }

}
