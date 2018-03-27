package de.webis.warc.index;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.apache.lucene.search.join.ScoreMode;
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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.Instant;

import de.webis.html.JerichoExtractor;
import edu.cmu.lemurproject.WarcRecord;

public class Index implements AutoCloseable {

  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(Index.class.getName());

  /////////////////////////////////////////////////////////////////////////////
  // STATIC VALUES
  /////////////////////////////////////////////////////////////////////////////
  
  public static final long NO_TIME_BOUND = -1;
  
  protected static final String INDEX_NAME = "archive";
  
  protected static final String TYPE_NAME = "response";
  
  protected static final String FIELD_REVISITED_NAME = "revisited";
  
  protected static final String FIELD_CONTENT_NAME = "content";
  
  protected static final String FIELD_REQUEST_NAME = "request";
  
  protected static final String FIELD_URI_NAME = "uri";
  
  protected static final String FIELD_DATE_NAME = "date";
  
  protected static final String TYPE_MAPPING = 
      "{\"" + TYPE_NAME + "\":{\n" + 
      "  \"properties\":{\n" +
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
  
  public boolean indexResponse(final String id, final String content)
  throws IOException {
    if (id == null) { throw new NullPointerException("id"); }
    if (content == null) {
      throw new NullPointerException("content for " + id);
    }

    final XContentBuilder sourceBuilder = XContentFactory.jsonBuilder();
    sourceBuilder.startObject();
    sourceBuilder.field(FIELD_CONTENT_NAME, content);
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
      final String uri, final long instant)
  throws IOException {
    final String responseId = this.resolveConcurrentId(concurrentId);
    if (responseId == null) {
      LOG.fine("No response found for ID = " + concurrentId);
      return false;
    }
    
    final Map<String, Object> requestMap = new HashMap<>();
    requestMap.put(FIELD_URI_NAME, uri);
    requestMap.put(FIELD_DATE_NAME, new Instant(instant));
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
  
  public Consumer<WarcRecord> warcIndexer() {
    return this.warcIndexer(JerichoExtractor.INSTANCE);
  }
  
  public Consumer<WarcRecord> warcIndexer(
      final Function<String, String> htmlContentExtractor) {
    return new WarcIndexer(this, htmlContentExtractor);
  }

  /////////////////////////////////////////////////////////////////////////////
  // QUERY
  
  public void query(final long from, final long to, final String query)
  throws IOException {
    BoolQueryBuilder contentQuery = QueryBuilders.boolQuery();
    for (final String term : query.split("\\s+")) {
      contentQuery = contentQuery.should(QueryBuilders.termQuery(FIELD_CONTENT_NAME, term));
    }
    
    final SearchSourceBuilder searchBuilder = new SearchSourceBuilder();
    if (from == NO_TIME_BOUND && to == NO_TIME_BOUND) {
      searchBuilder.query(contentQuery);
    } else {
      RangeQueryBuilder rangeQuery =
          QueryBuilders.rangeQuery(FIELD_REQUEST_NAME + "." + FIELD_DATE_NAME);
      if (from != NO_TIME_BOUND) {
        rangeQuery = rangeQuery.from(new Instant(from), true);
      }
      if (to != NO_TIME_BOUND) {
        rangeQuery = rangeQuery.to(new Instant(to), true);
      }
      final QueryBuilder timeQuery =
          QueryBuilders.nestedQuery(FIELD_REQUEST_NAME, rangeQuery, ScoreMode.Avg);
      searchBuilder.query(QueryBuilders.boolQuery().must(timeQuery).should(contentQuery));
    }

    final SearchRequest searchRequest = new SearchRequest();
    searchRequest.source(searchBuilder);
    final SearchResponse searchResponse = this.client.search(searchRequest);
    final SearchHits hits = searchResponse.getHits();
    for (final SearchHit hit : hits) {
      System.out.println("HIT");
      System.out.println(hit.getScore());
      System.out.println(hit.getInnerHits());
      System.out.println(hit.getSourceAsMap());
    }
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // MAIN
  /////////////////////////////////////////////////////////////////////////////
  
  public static void main(final String[] args) throws Exception {
    try (final Index index = new Index(9200)) {
/*
      index.initialize();
      System.out.println(".");
      index.indexResponse("myid", "content");
      System.out.println(".");
      index.indexRevisit("myid3", "myid");
      System.out.println(".");
      index.indexRevisit("myid4", "myid2");
      System.out.println(".");
      index.indexRequest("myid", "myuri1", new Date().getTime());
      System.out.println(".");
      index.indexRequest("myid2", "myuri2", new Date().getTime());
      System.out.println(".");
      index.indexRequest("myid3", "myuri3", new Date().getTime());
      index.indexResponse("myid5", "con carne");
      index.indexRequest("myid5", "myuri5", 1000);
      index.indexResponse("myid6", "con carne et salsa");
      index.indexRequest("myid6", "myuri6", 2000);
      index.indexResponse("myid7", "con carne et salsa et more");
*/
      index.query(0, 3000, "carne et prima");
    }
  }

}
