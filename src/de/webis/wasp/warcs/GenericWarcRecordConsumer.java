package de.webis.wasp.warcs;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Instant;

import org.apache.http.HttpResponse;

import edu.cmu.lemurproject.WarcRecord;

/**
 * Generic class for consuming WARC records with methods for different records.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public abstract class GenericWarcRecordConsumer
implements Consumer<WarcRecord> {

  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(GenericWarcRecordConsumer.class.getName());
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////

  @Override
  public void accept(final WarcRecord record) {
    final String type = Warcs.getType(record);

    final Instant time =
        Instant.ofEpochSecond(Warcs.getDate(record).getEpochSecond());
    try {
      switch (type) {
      case Warcs.HEADER_TYPE_RESPONSE:
        this.acceptResponse(record, time);
        break;
      case Warcs.HEADER_TYPE_REQUEST:
        this.acceptRequest(record, time);
        break;
      case Warcs.HEADER_TYPE_REVISIT:
        this.acceptRevisit(record, time);
        break;
      default:
        break;
      }
    } catch (final Throwable exception) {
      LOG.log(Level.WARNING, "Failed to index record " + Warcs.getId(record)
          + " of type " + Warcs.getType(record), exception);
    }
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // Response

  protected void acceptResponse(final WarcRecord record, final Instant time)
  throws IOException {
    final String id = Warcs.getId(record);
    final String html = this.getHtml(record);
    LOG.fine("accept response " + id + " html = " + (html != null));
    if (html != null) {
      this.acceptHtmlResponse(id, html, time);
    } else {
      this.acceptNonHtmlResponse(id, time);
    }
  }

  protected void acceptNonHtmlResponse(
      final String id, final Instant time)
  throws IOException {
    // do nothing by default
  }

  protected void acceptHtmlResponse(
      final String id, final String html, final Instant time)
  throws IOException {
    // do nothing by default
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // Revisit

  protected void acceptRevisit(final WarcRecord record, final Instant time)
  throws IOException {
    this.acceptRevisit(
        Warcs.getId(record),
        Warcs.getReferedToRecordId(record),
        time);
  }

  protected void acceptRevisit(
      final String id, final String revisitedId, final Instant time)
  throws IOException {
    // do nothing by default
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // Request
  
  protected void acceptRequest(final WarcRecord record, final Instant time)
  throws IOException {
    this.acceptRequest(
        Warcs.getConcurrentRecordId(record),
        Warcs.getTargetUri(record),
        time);
  }
  
  protected void acceptRequest(
      final String concurrentRecordId,
      final String targetUri,
      final Instant time)
  throws IOException {
    // do nothing by default
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // HELPERS
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Gets the HTML from a response WARC record.
   * @param record The record
   * @return The HTML if it exists, or <code>null</code>
   */
  protected String getHtml(final WarcRecord record) {
    try {
      final HttpResponse response = Warcs.toResponse(record);
      if (Warcs.isHtml(response)) {
        return Warcs.getHtml(record);
      }
    } catch (final Throwable exception) {
      LOG.log(Level.FINER,
          "Could not parse record " + Warcs.getId(record),
          exception);
    }
    return null;
  }
  
}
