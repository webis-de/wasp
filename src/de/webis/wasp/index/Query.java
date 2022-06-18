package de.webis.wasp.index;

import java.time.Instant;
import java.util.Objects;

import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;

/**
 * A query to the index with optional time constraints
 * 
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class Query {
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTANTS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Boosting factor for the title over the content.
   */
  protected static final float TITLE_BOOST = 2.0f;

  /**
   * Field name of the request's date within the response.
   */
  protected static final String FIELD_DATE_COMPLETE =
      ResponseRecord.FIELD_REQUESTS + "." + RequestRecord.FIELD_DATE;

  /**
   * Snippet generator.
   */
  protected static final Highlight HIGHLIGHT =
      Highlight.of(highlight -> highlight
          .fields(ResponseRecord.FIELD_CONTENT, HighlightField.of(field -> field
              .type("unified"))));
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  private final String terms;
  
  private Instant from;
  
  private Instant to;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a new query.
   * @param terms The query terms to match the response content and title with
   * @param from The earliest time for a request to match this query, or
   * <code>null</code> for no constraint in this direction
   * @param to The latest time for a request to match this query, or
   * <code>null</code> for no constraint in this direction
   */
  public Query(
      final String terms, final Instant from, final Instant to) {
    this.terms = Objects.requireNonNull(terms);
    this.from = from;
    this.to = to;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // GETTERS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Gets the query terms to match the response content and title with.
   * @return The terms
   */
  public String getTerms() {
    return this.terms;
  }

  /**
   * Gets the earliest time for a request to match this query, if any.
   * @return The time or <code>null</code> for no constraint in this direction
   */
  public Instant getFrom() {
    return this.from;
  }

  /**
   * Gets the latest time for a request to match this query, if any.
   * @return The time or <code>null</code> for no constraint in this direction
   */
  public Instant getTo() {
    return this.to;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  @Override
  public boolean equals(final Object obj) {
    if (obj == null) { return false; }
    if (obj instanceof Query) {
      final Query other = (Query) obj;

      if (!this.getTerms().equals(other.getTerms())) { return false; }

      final Instant thisFrom = this.getFrom();
      final Instant otherFrom = other.getFrom();
      if ((thisFrom == null && otherFrom != null)
          || (thisFrom != null && !thisFrom.equals(otherFrom))) {
        return false;
      }

      final Instant thisTo = this.getTo();
      final Instant otherTo = other.getTo();
      if ((thisTo == null && otherTo != null)
          || (thisTo != null && !thisTo.equals(otherTo))) {
        return false;
      }
      return true;
    }
    return false;
  }

  /**
   * Creates a search request from this query.
   * @return A search request builder that is configured accordingly
   */
  public SearchRequest.Builder build() {
    final Instant from = this.getFrom();
    final Instant to = this.getTo();
    final String terms = this.getTerms();

    return new SearchRequest.Builder()
        .query(query -> query
            .bool(main -> main
                .must(time -> time.nested(nested -> nested
                    .path(ResponseRecord.FIELD_REQUESTS)
                    .scoreMode(ChildScoreMode.Max)
                    .query(inner -> inner.range(range -> {
                      range.field(FIELD_DATE_COMPLETE);
                      if (from != null) { range.from(from.toString()); }
                      if (to != null) { range.to(to.toString()); }
                      return range;
                    }))))
                .should(term -> term.bool(bool -> bool
                    .should(should -> should
                        .match(match -> match
                            .field(ResponseRecord.FIELD_CONTENT)
                            .query(terms)
                            .operator(Operator.And)))
                    .should(should -> should
                        .match(match -> match
                            .field(ResponseRecord.FIELD_TITLE)
                            .query(terms)
                            .operator(Operator.And)
                            .boost(TITLE_BOOST))))
                    )))
        .highlight(HIGHLIGHT);
  }

  /**
   * Creates a search request from this query.
   * @param pageSize The result page size
   * @return A search request builder that is configured accordingly
   */
  public SearchRequest.Builder build(final int pageSize) {
    return this.build().size(pageSize);
  }

  /**
   * Creates a search request from this query.
   * @param pageSize The result page size
   * @param page The result page
   * @return A search request builder that is configured accordingly
   */
  public SearchRequest.Builder build(final int pageSize, final int page) {
    return this.build(pageSize).from((page - 1) * pageSize);
  }

}
