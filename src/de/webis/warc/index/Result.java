package de.webis.warc.index;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE)
public class Result {
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final double score;
  
  protected final String uri;
  
  protected final Instant instant;
  
  protected final String title;
  
  protected final String content;
  
  protected final String snippet;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  @JsonCreator
  public Result(
      @JsonProperty("score") final double score,
      @JsonProperty("uri") final String uri,
      @JsonProperty("instant") final Instant instant,
      @JsonProperty("title") final String title,
      @JsonProperty("content") final String content,
      @JsonProperty("snippet") final String snippet) {
    if (uri == null) { throw new NullPointerException("URI"); }
    if (instant == null) { throw new NullPointerException("instant"); }
    if (title == null) { throw new NullPointerException("title"); }
    if (snippet == null) { throw new NullPointerException("snippet"); }

    this.score = score;
    this.uri = uri;
    this.instant = instant;
    this.title = title;
    this.content = content;
    this.snippet = snippet;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // GETTER
  /////////////////////////////////////////////////////////////////////////////

  @JsonGetter("score")
  public double getScore() {
    return this.score;
  }

  @JsonGetter("uri")
  public String getUri() {
    return this.uri;
  }

  @JsonGetter("instant")
  public Instant getInstant() {
    return this.instant;
  }
  
  @JsonGetter("title")
  public String getTitle() {
    return this.title;
  }

  @JsonGetter("content")
  public String getContent() {
    return this.content;
  }

  @JsonGetter("snippet")
  public String getSnippet() {
    return this.snippet;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  public boolean isEmpty() {
    return this.getSnippet().isEmpty();
  } 
  
  @Override
  public String toString() {
    return String.format(
        "RESULT %.2f '%s' FROM '%s' AT %s: '%s'",
        this.getScore(), this.getTitle(), this.getUri(), this.getInstant(),
        this.getSnippet());
  }

}
