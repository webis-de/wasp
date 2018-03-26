package de.webis.warc.read;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

import de.webis.warc.WarcRecordPair;
import de.webis.warc.Warcs;
import edu.cmu.lemurproject.WarcRecord;

/**
 * Wrapper that matches the requests and responses it consumes and forwards them
 * paired to another consumer.
 * <p>
 * This consumer assumes that in the WARC file the responses are always preceded
 * by their requests (as warcprox does it). Also, it ignores revisit records
 * (which will not occur when deduplication is turned off).
 * </p>
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class RecordMatchingConsumer implements Consumer<WarcRecord> {
  
  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(RecordMatchingConsumer.class.getName());
  
  /////////////////////////////////////////////////////////////////////////////
  // DEFAULTS
  /////////////////////////////////////////////////////////////////////////////
  
  protected static final int DEFAULT_CACHE_SIZE = 100;
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final Map<String, WarcRecord> recordCache;
  
  protected final Consumer<WarcRecordPair> matchedRecordsConsumer;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Creates a new consumer that forwards the matched request-response pairs to
   * the provided other consumer.
   * @param matchedRecordsConsumer The consumer to forward matched pairs to
   */
  public RecordMatchingConsumer(
      final Consumer<WarcRecordPair> matchedRecordsConsumer) {
    this(matchedRecordsConsumer, DEFAULT_CACHE_SIZE);
  }

  /**
   * Creates a new consumer that forwards the matched request-response pairs to
   * the provided other consumer.
   * @param matchedRecordsConsumer The consumer to forward matched pairs to
   * @param cacheSize The number of responses to keep in the cache until the
   * earliest unused response is dropped
   */
  public RecordMatchingConsumer(
      final Consumer<WarcRecordPair> matchedRecordsConsumer,
      final int cacheSize) {
    if (matchedRecordsConsumer == null) { throw new NullPointerException(); }
    this.matchedRecordsConsumer = matchedRecordsConsumer;
    this.recordCache = new Cache<>(cacheSize);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////

  @Override
  public void accept(final WarcRecord record) {
    final String type = Warcs.getType(record); 
    if (type.equals(Warcs.HEADER_TYPE_REQUEST)) {
      this.acceptRequest(record);
    } else if (type.equals(Warcs.HEADER_TYPE_RESPONSE)) {
      this.acceptResponse(record);
    } else {
      LOG.finer("Unused record of type " + type);
    }
  }
  
  protected void acceptRequest(final WarcRecord request) {
    final String responseId = Warcs.getConcurrentRecordId(request);
    final WarcRecord response = this.recordCache.remove(responseId);
    if (response == null) {
      LOG.severe("Request with unknown response " + responseId);
    } else {
      final WarcRecordPair pair = new WarcRecordPair(request, response);
      this.matchedRecordsConsumer.accept(pair);
    }
  }
  
  protected void acceptResponse(final WarcRecord response) {
    final String id = Warcs.getId(response);
    this.recordCache.put(id, response);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // HELPER CLASSES
  /////////////////////////////////////////////////////////////////////////////

  protected static class Cache<T> extends LinkedHashMap<String, T> {

    private static final long serialVersionUID = 8152191596052811280L;
    
    protected final int size;
    
    public Cache(final int size) {
      this.size = size;
    }
    
    @Override
    protected boolean removeEldestEntry(
        final java.util.Map.Entry<String, T> eldest) {
      return this.size() > this.size;
    }
    
  }
  

}
