package de.webis.warc.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResultPages {
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final ResultsFetcher fetcher;
  
  protected final List<List<Result>> results;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  public ResultPages(final ResultsFetcher fetcher) {
    if (fetcher == null) { throw new NullPointerException("resultsfetcher"); }
    this.fetcher = fetcher;
    this.results = new ArrayList<>();
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  public List<Result> getPage(final int page)
  throws IOException {
    final int pageIndex = page - 1;
    while (this.results.size() < pageIndex) {
      this.results.add(null);
    }
    
    if (this.results.size() <= pageIndex
        || this.results.get(pageIndex) == null) {
      this.results.add(pageIndex,
          Collections.unmodifiableList(this.fetcher.fetch(page)));
    }
    return this.results.get(pageIndex);
  }

}
