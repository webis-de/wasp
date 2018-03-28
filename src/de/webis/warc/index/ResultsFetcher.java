package de.webis.warc.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.joda.time.Instant;

public class ResultsFetcher {

  /////////////////////////////////////////////////////////////////////////////
  // STATIC FIELDS
  /////////////////////////////////////////////////////////////////////////////
  
  protected static final HighlightBuilder HIGHLIGHT_BUILDER =
      ResultsFetcher.getHighlightBuilder();

  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final Index index;

  protected final Query query;
  
  protected final SearchSourceBuilder searchBuilder;

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
    this.searchBuilder = new SearchSourceBuilder();
    this.searchBuilder.query(query.getBuilder());
    this.searchBuilder.highlighter(HIGHLIGHT_BUILDER);
    this.searchBuilder.size(pageSize);
  }

  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////

  public int getPageSize() {
    return this.searchBuilder.size();
  }

  public synchronized List<Result> fetch(final int page)
  throws IOException {
    this.searchBuilder.from((page - 1) * this.getPageSize());

    final SearchRequest searchRequest = new SearchRequest();
    searchRequest.source(this.searchBuilder);
    final SearchResponse searchResponse = this.index.search(searchRequest);
    final SearchHits hits = searchResponse.getHits();
    
    final List<Result> results = new ArrayList<>();
    for (final SearchHit hit : hits) {
      results.add(this.toResult(
          hit, this.query.getFrom(), this.query.getTo()));
    }
    return results;
  }
  
  protected Result toResult(
      final SearchHit hit, final Instant from, final Instant to) {
    final float score = hit.getScore();
    
    final Map<String, Object> source = hit.getSourceAsMap();
    final MinimalRequest request = this.pickRequest(source, from, to);
    
    final String uri = request.getUri();
    final Instant instant = request.getDate();
    final String title = source.get(Index.FIELD_TITLE_NAME).toString();
    final String snippet = this.getSnippet(hit);
    
    return new Result(score, uri, instant, title, snippet);
  }
  
  protected MinimalRequest pickRequest(
      final Map<String, Object> source, final Instant from, final Instant to) {
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> requestSources =
      (List<Map<String, Object>>) source.get(Index.FIELD_REQUEST_NAME);
    final ListIterator<Map<String, Object>> requestSourceIterator =
        requestSources.listIterator(requestSources.size());
    while (requestSourceIterator.hasPrevious()) {
      final MinimalRequest request =
          new MinimalRequest(requestSourceIterator.previous());
      final Instant date = request.getDate();
      if (from != null && date.isBefore(from)) { continue; }
      if (to != null && date.isAfter(to)) { continue; }
      return request;
    }
    throw new IllegalArgumentException(
        "SearchHit contained no request in time interval");
  }
  
  protected String getSnippet(final SearchHit hit) {
    final Map<String, HighlightField> highlights = hit.getHighlightFields();
    final HighlightField highlight = highlights.get(Index.FIELD_CONTENT_NAME);
    if (highlight == null) { return ""; }

    final Text[] textFragments = highlight.fragments();
    final String[] fragments = new String[textFragments.length];
    for (int f = 0; f < fragments.length; ++f) {
      fragments[f] = textFragments[f].string();
    }
    return String.join(" ... ", fragments);
  }

  protected static HighlightBuilder getHighlightBuilder() {
    final HighlightBuilder highlightBuilder = new HighlightBuilder();
    
    final HighlightBuilder.Field content =
        new HighlightBuilder.Field(Index.FIELD_CONTENT_NAME);
    content.highlighterType("unified");
    highlightBuilder.field(content);

    return highlightBuilder;
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
    
    public MinimalRequest(final Map<String, Object> requestSource) {
      this.uri = (String) requestSource.get(Index.FIELD_URI_NAME);
      if (this.uri == null) { throw new NullPointerException("uri"); }
      this.date = Instant.parse(
          (String) requestSource.get(Index.FIELD_DATE_NAME));
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
