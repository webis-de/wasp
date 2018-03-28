package de.webis.warc.index;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.joda.time.Instant;

public class Query {
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final String terms;
  
  protected Instant from;
  
  protected Instant to;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  protected Query(final String terms) {
    if (terms == null) { throw new NullPointerException("terms"); }
    this.terms = terms;
    this.from = null;
    this.to = null;
  }
  
  public static Query of(final String terms) {
    return new Query(terms);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // SETTERS
  /////////////////////////////////////////////////////////////////////////////
  
  public Query from(final Instant from) {
    this.from = from;
    return this;
  }
  
  public Query to(final Instant to) {
    this.to = to;
    return this;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // GETTERS
  /////////////////////////////////////////////////////////////////////////////
  
  public String getTerms() {
    return this.terms;
  }
  
  public Instant getFrom() {
    return this.from;
  }
  
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
      
      if (!this.terms.equals(other.terms)) { return false; }
      if ((this.from == null && other.from != null)
          || (this.from != null && !this.from.equals(other.from))) {
        return false;
      }
      if ((this.to == null && other.to != null)
          || (this.to != null && !this.to.equals(other.to))) {
        return false;
      }
      return true;
    }
    return false;
  }
  
  protected QueryBuilder getBuilder() {
    final QueryBuilder termQueryBuilder = this.getTermQueryBuilder();
    final QueryBuilder timeQueryBuilder = this.getTimeQueryBuilder();
    return QueryBuilders.boolQuery()
        .must(timeQueryBuilder).should(termQueryBuilder);
  }
  
  protected QueryBuilder getTermQueryBuilder() {
    BoolQueryBuilder termQueryBuilder = QueryBuilders.boolQuery();
    for (final String term : this.terms.split("\\s+")) {
      termQueryBuilder = termQueryBuilder.should(
          QueryBuilders.termQuery(Index.FIELD_CONTENT_NAME, term));
      termQueryBuilder = termQueryBuilder.should(
          QueryBuilders.termQuery(Index.FIELD_TITLE_NAME, term).boost(2.0f));
    }
    return termQueryBuilder;
  }
  
  protected QueryBuilder getTimeQueryBuilder() {
    RangeQueryBuilder rangeQueryBuilder =
        QueryBuilders.rangeQuery(
            Index.FIELD_REQUEST_NAME + "." + Index.FIELD_DATE_NAME);
    if (this.from != null) {
      rangeQueryBuilder = rangeQueryBuilder.from(this.from, true);
    }
    if (this.to != null) {
      rangeQueryBuilder = rangeQueryBuilder.to(this.to, true);
    }

    final QueryBuilder timeQueryBuilder = QueryBuilders.nestedQuery(
        Index.FIELD_REQUEST_NAME, rangeQueryBuilder, ScoreMode.Max);
    return timeQueryBuilder;
  }

}
