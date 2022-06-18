package de.webis.wasp.index;

import java.time.Instant;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import co.elastic.clients.elasticsearch.core.search.Hit;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

/**
 * A result for a query.
 * 
 * @author johannes.kiesel@uni-weimar.de
 *
 */
@JsonAutoDetect(
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE)
public class Result {
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTANTS
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Name of the result's retrieval score field.
   */
  public static final String FIELD_SCORE = "score";
  
  /**
   * Name of the result's snippet field.
   */
  public static final String FIELD_SNIPPET = "snippet";
  
  /**
   * Name of the result's response field.
   */
  public static final String FIELD_RESPONSE = "response";
  
  /**
   * Name of the results's matched request field.
   */
  public static final String FIELD_MATCHED_REQUEST = "matchedRequest";
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  private final double score;
  
  private final String snippet;

  private final ResponseRecord response;

  private final RequestRecord matchedRequest;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTION
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Creates a new result.
   * @param score The retrieval score
   * @param snippet The snippet to display
   * @param response The underlying response
   * @param matchedRequest The response's request that matched the query's time
   * constraints
   */
  @JsonCreator
  public Result(
      @JsonProperty(FIELD_SCORE) final double score,
      @JsonProperty(FIELD_SNIPPET) final String snippet,
      @JsonProperty(FIELD_RESPONSE) final ResponseRecord response,
      @JsonProperty(FIELD_MATCHED_REQUEST) final RequestRecord matchedRequest) {
    this.score = score;
    this.snippet = Objects.requireNonNull(snippet);
    this.response = Objects.requireNonNull(response);
    this.matchedRequest = Objects.requireNonNull(matchedRequest);
  }

  /**
   * Creates a new result from a search hit.
   * @param hit The hit
   * @param from The earliest time for a request, or <code>null</code> for no
   * constraint in this direction
   * @param to The latest time for a request, or <code>null</code> for no
   * constraint in this direction
   * @return The result
   * @throws IllegalArgumentException If no request matches the constraints
   */
  public static Result fromHit(
      final Hit<ResponseRecord> hit, final Instant from, final Instant to) {
    final double score = hit.score();
    
    final ResponseRecord response = hit.source();
    final RequestRecord request = Result.matchRequest(response, from, to);
    final String snippet = Result.getSnippet(hit);
    
    return new Result(score, snippet, response, request);
  }

  /**
   * Get the response's request the matches the time constraints.
   * @param response The response
   * @param from The earliest time for a request, or <code>null</code> for no
   * constraint in this direction
   * @param to The latest time for a request, or <code>null</code> for no
   * constraint in this direction
   * @return The latest request matching the constraints
   * @throws IllegalArgumentException If no request matches the constraints
   */
  protected static RequestRecord matchRequest(
      final ResponseRecord response, final Instant from, final Instant to) {
    final List<RequestRecord> requests = response.getRequests();
    final ListIterator<RequestRecord> iterator =
        requests.listIterator(requests.size());
    while (iterator.hasPrevious()) {
      final RequestRecord request = iterator.previous();
      final Instant date = request.getDate();
      if (from != null && date.isBefore(from)) { continue; }
      if (to != null && date.isAfter(to)) { continue; }
      return request;
    }
    throw new IllegalArgumentException(
        "it contained no request in time interval");
  }

  /**
   * Gets the snippet of a search hit.
   * @param hit The hit
   * @return The snippet (may be empty)
   */
  protected static String getSnippet(final Hit<ResponseRecord> hit) {
    final Map<String, List<String>> snippetsPerField = hit.highlight();
    final List<String> snippetParts =
        snippetsPerField.get(ResponseRecord.FIELD_CONTENT);
    if (snippetParts == null) { return ""; }
    return String.join(" ... ", snippetParts);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // GETTER
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Gets the retrieval score of the result.
   * @return The score
   */
  @JsonGetter(FIELD_SCORE)
  public double getScore() {
    return this.score;
  }

  /**
   * Gets the snippet of the result.
   * @return The snippet
   */
  @JsonGetter(FIELD_SNIPPET)
  public String getSnippet() {
    return this.snippet;
  }

  /**
   * Gets the response of the result.
   * @return The response
   */
  @JsonGetter(FIELD_RESPONSE)
  public ResponseRecord getResponse() {
    return this.response;
  }

  /**
   * Gets the request of the response that was matched by the query.
   * @return The request
   */
  @JsonGetter(FIELD_MATCHED_REQUEST)
  public RequestRecord getMatchedRequest() {
    return this.matchedRequest;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Checks whether the result snippet is empty.
   * @return Whether it is
   */
  public boolean hasEmptySnippet() {
    return this.getSnippet().isEmpty();
  } 
  
  @Override
  public String toString() {
    return String.format(
        "RESULT %.2f '%s' FROM '%s' AT %s: '%s'",
        this.getScore(), this.getResponse().getTitle(),
        this.getMatchedRequest().getUri(),
        this.getMatchedRequest().getDate(),
        this.getSnippet());
  }

}
