package de.webis.warc.index;

import java.time.Instant;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;

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
  
  protected BoolQuery getBuilder() {
    return BoolQuery.of(builder -> builder
        .must(must -> must.nested(this.getTimeQueryBuilder()))
        .should(should -> should.bool(this.getTermQueryBuilder())));
  }
  
  protected BoolQuery getTermQueryBuilder() {
    return BoolQuery.of(builder -> builder
        .should(should -> should
            .match(match -> match
                .field(Index.FIELD_CONTENT_NAME)
                .query(this.getTerms())
                .operator(Operator.And)))
        .should(should -> should
            .match(match -> match
                .field(Index.FIELD_TITLE_NAME)
                .query(this.getTerms())
                .operator(Operator.And)
                .boost(2.0f))));
  }
  
  protected NestedQuery getTimeQueryBuilder() {
    final RangeQuery rangeQuery =
        RangeQuery.of(builder -> {
          builder.field(Index.FIELD_REQUEST_NAME + "." + Index.FIELD_DATE_NAME);
          if (this.from != null) { builder.from(this.from.toString()); }
          if (this.to != null) { builder.to(this.to.toString()); }
          return builder;
        });
    
    return NestedQuery.of(builder -> builder
        .path(Index.FIELD_REQUEST_NAME)
        .query(innerBuilder -> innerBuilder.range(rangeQuery))
        .scoreMode(ChildScoreMode.Max));
  }

}
