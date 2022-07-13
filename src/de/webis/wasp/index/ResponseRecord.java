package de.webis.wasp.index;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.NestedProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;

/**
 * A record of a response or revisit for indexing / retrieval.
 * 
 * @author johannes.kiesel@uni-weimar.de
 *
 */
@JsonAutoDetect(
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE)
public class ResponseRecord {

  /////////////////////////////////////////////////////////////////////////////
  // CONSTANTS
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Name of the record's target URI field.
   */
  public static final String FIELD_URI = "uri";
  
  /**
   * Name of the record's title field.
   */
  public static final String FIELD_TITLE = "title";
  
  /**
   * Name of the record's content field.
   */
  public static final String FIELD_CONTENT = "content";
  
  /**
   * Name of the record's requests field.
   */
  public static final String FIELD_REQUESTS = "requests";

  /**
   * Properties for an Elasticsearch mapping of this class.
   */
  public static Map<String, Property> TYPE_PROPERTIES = Map.of(
      FIELD_URI, KeywordProperty.of(property -> property)._toProperty(),
      FIELD_TITLE, TextProperty.of(property -> property)._toProperty(),
      FIELD_CONTENT, TextProperty.of(property -> property)._toProperty(),
      FIELD_REQUESTS, NestedProperty.of(property -> property
            .properties(RequestRecord.TYPE_PROPERTIES)
          )._toProperty());

  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////

  private final String uri;

  private final String title;

  private final String content;

  private final List<RequestRecord> requests;

  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTION
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a new record for some request.
   * @param uri The target URI of the response page or revisit
   * @param title The title of the response page (or <code>null</code> if a
   * revisit)
   * @param content The extracted content of the response page (or
   * <code>null</code> if a revisit)
   * @param requests The requests that led to this response (empty if a revisit) 
   */
  @JsonCreator
  public ResponseRecord(
      @JsonProperty(FIELD_URI) final String uri,
      @JsonProperty(FIELD_TITLE) final String title,
      @JsonProperty(FIELD_CONTENT) final String content,
      @JsonProperty(FIELD_REQUESTS) final List<RequestRecord> requests) {
    this.uri = Objects.requireNonNull(uri);
    this.title = title;
    this.content = content;
    if (requests == null) {
      this.requests = List.of();
    } else {
      this.requests = List.copyOf(requests);
    }
  }

  /**
   * Creates a new record for a response page without assigned requests.
   * @param uri The target URI of the response page
   * @param title The title of the page
   * @param content The extracted content of the page
   * @return The request
   */
  public static ResponseRecord forPage(
      final String uri, final String title, final String content) {
    return new ResponseRecord(
        Objects.requireNonNull(uri),
        Objects.requireNonNull(title), Objects.requireNonNull(content), null);
  }

  /////////////////////////////////////////////////////////////////////////////
  // GETTERS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Gets the URI of the response.
   * @return The URI
   */
  @JsonGetter(FIELD_URI)
  public String getUri() {
    return this.uri;
  }

  /**
   * Gets the title of the response.
   * @return The title or <code>null</code> if a revisit
   */
  @JsonGetter(FIELD_TITLE)
  public String getTitle() {
    return this.title;
  }

  /**
   * Gets the content of the response.
   * @return The content or <code>null</code> if a revisit
   */
  @JsonGetter(FIELD_CONTENT)
  public String getContent() {
    return this.content;
  }

  /**
   * Gets the requests that led to this response.
   * @return The list of requests (empty if a revisit)
   */
  @JsonGetter(FIELD_REQUESTS)
  public List<RequestRecord> getRequests() {
    return this.requests;
  }

}
