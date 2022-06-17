package de.webis.wasp.index;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;

/**
 * A record of a request for indexing / retrieval.
 * 
 * @author johannes.kiesel@uni-weimar.de
 *
 */
@JsonAutoDetect(
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE)
public class RequestRecord {

  /////////////////////////////////////////////////////////////////////////////
  // CONSTANTS
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Name of the record's URI field.
   */
  public static final String FIELD_URI = "uri";
  
  /**
   * Name of the record's date field.
   */
  public static final String FIELD_DATE = "date";

  /**
   * Properties for an Elasticsearch mapping of this class.
   */
  public static Map<String, Property> TYPE_PROPERTIES = Map.of(
      FIELD_URI, KeywordProperty.of(property -> property)._toProperty(),
      FIELD_DATE, DateProperty.of(property -> property)._toProperty());

  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////

  private final String uri;

  private final Instant date;

  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTION
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a new record for some request.
   * @param uri The URI of the request
   * @param date The date of the request
   */
  @JsonCreator
  public RequestRecord(
      @JsonProperty(FIELD_URI) final String uri,
      @JsonProperty(FIELD_DATE) final Instant date) {
    this.uri = Objects.requireNonNull(uri);
    this.date = Objects.requireNonNull(date);
  }

  /////////////////////////////////////////////////////////////////////////////
  // GETTERS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Gets the URI of the request.
   * @return The URI
   */
  @JsonGetter(FIELD_URI)
  public String getUri() {
    return this.uri;
  }

  /**
   * Gets the date of the request.
   * @return The date
   */
  @JsonGetter(FIELD_DATE)
  public Instant getDate() {
    return this.date;
  }

}
