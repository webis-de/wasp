package de.webis.warc.index;

import org.joda.time.Instant;

public class Result {
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final float score;
  
  protected final String uri;
  
  protected final Instant instant;
  
  protected final String title;
  
  protected final String content;
  
  protected final String snippet;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  public Result(final float score,
      final String uri, final Instant instant,
      final String title, final String content, final String snippet) {
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
  
  public float getScore() {
    return this.score;
  }
  
  public String getUri() {
    return this.uri;
  }
  
  public Instant getInstant() {
    return this.instant;
  }
  
  public String getTitle() {
    return this.title;
  }
  
  public String getContent() {
    return this.content;
  }
  
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
