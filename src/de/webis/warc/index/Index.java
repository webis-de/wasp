package de.webis.warc.index;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class Index implements AutoCloseable {

  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(Index.class.getName());
  
  protected static final String TYPE_NAME = "document";
  
  protected final RestHighLevelClient client;
  
  protected final String name;
  
  public Index(final int port, final String name) {
    if (name == null) { throw new NullPointerException(); }
    this.client = new RestHighLevelClient(RestClient.builder(
        new HttpHost("localhost", port, "http")));
    this.name = name;
  }
  
  public Index(final int port, final String name,
      final Map<String, String> types) throws IOException {
    this(port, name);

    // convert map to array
    final Object[] typesArray = new Object[types.size() * 2];
    int t = 0;
    for (final Entry<String, String> type : types.entrySet()) {
      typesArray[2 * t] = type.getKey();
      typesArray[2 * t + 1] = type.getValue(); 
      ++t;
    }

    // issue request
    final CreateIndexRequest request =
        new CreateIndexRequest(this.name);
    request.mapping(TYPE_NAME, typesArray);
    this.client.indices().create(request);
  }
  
  public void index(final Map<String, ? extends Object> document) {
    final IndexRequest request =
        new IndexRequest(this.name, TYPE_NAME);
    request.source(document);
    this.client.indexAsync(request, LoggingListener.INDEX_RESPONSE);
  }
  
  @Override
  public void close() throws IOException {
    this.client.close();
  }
  
  protected static class LoggingListener<RESPONSE extends Object>
  implements ActionListener<RESPONSE> {
    
    protected static ActionListener<IndexResponse>
    INDEX_RESPONSE = new LoggingListener<IndexResponse>();

    @Override
    public void onFailure(final Exception exception) {
      LOG.log(Level.SEVERE, "Failed to index", exception);
    }

    @Override
    public void onResponse(final RESPONSE response) { }
    
  }
  
  public static void main(final String[] args) throws Exception {
    final Map<String, String> types = new HashMap<>();
    types.put("uri", "type=keyword");
    types.put("date", "type=date");
    types.put("content", "type=text");
    try (final Index index = new Index(9200, "archive",
        types)) {
      final Map<String, Object> document = new HashMap<>();
      document.put("uri", "blub");
      index.index(document);
      Thread.sleep(1000);
    }
  }

}
