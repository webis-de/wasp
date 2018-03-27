package de.webis.warc.index;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;

import de.webis.warc.Warcs;
import edu.cmu.lemurproject.WarcRecord;

public class WarcIndexer implements Consumer<WarcRecord> {

  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(WarcIndexer.class.getName());
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final Index index;
  
  protected final Function<String, String> htmlContentExtractor;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  public WarcIndexer(
      final Index index,
      final Function<String, String> htmlContentExtractor) {
    if (index == null) { throw new NullPointerException(); }
    
    this.index = index;
    if (htmlContentExtractor == null) {
      this.htmlContentExtractor = Function.identity();
    } else {
      this.htmlContentExtractor = htmlContentExtractor;
    }
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////

  @Override
  public void accept(final WarcRecord record) {
    try {
      this.index(record);
    } catch (final IOException | HttpException exception) {
      LOG.log(Level.WARNING, "Failed to index record " + Warcs.getId(record)
          + " of type " + Warcs.getType(record), exception);
    }
  }

  public void index(final WarcRecord record)
  throws IOException, HttpException {
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
  
  protected void indexResponse(final WarcRecord record)
  throws IOException, HttpException {
    final String id = Warcs.getId(record);

    final HttpResponse response = Warcs.toResponse(record);
    if (Warcs.isHtml(response)) {
      final String html = Warcs.getHtml(record);
      final String content = this.htmlContentExtractor.apply(html);
      
      if (content != null) {
        this.index.indexResponse(id, content);
      }
    }
  }
  
  protected void indexRevisit(final WarcRecord record) throws IOException {
    this.index.indexRevisit(
        Warcs.getId(record),
        Warcs.getReferedToRecordId(record));
  }
  
  protected void indexRequest(final WarcRecord record) throws IOException {
    this.index.indexRequest(
        Warcs.getConcurrentRecordId(record),
        Warcs.getTargetUri(record),
        Warcs.getDate(record).getEpochSecond());
  }
  
}
