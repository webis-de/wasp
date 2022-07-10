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
   * Name of the record's title field.
   */
  public static final String FIELD_TITLE = "title";
  
  /**
   * Name of the record's content field.
   */
  public static final String FIELD_CONTENT = "content";
  
  /**
   * Name of the record's revisited field.
   */
  public static final String FIELD_REVISITED = "revisited";
  
  /**
   * Name of the record's requests field.
   */
  public static final String FIELD_REQUESTS = "requests";

  /**
   * Properties for an Elasticsearch mapping of this class.
   */
  public static Map<String, Property> TYPE_PROPERTIES = Map.of(
      FIELD_TITLE, TextProperty.of(property -> property)._toProperty(),
      FIELD_CONTENT, TextProperty.of(property -> property)._toProperty(),
      FIELD_REVISITED, KeywordProperty.of(property -> property)._toProperty(),
      FIELD_REQUESTS, NestedProperty.of(property -> property
            .properties(RequestRecord.TYPE_PROPERTIES)
          )._toProperty());

  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////

  private final String title;

  private final String content;

  private final String revisited;

  private final List<RequestRecord> requests;

  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTION
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a new record for some request.
   * @param title The title of the response page (or <code>null</code> if a
   * revisit)
   * @param content The extracted content of the response page (or
   * <code>null</code> if a revisit)
   * @param revisited The ID of the revisited response (or <code>null</code> if
   * not a revisit)
   * @param requests The requests that led to this response (empty if a revisit) 
   */
  @JsonCreator
  public ResponseRecord(
      @JsonProperty(FIELD_TITLE) final String title,
      @JsonProperty(FIELD_CONTENT) final String content,
      @JsonProperty(FIELD_REVISITED) final String revisited,
      @JsonProperty(FIELD_REQUESTS) final List<RequestRecord> requests) {
    if (title == null && content == null && revisited == null) {
      throw new NullPointerException();
    }
    this.title = title;
    this.content = content;
    this.revisited = revisited;
    if (requests == null) {
      this.requests = List.of();
    } else {
      this.requests = List.copyOf(requests);
    }
  }

  /**
   * Creates a new record for a response page without assigned requests.
   * @param title The title of the page
   * @param content The extracted content of the page
   * @return The request
   */
  public static ResponseRecord forPage(
      final String title, final String content) {
    return new ResponseRecord(
        Objects.requireNonNull(title), Objects.requireNonNull(content),
        null, null);
  }

  /**
   * Creates a new record for a revisit.
   * @param revisited The ID of the revisited response
   * @return The request
   */
  public static ResponseRecord forRevisit(final String revisited) {
    return new ResponseRecord(
        null, null, Objects.requireNonNull(revisited), null);
  }

  /////////////////////////////////////////////////////////////////////////////
  // GETTERS
  /////////////////////////////////////////////////////////////////////////////

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
   * Gets the title of the response.
   * @return The ID of the revisited response or <code>null</code> if not a
   * revisit
   */
  @JsonGetter(FIELD_REVISITED)
  public String getRevisited() {
    return this.revisited;
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
