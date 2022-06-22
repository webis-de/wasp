package de.webis.wasp.ui;

import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import de.webis.wasp.index.Query;
import de.webis.wasp.index.RequestRecord;
import de.webis.wasp.index.ResponseRecord;
import de.webis.wasp.index.Result;

/**
 * Model for a WASP user interface web page.
 * 
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class UiPage {
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTANTS
  /////////////////////////////////////////////////////////////////////////////
  
  protected static final int MAX_URI_DISPLAY_LENGTH = 60;
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////

  public final String replayServer;

  public final String replayCollection;

  public final String locale;

  public final UiQuery query;

  public final List<UiResult> results;

  public final List<UiPaginationLink> pagination;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTION
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Create a page without query or results.
   * @param replayServer The URL of the replay server (including port and
   * optional path up to the collection name)
   * @param replayCollection The name of the collection to replay from
   * @param locale Locale of the user client
   * @param timeZone Time zone of the user client
   */
  public UiPage(
      final String replayServer, final String replayCollection,
      final Locale locale, final TimeZone timeZone) {
    this.replayServer = Objects.requireNonNull(replayServer);
    this.replayCollection = Objects.requireNonNull(replayCollection);
    this.locale = locale.toString();
    this.query = null;
    this.results = List.of();
    this.pagination = List.of();
  }

  /**
   * Create a page with query and results.
   * @param replayServer The URL of the replay server (including port and
   * optional path up to the collection name)
   * @param replayCollection The name of the collection to replay from
   * @param query The query for which the results were retrieved
   * @param paginatedResults The results for the specific page
   * @param pageNumber The number of the result page for the query
   * @param numPages The number of available result pages for the query
   * @param locale The locale of the user client
   * @param timeZone The time zone of the user client
   */
  public UiPage(
      final String replayServer, final String replayCollection,
      final Query query, final List<Result> paginatedResults,
      final int pageNumber, final int numPages,
      final Locale locale, final TimeZone timeZone) {
    this.replayServer = Objects.requireNonNull(replayServer);
    this.replayCollection = Objects.requireNonNull(replayCollection);
    this.locale = locale.toString();
    this.query = new UiQuery(query, pageNumber, timeZone);

    final List<UiResult> results = new ArrayList<>();
    for (final Result result : paginatedResults) {
      results.add(
          new UiResult(replayServer, replayCollection, result, timeZone));
    }
    this.results = Collections.unmodifiableList(results);

    final List<UiPaginationLink> pagination = new ArrayList<>();
    final StringBuilder hrefBaseBuilder = new StringBuilder();
    try {
      hrefBaseBuilder.append('?')
        .append(SearchServlet.REQUEST_PARAMETER_TERMS).append('=')
        .append(URLEncoder.encode(query.getTerms(), "UTF-8"));
      if (query.getFrom() != null) {
        hrefBaseBuilder.append('&')
          .append(SearchServlet.REQUEST_PARAMETER_FROM).append('=')
          .append(URLEncoder.encode(this.query.from.timePickerValue, "UTF-8"));
      }
      if (query.getTo() != null) {
        hrefBaseBuilder.append('&')
          .append(SearchServlet.REQUEST_PARAMETER_TO).append('=')
          .append(URLEncoder.encode(this.query.to.timePickerValue, "UTF-8"));
      }
      hrefBaseBuilder.append("&page=");
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    final String hrefBase = hrefBaseBuilder.toString();
    // to first
    pagination.add(new UiPaginationLink(
        1, "&laquo;", hrefBase + "1",
        false, pageNumber == 1));
    // pages
    for (int p = 1; p <= numPages; ++p) {
      pagination.add(new UiPaginationLink(
          p, String.valueOf(p), hrefBase + p,
          p == pageNumber, false));
    }
    // to last
    pagination.add(new UiPaginationLink(
        numPages, "&raquo;", hrefBase + numPages,
        false, pageNumber == numPages));
    this.pagination = Collections.unmodifiableList(pagination);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // HELPER CLASSES

  /**
   * Model for a WASP query in a user interface web page.
   * 
   * @author johannes.kiesel@uni-weimar.de
   *
   */
  public static final class UiQuery {
    
    ///////////////////////////////////////////////////////////////////////////
    // MEMBERS
    ///////////////////////////////////////////////////////////////////////////
    
    public final String terms;

    public final String termsUrl;

    public final UiInstant from;

    public final UiInstant to;

    public final int pageNumber;
    
    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTION
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new query for a WASP page.
     * @param query The original WASP query
     * @param timeZone The time zone of the user client
     */
    protected UiQuery(
        final Query query, final int pageNumber, final TimeZone timeZone) {
      this.terms = query.getTerms();
      try {
        this.termsUrl = URLEncoder.encode(this.terms, "UTF-8");
      } catch (final UnsupportedEncodingException exception) {
        throw new RuntimeException(exception);
      }
      this.from = new UiInstant(query.getFrom(), timeZone, true, false);
      this.to = new UiInstant(query.getTo(), timeZone, false, true);
      this.pageNumber = pageNumber;
    }

  }

  /**
   * Model for a WASP result in a user interface web page.
   * 
   * @author johannes.kiesel@uni-weimar.de
   *
   */
  public static final class UiResult {
    
    ///////////////////////////////////////////////////////////////////////////
    // MEMBERS
    ///////////////////////////////////////////////////////////////////////////
    
    public final String title;

    public final UiInstant date;

    public final String liveUri;

    public final String liveUriShortened;

    public final String replayUri;

    public final String snippet;
    
    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTION
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new result for a WASP page.
     * @param replayServer The URL of the replay server (including port and
     * optional path up to the collection name)
     * @param replayCollection The name of the collection to replay from
     * @param result One result retrieved for the query
     * @param timeZone The time zone of the user client
     */
    protected UiResult(
        final String replayServer, final String replayCollection,
        final Result result, final TimeZone timeZone) {
      this.title = result.getResponse().getTitle();

      this.date = new UiInstant(
          result.getMatchedRequest().getDate(), timeZone, false, false);

      this.liveUri = result.getMatchedRequest().getUri();
      if (this.liveUri.length() <= MAX_URI_DISPLAY_LENGTH) {
        this.liveUriShortened = this.liveUri;
      } else {
        final int splitIndex = (MAX_URI_DISPLAY_LENGTH - 3) / 2;
        this.liveUriShortened = this.liveUri.substring(0, splitIndex) + "..."
            + this.liveUri.substring(this.liveUri.length() - splitIndex);
      }

      this.replayUri = String.format("%s/%s/%s/%s",
          Objects.requireNonNull(replayServer),
          Objects.requireNonNull(replayCollection),
          this.date.replayPathValue,
          this.liveUri);
      
      this.snippet = result.getSnippet();
      /*
       * StringEscapeUtils.escapeHtml4(
       * 
        final Pattern highlightStartPattern = Pattern.compile("&lt;em&gt;");
        final String startUnescaped =
            highlightStartPattern.matcher(htmlEscaped).replaceAll(
                "<em class='query'>");
        final Pattern highlightEndPattern = Pattern.compile("&lt;/em&gt;");
        return highlightEndPattern.matcher(startUnescaped).replaceAll("</em>");
       */
    }

  }

  /**
   * Model for an instant in a user interface web page.
   * 
   * @author johannes.kiesel@uni-weimar.de
   *
   */
  public static final class UiInstant {
    
    ///////////////////////////////////////////////////////////////////////////
    // CONSTANTS
    ///////////////////////////////////////////////////////////////////////////
    
    protected static final DateTimeFormatter DATE_TIME_PICKER_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    protected static final DateTimeFormatter REPLAY_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    ///////////////////////////////////////////////////////////////////////////
    // MEMBERS
    ///////////////////////////////////////////////////////////////////////////

    public final String text;

    public final String iso;

    public final String timePickerValue;

    public final String replayPathValue;
    
    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTION
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new instant for a WASP page.
     * @param instant The instant or <ocde>null</code> for none
     * @param timeZone The time zone of the user client
     * @param isFrom Whether this instant denotes the start of a time interval
     * @param isTo Whether this instant denotes the end of a time interval
     */
    protected UiInstant(
        final Instant instant, final TimeZone timeZone,
        final boolean isFrom, final boolean isTo) {
      if (instant == null) {
        this.iso = null;
        this.timePickerValue = null;
        this.replayPathValue = null;
        if (isFrom) {
          this.text = "beginning";
        } else if (isTo) {
          this.text = "now";
        } else {
          this.text = null;
        }
      } else {
        this.iso = instant.toString();
        this.timePickerValue = DATE_TIME_PICKER_FORMATTER.format(instant
            .atZone(timeZone.toZoneId()));
        this.replayPathValue = REPLAY_FORMATTER.format(instant
            .atOffset(ZoneOffset.UTC));
        this.text = this.timePickerValue;
      }
    }
    
  }

  /**
   * Model for a link to a different result page in a user interface web page.
   * 
   * @author johannes.kiesel@uni-weimar.de
   *
   */
  public static final class UiPaginationLink {
    
    ///////////////////////////////////////////////////////////////////////////
    // MEMBERS
    ///////////////////////////////////////////////////////////////////////////

    public final int number;

    public final String text;

    public final String link;

    public final boolean isActive;

    public final boolean isDisabled;
    
    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTION
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new pagination link for a WASP page.
     * @param number The target page number
     * @param text The text to show
     * @param link The link to the page
     * @param isActive Whether this link leads to the current page
     * @param isDisabled Whether this link is disabled
     */
    public UiPaginationLink(
        final int number, final String text, final String link,
        final boolean isActive, final boolean isDisabled) {
      this.number = number;
      this.text = Objects.requireNonNull(text);
      this.link = Objects.requireNonNull(link);
      this.isActive = isActive;
      this.isDisabled = isDisabled;
    }
    
  }

  public static void main(String[] args) {
    final MustacheFactory factory = new DefaultMustacheFactory();
    final Mustache pageRenderer = factory.compile(new InputStreamReader(
        SearchServlet.class.getResourceAsStream("search.mustache")),
        "search.mustache");
    final Query query = new Query("foo bar", null, Instant.now());
    final List<Result> results = List.of(
        new Result(0.5, "my snippet",
            new ResponseRecord("foo", "bar", null, null),
            new RequestRecord("https://webis.de", Instant.now())),
        new Result(0.25, "my second snippet",
            new ResponseRecord("foo2", "bar2", null, null),
            new RequestRecord("https://webis.de", Instant.now())));
    final int pageNumber = 1;
    final int numPages = 3;
    final UiPage page = new UiPage(
        "https://wasp.de", "mywasp",
        query, results, pageNumber, numPages,
        Locale.ENGLISH, TimeZone.getDefault());
    final StringWriter writer = new StringWriter();
    pageRenderer.execute(writer, page);
    System.out.println(writer.toString());
  }

}
