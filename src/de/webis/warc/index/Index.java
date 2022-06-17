package de.webis.warc.index;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

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
      "{\"mappings\": {\"properties\": {\n" +
      "  {\"" + TYPE_NAME + "\":{\n" + 
      "    \"properties\":{\n" +
      "      \"" + FIELD_TITLE_NAME + "\":{\"type\":\"text\"},\n" +
      "      \"" + FIELD_CONTENT_NAME + "\":{\"type\":\"text\"},\n" +
      "      \"" + FIELD_REVISITED_NAME + "\":{\"type\":\"keyword\"},\n" +
      "      \"" + FIELD_REQUEST_NAME + "\":{\n" +
      "        \"type\":\"nested\",\n" +
      "        \"properties\":{\n" +
      "          \""+ FIELD_URI_NAME + "\":{\"type\":\"keyword\"},\n" +
      "          \""+ FIELD_DATE_NAME + "\":{\"type\":\"date\"}\n" +
      "        }\n" +
      "      }\n" +
      "    }\n" +
      "  }}\n" +
      "}}}";

  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .registerModule(new SimpleModule()
          .addSerializer(new InstantSerializer())
          .addDeserializer(Instant.class, new InstantDeserializer()));
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final ElasticsearchClient client;

  protected final RestClient lowLevelClient;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  public Index() {
    this(DEFAULT_PORT);
  }
  
  public Index(final int port) {
    this.lowLevelClient =
        RestClient.builder(new HttpHost("localhost", port)).build();

    final JacksonJsonpMapper mapper = new JacksonJsonpMapper(OBJECT_MAPPER);
    final ElasticsearchTransport transport =
        new RestClientTransport(this.lowLevelClient, mapper);
    this.client = new ElasticsearchClient(transport);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  public void initialize() throws IOException {
    final CreateIndexRequest createIndexRequest = CreateIndexRequest.of(
        indexBuilder -> indexBuilder
          .index(INDEX_NAME)
          .withJson(new StringReader(TYPE_MAPPING)));
    LOG.info("Created index with mapping:\n" + TYPE_MAPPING);
    this.client.indices().create(createIndexRequest);
  }
  
  @Override
  public void close() throws IOException {
    this.lowLevelClient.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  // ID ACCESS
  
  public String resolveConcurrentId(final String id) throws IOException {
    final GetResponse<ObjectNode> getIdResponse = this.getResponse(id);
    if (getIdResponse == null) {
      return null;
    } else {
      return getIdResponse.id();
    }
  }
  
  public GetResponse<ObjectNode> getResponse(final String id)
  throws IOException {
    final GetResponse<ObjectNode> indexResponse = this.client.get(
        request -> request.index(INDEX_NAME).id(id),
        ObjectNode.class);
    if (indexResponse.found()) {
      final ObjectNode response = indexResponse.source();
      final JsonNode revisited = response.get(FIELD_REVISITED_NAME);
      if (revisited == null) {
        return indexResponse;
      } else {
        return this.getResponse(revisited.asText());
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
    

    final ObjectNode document = OBJECT_MAPPER.createObjectNode();
    document.set(FIELD_CONTENT_NAME,
        OBJECT_MAPPER.convertValue(content, JsonNode.class));
    document.set(FIELD_TITLE_NAME,
        OBJECT_MAPPER.convertValue(title, JsonNode.class));
    document.set(FIELD_REQUEST_NAME,
        OBJECT_MAPPER.convertValue(Collections.EMPTY_LIST, JsonNode.class));
    final IndexRequest<ObjectNode> indexRequest = IndexRequest.of(
        builder -> builder
          .index(INDEX_NAME)
          .id(id)
          .document(document));
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

    final ObjectNode document = OBJECT_MAPPER.createObjectNode();
    document.set(FIELD_REVISITED_NAME,
        OBJECT_MAPPER.convertValue(responseId, JsonNode.class));
    final IndexRequest<ObjectNode> indexRequest = IndexRequest.of(
        builder -> builder
          .index(INDEX_NAME)
          .id(id)
          .document(document));
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
    
    final Map<String, String> requestMap = new HashMap<>();
    requestMap.put(FIELD_URI_NAME, uri);
    requestMap.put(FIELD_DATE_NAME, instant.toString());
    final Map<String, JsonData> parameters =
        Collections.singletonMap("request", JsonData.of(requestMap));
    
    final String scriptSource = 
        "ctx._source." + FIELD_REQUEST_NAME + ".add(params.request);";

    final UpdateRequest<ObjectNode, ObjectNode> updateRequest =
        UpdateRequest.of(builder -> builder
            .index(INDEX_NAME)
            .id(responseId)
            .script(script -> script.inline(inline -> inline
                .lang("painless")
                .source(scriptSource)
                .params(parameters))));
    this.client.update(updateRequest, ObjectNode.class);
    return true;
  }

  /////////////////////////////////////////////////////////////////////////////
  // QUERY
  
  public ResultsFetcher query(final Query query, final int pageSize) {
    return new ResultsFetcher(this, query, pageSize);
  }
  
  protected SearchResponse<ObjectNode> search(final SearchRequest searchRequest)
  throws IOException {
    return this.client.search(searchRequest, ObjectNode.class);
  }
  

  /////////////////////////////////////////////////////////////////////////////
  // JSON BINDINGS
  
  /**
   * Serializer for {@link Instant} using ISO-8601.
   *
   * @author johannes.kiesel@uni-weimar.de
   * @see InstantDeserializer
   * @see DateTimeFormatter#ISO_INSTANT
   *
   */
  public static class InstantSerializer extends StdSerializer<Instant> {

    private static final long serialVersionUID = 2795427768750728869L;

    /**
     * Creates a new serializer.
     */
    public InstantSerializer() {
      super(Instant.class);
    }

    @Override
    public void serialize(
        final Instant value,
        final JsonGenerator generator,
        final SerializerProvider provider)
    throws IOException {
      generator.writeString(value.toString());
    }
    
  }

  /**
   * Deserializer for {@link Instant} using ISO-8601.
   *
   * @author johannes.kiesel@uni-weimar.de
   * @see InstantSerializer
   * @see DateTimeFormatter#ISO_INSTANT
   *
   */
  public static class InstantDeserializer extends StdDeserializer<Instant> {

    private static final long serialVersionUID = -3591379516415686398L;

    /**
     * Creates a new deserializer.
     */
    public InstantDeserializer() {
      super(Instant.class);
    }

    @Override
    public Instant deserialize(
        final JsonParser parser,
        final DeserializationContext context)
    throws IOException, JsonProcessingException {
      final String text = parser.getValueAsString();
      return Instant.parse(text);
    }
    
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
