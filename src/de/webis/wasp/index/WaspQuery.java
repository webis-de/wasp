package de.webis.wasp.index;

import java.time.Instant;
import java.util.Objects;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;

public class WaspQuery {
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTANTS
  /////////////////////////////////////////////////////////////////////////////

  public static final float TITLE_BOOST = 2.0f;
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  private final String terms;
  
  private Instant from;
  
  private Instant to;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////

  public WaspQuery(
      final String terms, final Instant from, final Instant to) {
    this.terms = Objects.requireNonNull(terms);
    this.from = from;
    this.to = to;
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
    if (obj instanceof WaspQuery) {
      final WaspQuery other = (WaspQuery) obj;

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
  
  protected BoolQuery getBuilder() {
    return BoolQuery.of(builder -> builder
        .must(must -> must.nested(this.getTimeQueryBuilder()))
        .should(should -> should.bool(this.getTermQueryBuilder())));
  }
  
  protected BoolQuery getTermQueryBuilder() {
    return BoolQuery.of(builder -> builder
        .should(should -> should
            .match(match -> match
                .field(ResponseRecord.FIELD_CONTENT)
                .query(this.getTerms())
                .operator(Operator.And)))
        .should(should -> should
            .match(match -> match
                .field(ResponseRecord.FIELD_TITLE)
                .query(this.getTerms())
                .operator(Operator.And)
                .boost(TITLE_BOOST))));
  }
  
  protected NestedQuery getTimeQueryBuilder() {
    final Instant from = this.getFrom();
    final Instant to = this.getTo();

    final RangeQuery rangeQuery =
        RangeQuery.of(builder -> {
          builder.field(
              ResponseRecord.FIELD_REQUESTS + "." + RequestRecord.FIELD_DATE);
          if (from != null) { builder.from(from.toString()); }
          if (to != null) { builder.to(to.toString()); }
          return builder;
        });
    
    return NestedQuery.of(builder -> builder
        .path(ResponseRecord.FIELD_REQUESTS)
        .query(innerBuilder -> innerBuilder.range(rangeQuery))
        .scoreMode(ChildScoreMode.Max));
  }

}
