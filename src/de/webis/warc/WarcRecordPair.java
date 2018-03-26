package de.webis.warc;

import edu.cmu.lemurproject.WarcRecord;

/**
 * A request with associated response.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class WarcRecordPair {
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final WarcRecord request;
  
  protected final WarcRecord response;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Creates a new request-response record pair.
   */
  public WarcRecordPair(
      final WarcRecord request, final WarcRecord response) {
    if (request == null) { throw new NullPointerException(); }
    if (response == null) { throw new NullPointerException(); }
    
    final String requestType = Warcs.getType(request);
    if (!requestType.equals(Warcs.HEADER_TYPE_REQUEST)) {
      throw new IllegalArgumentException(
          "Not a request but a " + requestType);
    }

    final String responseType = Warcs.getType(response);
    if (!responseType.equals(Warcs.HEADER_TYPE_RESPONSE)) {
      throw new IllegalArgumentException(
          "Not a response but a " + responseType);
    }
    
    this.request = request;
    this.response = response;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  public WarcRecord getRequest() {
    return this.request;
  }
  
  public WarcRecord getResponse() {
    return this.response;
  }

}
