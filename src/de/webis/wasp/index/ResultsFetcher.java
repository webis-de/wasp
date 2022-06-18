package de.webis.wasp.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

  protected final WaspQuery query;

  protected final int pageSize;

  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  public ResultsFetcher(
      final Index index, final WaspQuery query, final int pageSize) {
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
      final Result result =
          Result.fromHit(hit, this.query.getFrom(), this.query.getTo());
      if (!result.hasEmptySnippet()) { results.add(result); }
    }
    return results;
  }

}
