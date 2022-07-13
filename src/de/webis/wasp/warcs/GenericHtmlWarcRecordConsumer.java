package de.webis.wasp.warcs;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

/**
 * Generic class for consuming HTML WARC records.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public abstract class GenericHtmlWarcRecordConsumer
extends GenericWarcRecordConsumer {

  /////////////////////////////////////////////////////////////////////////////
  // CONSTANTS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Default function for extracting HTML from response records.
   */
  public static final Function<String, Document> DEFAULT_DOCUMENT_EXTRACTOR =
      JerichoDocumentExtractor.INSTANCE;
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////

  private Function<String, Document> documentExtractor;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTION
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a new consumer using the default extractor for HTML responses.
   */
  public GenericHtmlWarcRecordConsumer() {
    this(DEFAULT_DOCUMENT_EXTRACTOR);
  }

  /**
   * Creates a new consumer using the specified extractor for HTML responses.
   * @param documentExtractor The extractor
   */
  public GenericHtmlWarcRecordConsumer(
      final Function<String, Document> documentExtractor) {
    this.setDocumentExtractor(documentExtractor);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // GETTERS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Gets the document extractor for HTML responses.
   * @return The extractor
   */
  public Function<String, Document> getDocumentExtractor() {
    return this.documentExtractor;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // SETTERS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Sets the document extractor for HTML responses.
   * @param documentExtractor The extractor
   */
  protected void setDocumentExtractor(
      final Function<String, Document> documentExtractor) {
    this.documentExtractor = Objects.requireNonNull(documentExtractor);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////

  @Override
  protected void acceptHtmlResponse(
      final String id, final String uri, final String html, final Instant time)
  throws IOException {
    final Document document = this.getDocumentExtractor().apply(html);
    this.acceptHtmlResponse(id, uri, document, time);
  }

  protected abstract void acceptHtmlResponse(
      final String id, final String uri, final Document document,
      final Instant time)
  throws IOException;

  /////////////////////////////////////////////////////////////////////////////
  // DOCUMENT
  /////////////////////////////////////////////////////////////////////////////

  /**
   * A processed document.
   *
   * @author johannes.kiesel@uni-weimar.de
   *
   */
  public static final class Document {

    private final String title;

    private final String content;

    /**
     * Creates a new document.
     * @param title The document's title (or <code>null</code>)
     * @param content The document's content (or <code>null</code>)
     */
    public Document(final String title, final String content) {
      this.title = title;
      this.content = content;
    }

    /**
     * Gets the title of the document.
     * @return The title (may be <code>null</code> or empty)
     */
    public String getTitle() {
      return this.title;
    }

    /**
     * Gets the text content of the document.
     * @return The content (may be <code>null</code> or empty)
     */
    public String getContent() {
      return this.content;
    }
    
  }

}
