package de.webis.wasp.index;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
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

  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .registerModule(new SimpleModule()
          .addSerializer(new InstantSerializer())
          .addDeserializer(Instant.class, new InstantDeserializer()));

  protected static final JacksonJsonpMapper MAPPER =
      new JacksonJsonpMapper(OBJECT_MAPPER);
  
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
    final ElasticsearchTransport transport =
        new RestClientTransport(this.lowLevelClient, MAPPER);
    this.client = new ElasticsearchClient(transport);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  public void initialize() throws IOException {
    final CreateIndexRequest createIndexRequest = CreateIndexRequest.of(
        indexBuilder -> indexBuilder
          .index(INDEX_NAME)
          .mappings(mappings -> mappings
              .properties(ResponseRecord.TYPE_PROPERTIES)));
    LOG.info("Created index: " + createIndexRequest);
    this.client.indices().create(createIndexRequest);
  }
  
  @Override
  public void close() throws IOException {
    this.lowLevelClient.close();
  }

  /////////////////////////////////////////////////////////////////////////////
  // ID ACCESS
  
  public String resolveConcurrentId(final String id) throws IOException {
    final GetResponse<ResponseRecord> getResponse = this.getResponse(id);
    if (getResponse == null) {
      return null;
    } else {
      return getResponse.id();
    }
  }
  
  public GetResponse<ResponseRecord> getResponse(final String id)
  throws IOException {
    final GetResponse<ResponseRecord> getResponse = this.client.get(
        get -> get.index(INDEX_NAME).id(id),
        ResponseRecord.class);
    if (getResponse.found()) {
      final ResponseRecord response = getResponse.source();
      if (response.isRevisit()) {
        return this.getResponse(response.getRevisited());
      } else {
        return getResponse;
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
    final IndexRequest<ResponseRecord> indexRequest = IndexRequest.of(
        builder -> builder
          .index(INDEX_NAME)
          .id(Objects.requireNonNull(id))
          .document(ResponseRecord.forPage(title, content)));
    this.client.index(indexRequest);
    return true;
  }
  
  public boolean indexRevisit(final String id, final String responseId)
  throws IOException {
    if (this.resolveConcurrentId(responseId) == null) {
      LOG.fine("No response found for ID = " + responseId);
      return false;
    }

    final IndexRequest<ResponseRecord> indexRequest = IndexRequest.of(
        builder -> builder
          .index(INDEX_NAME)
          .id(Objects.requireNonNull(id))
          .document(ResponseRecord.forRevisit(responseId)));
    this.client.index(indexRequest);
    return true;
  }
  
  public boolean indexRequest(
      final String concurrentId, final String uri, final Instant instant)
  throws IOException {
    final String responseId = this.resolveConcurrentId(concurrentId);
    if (responseId == null) {
      LOG.fine("No response found for ID = " + concurrentId);
      return false;
    }

    final String field = ResponseRecord.FIELD_REQUESTS;
    final Map<String, JsonData> params = Map.of(field, JsonData.of(
        new RequestRecord(uri, instant), MAPPER));
    final String scriptSource = 
        "ctx._source." + field + ".add(params." + field + ");";

    final UpdateRequest<ResponseRecord, ObjectNode> updateRequest =
        UpdateRequest.of(builder -> builder
            .index(INDEX_NAME)
            .id(responseId)
            .script(script -> script.inline(inline -> inline
                .lang("painless")
                .source(scriptSource)
                .params(params))));
    this.client.update(updateRequest, ResponseRecord.class);
    return true;
  }

  /////////////////////////////////////////////////////////////////////////////
  // QUERY
  
  public ResultsFetcher query(final Query query, final int pageSize) {
    return new ResultsFetcher(this, query, pageSize);
  }
  
  protected SearchResponse<ResponseRecord> search(
      final SearchRequest searchRequest)
  throws IOException {
    return this.client.search(searchRequest, ResponseRecord.class);
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
