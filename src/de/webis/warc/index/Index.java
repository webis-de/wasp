package de.webis.warc.index;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.joda.time.Instant;

public class Index implements AutoCloseable {

  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(Index.class.getName());

  /////////////////////////////////////////////////////////////////////////////
  // STATIC VALUES
  /////////////////////////////////////////////////////////////////////////////
  
  public static final int DEFAULT_PORT = 9200;
  
  public static final String INDEX_NAME = "archive";
  
  public static final String TYPE_NAME = "response";
  
  public static final String FIELD_REVISITED_NAME = "revisited";
  
  public static final String FIELD_TITLE_NAME = "title";
  
  public static final String FIELD_CONTENT_NAME = "content";
  
  public static final String FIELD_REQUEST_NAME = "request";
  
  public static final String FIELD_URI_NAME = "uri";
  
  public static final String FIELD_DATE_NAME = "date";
  
  protected static final String TYPE_MAPPING = 
      "{\"" + TYPE_NAME + "\":{\n" + 
      "  \"properties\":{\n" +
      "    \"" + FIELD_TITLE_NAME + "\":{\"type\":\"text\"},\n" +
      "    \"" + FIELD_CONTENT_NAME + "\":{\"type\":\"text\"},\n" +
      "    \"" + FIELD_REVISITED_NAME + "\":{\"type\":\"keyword\"},\n" +
      "    \"" + FIELD_REQUEST_NAME + "\":{\n" +
      "      \"type\":\"nested\",\n" +
      "      \"properties\":{\n" +
      "        \""+ FIELD_URI_NAME + "\":{\"type\":\"keyword\"},\n" +
      "        \""+ FIELD_DATE_NAME + "\":{\"type\":\"date\"}\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}}";
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final RestHighLevelClient client;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  public Index(final int port) {
    this.client = new RestHighLevelClient(RestClient.builder(
        new HttpHost("localhost", port, "http")));
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  public void initialize() throws IOException {
    final CreateIndexRequest createIndexRequest =
        new CreateIndexRequest(INDEX_NAME);
    LOG.info("Created index with mapping:\n" + TYPE_MAPPING);
    createIndexRequest.mapping(TYPE_NAME, TYPE_MAPPING, XContentType.JSON);
    this.client.indices().create(createIndexRequest);
  }
  
  @Override
  public void close() throws IOException {
    this.client.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  // ID ACCESS
  
  public String resolveConcurrentId(final String id) throws IOException {
    final GetResponse getIdResponse = this.getResponse(id);
    if (getIdResponse == null) {
      return null;
    } else {
      return getIdResponse.getId();
    }
  }
  
  public GetResponse getResponse(final String id) throws IOException {
    final GetRequest getIdRequest = new GetRequest(INDEX_NAME, TYPE_NAME, id);
    final GetResponse getIdResponse = this.client.get(getIdRequest);
    if (getIdResponse.isExists()) {
      final Object revisited = getIdResponse.getSource().get(FIELD_REVISITED_NAME);
      if (revisited == null) {
        return getIdResponse;
      } else {
        return this.getResponse(revisited.toString());
      }
    } else {
      return null;
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // INDEXING
  
  public boolean indexResponse(
      final String id, final String content, final String title)
  throws IOException {
    if (id == null) { throw new NullPointerException("id"); }
    if (content == null) {
      throw new NullPointerException("content for " + id);
    }

    final XContentBuilder sourceBuilder = XContentFactory.jsonBuilder();
    sourceBuilder.startObject();
    sourceBuilder.field(FIELD_CONTENT_NAME, content);
    sourceBuilder.field(FIELD_TITLE_NAME, title);
    sourceBuilder.field(FIELD_REQUEST_NAME, Collections.EMPTY_LIST);
    sourceBuilder.endObject();

    final IndexRequest indexRequest =
        new IndexRequest(INDEX_NAME, TYPE_NAME, id);
    indexRequest.source(sourceBuilder);
    this.client.index(indexRequest);
    return true;
  }
  
  public boolean indexRevisit(final String id, final String responseId)
  throws IOException {
    if (id == null) { throw new NullPointerException("id"); }
    if (responseId == null) {
      throw new NullPointerException("responseId for " + id);
    }
  
    if (this.resolveConcurrentId(responseId) == null) {
      LOG.fine("No response found for ID = " + responseId);
      return false;
    }

    final XContentBuilder sourceBuilder = XContentFactory.jsonBuilder();
    sourceBuilder.startObject();
    sourceBuilder.field(FIELD_REVISITED_NAME, responseId);
    sourceBuilder.endObject();

    final IndexRequest indexRequest =
        new IndexRequest(INDEX_NAME, TYPE_NAME, id);
    indexRequest.source(sourceBuilder);
    this.client.index(indexRequest);
    return true;
  }
  
  public boolean indexRequest(final String concurrentId,
      final String uri, final Instant instant)
  throws IOException {
    final String responseId = this.resolveConcurrentId(concurrentId);
    if (responseId == null) {
      LOG.fine("No response found for ID = " + concurrentId);
      return false;
    }
    
    final Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(FIELD_URI_NAME, uri);
    requestMap.put(FIELD_DATE_NAME, instant);
    final Map<String, Object> parameters =
        Collections.singletonMap("request", requestMap);
    
    final Script script = new Script(ScriptType.INLINE, "painless",
        "ctx._source." + FIELD_REQUEST_NAME + ".add(params.request);",
        parameters);

    final UpdateRequest updateRequest =
        new UpdateRequest(INDEX_NAME, TYPE_NAME, responseId);
    updateRequest.script(script);
    this.client.update(updateRequest);
    return true;
  }

  /////////////////////////////////////////////////////////////////////////////
  // QUERY
  
  public ResultPages query(final Query query, final int pageSize) {
    return new ResultPages(new ResultsFetcher(this, query, pageSize));
  }
  
  protected SearchResponse search(final SearchRequest searchRequest)
  throws IOException {
    return this.client.search(searchRequest);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // MAIN
  /////////////////////////////////////////////////////////////////////////////
  
  public static void main(final String[] args) throws IOException {
    final int port =
        args.length == 0 ? DEFAULT_PORT : Integer.parseInt(args[0]);
    try (final Index index = new Index(port)) {
      index.initialize();
    }
  }

}
