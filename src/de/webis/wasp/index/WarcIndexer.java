package de.webis.wasp.index;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;
import java.time.Instant;

import de.webis.wasp.warcs.GenericHtmlWarcRecordConsumer;

/**
 * Consumer to index WARC records.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class WarcIndexer
extends GenericHtmlWarcRecordConsumer {

  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(WarcIndexer.class.getName());
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  private final Index index;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a new consumer that indexes to the specified index.
   * @param index The index
   */
  public WarcIndexer(final Index index) {
    this.index = Objects.requireNonNull(index);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // GETTERS
  /////////////////////////////////////////////////////////////////////////////

  public Index getIndex() {
    return this.index;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////

  @Override
  protected void acceptHtmlResponse(
      final String id, final Document document, final Instant time)
  throws IOException {
    String title = document.getTitle();
    if (title == null) { title = ""; }
    String content = document.getContent();
    if (content == null) { content = ""; }
    LOG.fine("accept html response " + id
        + " title = '" + title + "' content exists = " + !content.isEmpty());
    if (!title.isEmpty() || !content.isEmpty()) {
      this.getIndex().indexResponse(id, content, title);
    }
  }

  @Override
  protected void acceptRevisit(
      final String id, final String revisitedId, final Instant time)
  throws IOException {
    this.getIndex().indexRevisit(revisitedId, id);
  }

  @Override
  protected void acceptRequest(
      final String concurrentRecordId,
      final String targetUri,
      final Instant time)
  throws IOException {
    this.getIndex().indexRequest(concurrentRecordId, targetUri, time);
  }
  
}
