package de.webis.warc.index;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.joda.time.Instant;

import de.webis.warc.Warcs;
import edu.cmu.lemurproject.WarcRecord;
import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

public class WarcIndexer implements Consumer<WarcRecord> {

  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(WarcIndexer.class.getName());
  
  /////////////////////////////////////////////////////////////////////////////
  // DEFAULTS
  /////////////////////////////////////////////////////////////////////////////
  
  public static final Function<Source, String> DEFAULT_HTML_CONTENT_EXTRACTOR =
      source -> {
        final Renderer renderer = new Renderer(source);
        renderer.setMaxLineLength(0);
        renderer.setIncludeHyperlinkURLs(false);
        renderer.setIncludeAlternateText(true);
        return renderer.toString();
      };
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final Index index;
  
  protected final Function<Source, String> htmlContentExtractor;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  public WarcIndexer(final Index index) {
    this(index, DEFAULT_HTML_CONTENT_EXTRACTOR);
  }
  
  public WarcIndexer(
      final Index index,
      final Function<Source, String> htmlContentExtractor) {
    if (index == null) { throw new NullPointerException("index"); }
    if (htmlContentExtractor == null) {
      throw new NullPointerException("HTML Content Extractor");
    }
    
    this.index = index;
    this.htmlContentExtractor = htmlContentExtractor;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////

  @Override
  public void accept(final WarcRecord record) {
    try {
      this.index(record);
    } catch (final IOException exception) {
      LOG.log(Level.WARNING, "Failed to index record " + Warcs.getId(record)
          + " of type " + Warcs.getType(record), exception);
    }
  }

  public void index(final WarcRecord record)
  throws IOException {
    final String type = Warcs.getType(record);
    switch (type) {
    case Warcs.HEADER_TYPE_RESPONSE:
      this.indexResponse(record);
      break;
    case Warcs.HEADER_TYPE_REQUEST:
      this.indexRequest(record);
      break;
    case Warcs.HEADER_TYPE_REVISIT:
      this.indexRevisit(record);
      break;
    default:
      break;
    }
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // Response
  
  protected void indexResponse(final WarcRecord record)
  throws IOException {
    final String id = Warcs.getId(record);
    final Source source = this.getSource(record);
    if (source != null) {
      final String content = this.getContent(source);
      if (content != null) {
        final String title = this.getTitle(source);
        this.index.indexResponse(id, content, title);
      }
    }
  }
  
  protected Source getSource(final WarcRecord record) {
    try {
      final HttpResponse response = Warcs.toResponse(record);
      if (Warcs.isHtml(response)) {
        final String html = Warcs.getHtml(record);
        return new Source(html);
      }
    } catch (final Throwable exception) {
      LOG.log(Level.FINER,
          "Could not parse record " + Warcs.getId(record),
          exception);
    }
    return null;
  }
  
  protected String getContent(final Source source) {
    try {
      return this.htmlContentExtractor.apply(source);
    } catch (final Throwable exception) {
      LOG.log(Level.FINER,
          "Could not parse source",
          exception);
    }
    return null;
  }
  
  protected String getTitle(final Source source) {
    final Element title = source.getFirstElement(HTMLElementName.TITLE);
    if (title == null) {
      return "";
    } else {
      return CharacterReference.decodeCollapseWhiteSpace(title.getContent());
    }
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // Revisit
  
  protected void indexRevisit(final WarcRecord record)
  throws IOException {
    this.index.indexRevisit(
        Warcs.getId(record),
        Warcs.getReferedToRecordId(record));
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // Request
  
  protected void indexRequest(final WarcRecord record) throws IOException {
    this.index.indexRequest(
        Warcs.getConcurrentRecordId(record),
        Warcs.getTargetUri(record),
        new Instant(Warcs.getDate(record).getEpochSecond()));
  }
  
}
