package de.webis.warc.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CachingResultsFetcher extends ResultsFetcher {
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final List<List<Result>> results;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////

  public CachingResultsFetcher(
      final Index index, final Query query, final int pageSize) {
    super(index, query, pageSize);
    this.results = new ArrayList<>();
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  @Override
  public List<Result> fetch(final int pageNumber)
  throws IOException {
    final int pageIndex = pageNumber - 1;
    while (this.results.size() < pageIndex) {
      this.results.add(null);
    }
    
    if (this.results.size() <= pageIndex
        || this.results.get(pageIndex) == null) {
      this.results.add(pageIndex,
          Collections.unmodifiableList(super.fetch(pageNumber)));
    }
    return this.results.get(pageIndex);
  }

}
