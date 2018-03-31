package de.webis.warc.ui;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import org.apache.commons.text.StringEscapeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.webis.warc.index.Query;
import de.webis.warc.index.Result;

public class ResultPageRenderer {
  
  protected static final DateTimeFormatter REPLAY_FORMATTER =
      DateTimeFormat.forPattern("yyyyMMddHHmmss");
  
  public static final DateTimeFormatter DATE_TIME_PICKER_FORMATTER =
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
  
  protected final int replayPort;
  
  protected final String replayCollection;
  
  public ResultPageRenderer() {
    this(SearchServlet.DEFAULT_REPLAY_PORT, SearchServlet.DEFAULT_REPLAY_COLLECTION);
  }
  
  public ResultPageRenderer(
      final int replayPort, final String replayCollection) {
    if (replayCollection == null) {
      throw new NullPointerException("collection");
    }
    this.replayPort = replayPort;
    this.replayCollection = replayCollection;
  }

  public void render(final PrintWriter output,
      final Locale locale, final DateTimeZone timezone) {
    this.renderHeader(output, locale, null);
    this.renderQueryBox(output, null, timezone);
    this.renderFooter(output);
  }
  
  public void render(final PrintWriter output,
      final Query query,
      final List<Result> page, final int pageNumber, final boolean isLastPage,
      final Locale locale, final DateTimeZone timezone) {
    this.renderHeader(output, locale, query);
    this.renderQueryBox(output, query, timezone);
    this.renderQueryConfirmation(output, query, timezone);
    output.append("<ol class='results'>\n");
    for (final Result result : page) {
      this.renderResult(output, result, timezone);
    }
    output.append("</ol>\n");
    this.renderPagination(output, query, pageNumber, isLastPage, timezone);
    this.renderFooter(output);
  }
  
  protected void renderHeader(final PrintWriter output,
      final Locale locale, final Query query) {
    final String title = this.getTitle(query);
    output.append(String.format(
        "<!DOCTYPE html>\n" +
        "<html lang='en'>\n" +
        "<head data-accept-language='%s'>\n" +
        "  <meta charset='utf-8'/>" +
        "  <title>%s</title>" +
        "  <link rel='stylesheet' href='css/bootstrap.min.css'/>" +
        "  <link rel='stylesheet' href='css/bootstrap-datetimepicker.min.css'/>" +
        "  <link rel='stylesheet' href='css/search.css'/>" +
        "</head>\n" +
        "<body>\n",
        locale, title
    ));
  }
  
  protected void renderFooter(final PrintWriter output) {
    output.append(
        "<script type='text/javascript' src='js/jquery.min.js'></script>\n" +
        "<script type='text/javascript' src='js/moment.min.js'></script>\n" +
        "<script type='text/javascript' src='js/bootstrap.min.js'></script>\n" +
        "<script type='text/javascript' src='js/bootstrap-datetimepicker.min.js'></script>\n" +
        "<script type='text/javascript' src='js/localize-time.js'></script>\n" +
        "</body>\n"
    );
    output.flush();
  }
  
  protected void renderQueryBox(final PrintWriter output,
      final Query query, final DateTimeZone timezone) {
    String terms = "";
    String from = "";
    String to = "";
    if (query != null) {
      terms = query.getTerms();
      if (query.getFrom() != null) {
        from = this.getDateTimePickerValue(query.getFrom(), timezone);
      }
      if (query.getTo() != null) {
        to = this.getDateTimePickerValue(query.getTo(), timezone);
      }
    }
    
    output.append(String.format(
        "<form method='GET' class='form-group'>\n" +
        "  <input type='text' name='terms' placeholder='query' value='%s'/>\n" +
        "  <div class='input-group date'>\n" +
        "    <input type='text' class='form-control 'name='from' value='%s' placeholder='from'>\n" +
        "    <span class='input-group-addon'>\n" +
        "      <span class='glyphicon glyphicon-calendar'></span>\n" +
        "    </span>\n" +
        "  </div>\n" +
        "  <div class='input-group date'>\n" +
        "    <input type='text' class='form-control 'name='to' value='%s' placeholder='to'>\n" +
        "    <span class='input-group-addon'>\n" +
        "      <span class='glyphicon glyphicon-calendar'></span>\n" +
        "    </span>\n" +
        "  </div>\n" +
        "  <input type='hidden' name='timezone'/>\n" +
        "  <button type='submit'>Go!</button>" +
        "</form>\n",
        terms, from , to
    ));
  }
  
  protected void renderQueryConfirmation(
      final PrintWriter output, final Query query,
      final DateTimeZone timezone) {
    output.append(String.format(
        "<div class='current-query'>\n" +
        "  Results for query '<span class='query'>%s</span>'\n",
        StringEscapeUtils.escapeHtml4(query.getTerms())));
    if (query.getFrom() != null) {
      output.append(String.format("  from %s\n",
          this.getHtmlTime(query.getFrom(), timezone)));
    }
    if (query.getTo() != null) {
      output.append(String.format("  until %s\n",
          this.getHtmlTime(query.getTo(), timezone)));
    }
    output.append(
        "</div>\n");
  }
  
  protected void renderResult(
      final PrintWriter output, final Result result,
      final DateTimeZone timezone) {
    output.append(String.format(
        "<li class='result'>\n" +
        "  <span class='title'><a href='%s'>%s</a></span><br/>\n" +
        "  <span class='uri'>%s</span> %s<br/>\n" +
        "  <span class='snippet'>%s</span>\n" +
        "</li>\n",
        this.getReplayUri(result),
        StringEscapeUtils.escapeHtml4(result.getTitle()),
        StringEscapeUtils.escapeHtml4(result.getUri()),
        this.getHtmlTime(result.getInstant(), timezone),
        StringEscapeUtils.escapeHtml4(result.getSnippet())));
  }
  
  protected void renderPagination(
      final PrintWriter output, final Query query,
      final int pageNumber, final boolean isLastPage
      , final DateTimeZone timezone) {
    final String hrefPrefix = this.getPaginationHrefPrefix(query, timezone);

    output.append("<ol class='pagination'>\n");
    for (int p = 1; p < pageNumber; ++p) {
      output.append(String.format(
          "  <li class='page'><a href='%s'>%d</a></li>\n",
          hrefPrefix + p, p));
    }
    output.append(String.format(
        "  <li class='page active'>%d</li>\n", pageNumber));
    if (!isLastPage) {
      output.append(String.format(
          "  <li class='page'><a href='%s'>%d</a></li>\n",
          hrefPrefix + (pageNumber + 1), pageNumber + 1));
    }
    output.append("</ol>\n");
  }
  
  protected String getTitle(final Query query) {
    final StringBuilder titleBuilder = new StringBuilder();
    titleBuilder.append("Web Archive Search Personalized");
    if (query != null) {
      titleBuilder.append(": ").append(query.getTerms());
    }
    return titleBuilder.toString();
  }
  
  protected String getDateTimePickerValue(
      final Instant instant, final DateTimeZone timezone) {
    return instant.plus(timezone.getOffset(instant))
        .toString(DATE_TIME_PICKER_FORMATTER);
  }
  
  protected String getPaginationHrefPrefix(
      final Query query, final DateTimeZone timezone) {
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
    return instant.toString(REPLAY_FORMATTER);
  }
  
  protected String getHtmlTime(
      final Instant instant, final DateTimeZone timezone) {
    return String.format("<time datetime='%s'>%s</time>",
        instant, this.instantToTimePickerFormat(instant, timezone));
  }
  
  protected String instantToTimePickerFormat(
      final Instant instant, final DateTimeZone timezone) {
    return instant.plus(timezone.getOffset(instant))
        .toString(DATE_TIME_PICKER_FORMATTER);
  }

}
