package de.webis.warc;

import java.time.format.DateTimeFormatter;

/**
 * Utility class for working with WARC files.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class Warcs {
  
  public static final String HEADER_ID = "WARC-Record-ID";
  
  public static final String HEADER_TYPE = "WARC-Type";
  
  public static final String HEADER_TYPE_INFO = "warcinfo";
  
  public static final String HEADER_TYPE_REQUEST = "request";
  
  public static final String HEADER_TYPE_RESPONSE = "response";
  
  public static final String HEADER_TYPE_REVISIT = "revisit";
  
  public static final String HEADER_REFERS_TO = "WARC-Refers-To";
  
  public static final String HEADER_DATE = "WARC-Date";
  
  public static final DateTimeFormatter HEADER_DATE_FORMAT =
      DateTimeFormatter.ISO_INSTANT;
  
  public static final String HEADER_TARGET_URI = "WARC-Target-URI";
  
  public static final String HEADER_CONCURRENT = "WARC-Concurrent-To";
  
  // Utility class
  private Warcs() { }

}
