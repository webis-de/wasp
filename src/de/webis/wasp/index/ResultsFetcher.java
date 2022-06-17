package de.webis.wasp.index;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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
          .fields(ResponseRecord.FIELD_CONTENT, HighlightField.of(field -> field
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

    final SearchResponse<ResponseRecord> searchResponse =
        this.index.search(searchRequest);
    final HitsMetadata<ResponseRecord> hits = searchResponse.hits();
    
    final List<Result> results = new ArrayList<>();
    for (final Hit<ResponseRecord> hit : hits.hits()) {
      final Result result = this.toResult(
          hit, this.query.getFrom(), this.query.getTo());
      if (!result.isEmpty()) { results.add(result); }
    }
    return results;
  }
  
  protected Result toResult(
      final Hit<ResponseRecord> hit, final Instant from, final Instant to) {
    final double score = hit.score();
    
    final ResponseRecord response = hit.source();
    final RequestRecord request = this.pickRequest(response, from, to);
    
    final String uri = request.getUri();
    final Instant instant = request.getDate();
    final String title = response.getTitle();
    final String content = response.getContent();
    final String snippet = this.getSnippet(hit);
    
    return new Result(score, uri, instant, title, content, snippet);
  }
  
  protected RequestRecord pickRequest(
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
  
  protected String getSnippet(final Hit<ResponseRecord> hit) {
    final Map<String, List<String>> highlights = hit.highlight();
    final List<String> highlight = highlights.get(ResponseRecord.FIELD_CONTENT);
    if (highlight == null) { return ""; }
    return String.join(" ... ", highlight);
  }

}
