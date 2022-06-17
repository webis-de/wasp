package de.webis.warc.index;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;

public class ResultsFetcher {

  /////////////////////////////////////////////////////////////////////////////
  // STATIC FIELDS
  /////////////////////////////////////////////////////////////////////////////
  
  protected static final Highlight HIGHLIGHT =
      Highlight.of(highlight -> highlight
          .fields(Index.FIELD_CONTENT_NAME, HighlightField.of(field -> field
              .type("unified"))));

  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final Index index;

  protected final Query query;

  protected final int pageSize;

  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  public ResultsFetcher(
      final Index index, final Query query, final int pageSize) {
    if (index == null) { throw new NullPointerException("index"); }
    if (query == null) { throw new NullPointerException("query"); }
    if (pageSize < 1) {
      throw new IllegalArgumentException("pageSize: " + pageSize);
    }

    this.index = index;
    this.query = query;
    this.pageSize = pageSize;
  }

  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////

  public int getPageSize() {
    return this.pageSize;
  }

  public synchronized List<Result> fetch(final int page)
  throws IOException {
    final SearchRequest searchRequest = SearchRequest.of(builder -> builder
        .query(q -> q.bool(this.query.getBuilder()))
        .highlight(HIGHLIGHT)
        .size(this.pageSize)
        .from((page - 1) * this.getPageSize()));

    final SearchResponse<ObjectNode> searchResponse =
        this.index.search(searchRequest);
    final HitsMetadata<ObjectNode> hits = searchResponse.hits();
    
    final List<Result> results = new ArrayList<>();
    for (final Hit<ObjectNode> hit : hits.hits()) {
      final Result result = this.toResult(
          hit, this.query.getFrom(), this.query.getTo());
      if (!result.isEmpty()) { results.add(result); }
    }
    return results;
  }
  
  protected Result toResult(
      final Hit<ObjectNode> hit, final Instant from, final Instant to) {
    final double score = hit.score();
    
    final ObjectNode source = hit.source();
    final MinimalRequest request = this.pickRequest(source, from, to);
    
    final String uri = request.getUri();
    final Instant instant = request.getDate();
    final String title = source.get(Index.FIELD_TITLE_NAME).toString();
    final String content = source.get(Index.FIELD_CONTENT_NAME).toString();
    final String snippet = this.getSnippet(hit);
    
    return new Result(score, uri, instant, title, content, snippet);
  }
  
  protected MinimalRequest pickRequest(
      final ObjectNode source, final Instant from, final Instant to) {
    final ArrayNode requestSources =
        (ArrayNode) source.get(Index.FIELD_REQUEST_NAME);
    if (requestSources == null || requestSources.size() == 0) {
      throw new IllegalArgumentException("Hit contained no request");
    }
    for (int i = requestSources.size() - 1; i >= 0; --i) {
      final JsonNode requestSource = requestSources.get(i);
      final MinimalRequest request = new MinimalRequest(requestSource.get(i));
      final Instant date = request.getDate();
      if (from != null && date.isBefore(from)) { continue; }
      if (to != null && date.isAfter(to)) { continue; }
      return request;
    }
    throw new IllegalArgumentException(
        "it contained no request in time interval");
  }
  
  protected String getSnippet(final Hit<ObjectNode> hit) {
    final Map<String, List<String>> highlights = hit.highlight();
    final List<String> highlight = highlights.get(Index.FIELD_CONTENT_NAME);
    if (highlight == null) { return ""; }
    return String.join(" ... ", highlight);
  }

  /////////////////////////////////////////////////////////////////////////////
  // HELPER CLASSES
  /////////////////////////////////////////////////////////////////////////////
  
  protected static class MinimalRequest {

    ///////////////////////////////////////////////////////////////////////////
    // MEMBERS
    ///////////////////////////////////////////////////////////////////////////
    
    protected final String uri;
    
    protected final Instant date;

    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////////
    
    public MinimalRequest(final String uri, final Instant date) {
      if (uri == null) { throw new NullPointerException("uri"); }
      if (date == null) { throw new NullPointerException("date"); }
      this.uri = uri;
      this.date = date;
    }
    
    public MinimalRequest(final JsonNode requestSource) {
      this.uri = requestSource.get(Index.FIELD_URI_NAME).asText();
      if (this.uri == null) { throw new NullPointerException("uri"); }
      this.date = Instant.parse(
          requestSource.get(Index.FIELD_DATE_NAME).asText());
      if (this.date == null) { throw new NullPointerException("date"); }
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////
    
    public String getUri() {
      return this.uri;
    }
    
    public Instant getDate() {
      return this.date;
    }
    
  }

}
