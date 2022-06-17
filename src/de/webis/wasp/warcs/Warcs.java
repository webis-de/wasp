package de.webis.wasp.warcs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.entity.DecompressingEntity;
import org.apache.http.client.entity.DeflateInputStream;
import org.apache.http.client.entity.InputStreamFactory;
import org.apache.http.config.Lookup;
import org.apache.http.config.MessageConstraints;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.entity.LaxContentLengthStrategy;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.impl.io.ContentLengthInputStream;
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.http.impl.io.EmptyInputStream;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.IdentityInputStream;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import edu.cmu.lemurproject.WarcRecord;

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
  
  public static final String HEADER_TARGET_URI = "WARC-Target-URI";
  
  public static final String HEADER_CONCURRENT = "WARC-Concurrent-To";
  
  public static final String HEADER_DATE = "WARC-Date";
  
  public static final DateTimeFormatter HEADER_DATE_FORMAT =
      DateTimeFormatter.ISO_INSTANT;
  

  public static final Pattern HTTP_HEADER_CONTENT_TYPE_HTML = Pattern.compile(
      "text/html.*");

  public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
  
  /////////////////////////////////////////////////////////////////////////////
  // STATIC HELPERS
  /////////////////////////////////////////////////////////////////////////////

  private final static InputStreamFactory GZIP = new InputStreamFactory() {
    @Override
    public InputStream create(final InputStream instream) throws IOException {
      return new GZIPInputStream(instream);
    }
  };

  private final static InputStreamFactory DEFLATE = new InputStreamFactory() {
    @Override
    public InputStream create(final InputStream instream) throws IOException {
      return new DeflateInputStream(instream);
    }
  };
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  // Utility class
  private Warcs() { }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////

  /////////////////////////////////////////////////////////////////////////////
  // Access header fields
  
  public static String getHeader(
      final WarcRecord record, final String header) {
    return record.getHeaderMetadataItem(header);
  }

  public static String getId(final WarcRecord record) {
    return Warcs.getHeader(record, HEADER_ID);
  }
  
  public static String getType(final WarcRecord record) {
    return Warcs.getHeader(record, HEADER_TYPE);
  }
  
  public static Instant getDate(final WarcRecord record) {
    final String date = Warcs.getHeader(record, HEADER_DATE);
    return Instant.from(HEADER_DATE_FORMAT.parse(date));
  }
  
  public static String getTargetUri(final WarcRecord record) {
    return Warcs.getHeader(record, HEADER_TARGET_URI);
  }
  
  public static String getConcurrentRecordId(final WarcRecord record) {
    return Warcs.getHeader(record, HEADER_CONCURRENT);
  }
  
  public static String getReferedToRecordId(final WarcRecord record) {
    return Warcs.getHeader(record, HEADER_REFERS_TO);
  }

  /////////////////////////////////////////////////////////////////////////////
  // HTML
  
  /**
   * Checks if this is a HTML response record.
   */
  public static boolean isHtml(final WarcRecord record)
  throws HttpException, IOException {
    if (record == null) { return false; }
    final HttpResponse response = Warcs.toResponse(record);
    return Warcs.isHtml(response);
  }
  
  /**
   * Checks if this is a HTML response.
   */
  public static boolean isHtml(final HttpResponse response) {
    if (response == null) { return false; }

    final String contentType =
        response.getLastHeader(HTTP_HEADER_CONTENT_TYPE).getValue();
    if (contentType == null) { return false; } // no content type

    if (!HTTP_HEADER_CONTENT_TYPE_HTML.matcher(contentType).matches()) {
      return false; // not HTML content type
    }
    
    return true;
  }
  
  /**
   * Gets the HTML part of a record or <tt>null</tt> if there is none or an
   * invalid one.
   */
  public static String getHtml(final WarcRecord record)
  throws ParseException, IOException, HttpException {
    final HttpResponse response = Warcs.toResponse(record);
    if (!Warcs.isHtml(response)) { return null; } // no HTML record

    final HttpEntity entity = response.getEntity();
    final String defaultCharset = null;
    return EntityUtils.toString(entity, defaultCharset);
  }

  /**
   * Gets an {@link HttpResponse} object from a WARC record of such a response.
   * @return The response or <tt>null</tt> when the record is not a response
   * record
   */
  public static HttpResponse toResponse(final WarcRecord record)
  throws IOException, HttpException {
    // based on http://stackoverflow.com/a/26586178
    if (!record.getHeaderRecordType().equals("response")) { return null; }
    
    final SessionInputBufferImpl sessionInputBuffer =
        new SessionInputBufferImpl(new HttpTransportMetricsImpl(), 2048);
    final InputStream inputStream =
        new ByteArrayInputStream(record.getByteContent());
    sessionInputBuffer.bind(inputStream);
    final MessageConstraints constraints = MessageConstraints.DEFAULT;
    final DefaultHttpResponseParser parser =
        new DefaultHttpResponseParser(
            sessionInputBuffer, null, new DefaultHttpResponseFactory(),
            constraints);
    final HttpResponse response = parser.parse();
    final HttpEntity entity = Warcs.getEntity(response, sessionInputBuffer);
    response.setEntity(entity);
    Warcs.encodeEntity(response);
    return response;
  }

  
  private static void encodeEntity(final HttpResponse response)
  throws HttpException, IOException {
    // Adapted from org.apache.http.client.protocol.ResponseContentEncoding
    final HttpEntity entity = response.getEntity();
  
    // entity can be null in case of 304 Not Modified, 204 No Content or similar
    // check for zero length entity.
    if (entity != null && entity.getContentLength() != 0) {
      final Header ceheader = entity.getContentEncoding();
      if (ceheader != null) {
        final HeaderElement[] codecs = ceheader.getElements();
        final Lookup<InputStreamFactory> decoderRegistry =
            RegistryBuilder.<InputStreamFactory>create()
              .register("gzip", GZIP)
              .register("x-gzip", GZIP)
              .register("deflate", DEFLATE)
              .build();
        for (final HeaderElement codec : codecs) {
          final String codecname = codec.getName().toLowerCase(Locale.ROOT);
          final InputStreamFactory decoderFactory =
              decoderRegistry.lookup(codecname);
          if (decoderFactory != null) {
            response.setEntity(new DecompressingEntity(
                response.getEntity(), decoderFactory));
            response.removeHeaders("Content-Length");
            response.removeHeaders("Content-Encoding");
            response.removeHeaders("Content-MD5");
          } else {
            if (!"identity".equals(codecname)) {
                throw new HttpException(
                    "Unsupported Content-Encoding: " + codec.getName());
            }
          }
        }
      }
    }
  }
  
  private static InputStream createInputStream(
      final long len, final SessionInputBuffer input) {
    // Adapted from the org.apache.http.impl.BHttpConnectionBase
    if (len == ContentLengthStrategy.CHUNKED) {
      return new ChunkedInputStream(input);
    } else if (len == ContentLengthStrategy.IDENTITY) {
      return new IdentityInputStream(input);
    } else if (len == 0L) {
      return EmptyInputStream.INSTANCE;
    } else {
      return new ContentLengthInputStream(input, len);
    }
  }
  
  private static HttpEntity getEntity(
      final HttpResponse response, final SessionInputBuffer input)
  throws HttpException {
    // Adapted from the org.apache.http.impl.BHttpConnectionBase
    final BasicHttpEntity entity = new BasicHttpEntity();

    final long len =
        new LaxContentLengthStrategy().determineLength(response);
    final InputStream instream = Warcs.createInputStream(len, input);
    if (len == ContentLengthStrategy.CHUNKED) {
      entity.setChunked(true);
      entity.setContentLength(-1);
      entity.setContent(instream);
    } else if (len == ContentLengthStrategy.IDENTITY) {
      entity.setChunked(false);
      entity.setContentLength(-1);
      entity.setContent(instream);
    } else {
      entity.setChunked(false);
      entity.setContentLength(len);
      entity.setContent(instream);
    }

    final Header contentTypeHeader = 
        response.getFirstHeader(HTTP.CONTENT_TYPE);
    if (contentTypeHeader != null) {
      entity.setContentType(contentTypeHeader);
    }
    final Header contentEncodingHeader =
        response.getFirstHeader(HTTP.CONTENT_ENCODING);
    if (contentEncodingHeader != null) {
      entity.setContentEncoding(contentEncodingHeader);
    }
    return entity;
  }

}
