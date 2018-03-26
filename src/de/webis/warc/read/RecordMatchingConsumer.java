package de.webis.warc.read;

import java.util.function.Consumer;
import java.util.logging.Logger;

import de.webis.warc.WarcRecordPair;
import de.webis.warc.Warcs;
import edu.cmu.lemurproject.WarcRecord;

public class RecordMatchingConsumer implements Consumer<WarcRecord> {
  
  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(RecordMatchingConsumer.class.getName());
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final Consumer<WarcRecordPair> matchedRecordsConsumer;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  public RecordMatchingConsumer(
      final Consumer<WarcRecordPair> matchedRecordsConsumer) {
    if (matchedRecordsConsumer == null) { throw new NullPointerException(); }
    this.matchedRecordsConsumer = matchedRecordsConsumer;
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
  
  protected void acceptRequest(final WarcRecord record) {
    final String targetUri = Warcs.getTargetUri(record);
  }
  
  protected void acceptResponse(final WarcRecord record) {
    
  }
  

}
