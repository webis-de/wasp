package de.webis.wasp.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import de.webis.wasp.index.Query;
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
    this.query = new UiQuery(query, timeZone);
    
    // TODO UiResult

    final List<UiPaginationLink> pagination = new ArrayList<>();
    // to first
    pagination.add(new UiPaginationLink(
        1, "&laquo;", false, pageNumber == 1));
    // pages
    for (int p = 1; p <= numPages; ++p) {
      pagination.add(new UiPaginationLink(
          p, String.valueOf(p), p == pageNumber, false));
    }
    // to last
    pagination.add(new UiPaginationLink(
        numPages, "&raquo;", false, pageNumber == numPages));
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
    
    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTION
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new query for a WASP page.
     * @param query The original WASP query
     * @param timeZone The time zone of the user client
     */
    protected UiQuery(final Query query, final TimeZone timeZone) {
      this.terms = query.getTerms();
      try {
        this.termsUrl = URLEncoder.encode(this.terms, "UTF-8");
      } catch (final UnsupportedEncodingException exception) {
        throw new RuntimeException(exception);
      }
      this.from = new UiInstant(query.getFrom(), timeZone, true, false);
      this.to = new UiInstant(query.getTo(), timeZone, false, true);
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
        this.timePickerValue = DATE_TIME_PICKER_FORMATTER.format(instant.plus(
            timeZone.getOffset(instant.toEpochMilli()), ChronoUnit.MILLIS));
        this.replayPathValue = REPLAY_FORMATTER.format(instant);
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

    public final boolean isCurrent;

    public final boolean isDisabled;
    
    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTION
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new pagination link for a WASP page.
     * @param number The target page number
     * @param text The text to show
     * @param isCurrent Whether this link leads to the current page
     * @param isDisabled Whether this link is disabled
     */
    public UiPaginationLink(
        final int number, final String text,
        final boolean isCurrent, final boolean isDisabled) {
      this.number = number;
      this.text = text;
      this.isCurrent = isCurrent;
      this.isDisabled = isDisabled;
    }
    
  }

}
