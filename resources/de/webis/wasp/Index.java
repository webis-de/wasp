package de.webis.wasp.index;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 * The WASP index client.
 * 
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class Index
implements AutoCloseable {

  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(Index.class.getName());

  /////////////////////////////////////////////////////////////////////////////
  // CONSTANTS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Default port of the index.
   */
  public static final int DEFAULT_PORT = 9200;

  /**
   * Name of the index to use.
   */
  public static final String INDEX_NAME = "archive";

  /**
   * Default number of results to retrieve at most from the index at once.
   */
  public static final int DEFAULT_MAX_RESULTS = 100;

  /**
   * Object mapper for JSON (de-)serialization.
   */
  protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
      .registerModule(new SimpleModule()
          .addSerializer(new InstantSerializer())
          .addDeserializer(Instant.class, new InstantDeserializer()));

  /**
   * Elasticsearch object mapper for JSON (de-)serialization.
   */
  protected static final JacksonJsonpMapper MAPPER =
      new JacksonJsonpMapper(OBJECT_MAPPER);
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  private final ElasticsearchClient client;

  private final RestClient lowLevelClient;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTION
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Creates a new index client talking to the index at the default port.
   * @see #DEFAULT_PORT
   */
  public Index() {
    this(DEFAULT_PORT);
  }

  /**
   * Creates a new index client talking to the index at the specified port.
   * @param port The port
   */
  public Index(final int port) {
    this.lowLevelClient =
        RestClient.builder(new HttpHost("localhost", port)).build();
    final ElasticsearchTransport transport =
        new RestClientTransport(this.lowLevelClient, MAPPER);
    this.client = new ElasticsearchClient(transport);
  }

  /**
   * Initializes the index.
   * <p>
   * This method must be called one time, but not again even after a restart of
   * WASP.
   * </p>
   * @throws IOException On initializing the index
   */
  public void initialize()
  throws IOException {
    final CreateIndexRequest createIndexRequest = CreateIndexRequest.of(
        indexBuilder -> indexBuilder
          .index(INDEX_NAME)
          .mappings(mappings -> mappings
              .properties(ResponseRecord.TYPE_PROPERTIES)));
    LOG.info("Created index: " + createIndexRequest);
    this.getClient().indices().create(createIndexRequest);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // GETTERS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Gets the low level REST client used to communicate with the index.
   * @return The client
   */
  protected RestClient getLowLevelClient() {
    return this.lowLevelClient;
  }


  /**
   * Gets the high level client used to communicate with the index.
   * @return The client
   */
  protected ElasticsearchClient getClient() {
    return this.client;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  @Override
  public void close() throws IOException {
    this.getLowLevelClient().close();
  }

  /////////////////////////////////////////////////////////////////////////////
  // INDEXING

  /**
   * Indexes a response record.
   * @param id The ID of the response
   * @param content The extracted content from the response
   * @param title The title of the response
   * @return Whether the response has been indexed (always)
   * @throws IOException On writing to the index
   */
  public boolean indexResponse(
      final String id, final String content, final String title)
  throws IOException {
    final IndexRequest<ResponseRecord> indexRequest = IndexRequest.of(
        builder -> builder
          .index(INDEX_NAME)
          .id(Objects.requireNonNull(id))
          .document(ResponseRecord.forPage(title, content)));
    this.getClient().index(indexRequest);
    LOG.fine("Index response " + id);
    return true;
  }

  /**
   * Indexes a revisit record.
   * @param id The ID of the revisit
   * @param responseId The ID of the revisited response
   * @return Whether the revisit has been indexed (not if no such response
   * exists)
   * @throws IOException On reading or writing to the index
   */
  public boolean indexRevisit(final String id, final String responseId)
  throws IOException {
    if (this.resolveResponse(responseId) == null) {
      LOG.fine("No response found for ID = " + responseId + " for revisit");
      return false;
    }

    final IndexRequest<ResponseRecord> indexRequest = IndexRequest.of(
        builder -> builder
          .index(INDEX_NAME)
          .id(Objects.requireNonNull(id))
          .document(ResponseRecord.forRevisit(responseId)));
    this.getClient().index(indexRequest);
    LOG.fine("Index revisit " + id + " -> " + responseId);
    return true;
  }

  /**
   * Indexes a request record.
   * @param concurrentId The ID of the concurrent response
   * @param uri The URI of the request
   * @param instant The time of the request
   * @return Whether the request has been indexed (not if no such response
   * exists)
   * @throws IOException On reading or writing to the index
   */
  public boolean indexRequest(
      final String concurrentId, final String uri, final Instant instant)
  throws IOException {
    final GetResponse<ResponseRecord> response =
        this.resolveResponse(concurrentId);
    if (response == null) {
      LOG.fine("No response found for ID = " + concurrentId + " for request");
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
            .id(response.id())
            .script(script -> script.inline(inline -> inline
                .lang("painless")
                .source(scriptSource)
                .params(params))));
    this.getClient().update(updateRequest, ResponseRecord.class);
    LOG.fine("Index request -> " + concurrentId);
    return true;
  }

  /////////////////////////////////////////////////////////////////////////////
  // SEARCH

  /**
   * Searches the index.
   * @param query The query to match responses and requests by
   * @return The results
   * @throws IOException On searching the index
   * @see #DEFAULT_MAX_RESULTS
   */
  public List<Result> search(final Query query)
  throws IOException {
    return this.search(query, DEFAULT_MAX_RESULTS);
  }

  /**
   * Searches the index.
   * @param query The query to match responses and requests by
   * @param maxResults The maximum number of results to get
   * @return The results
   * @throws IOException On searching the index
   */
  public List<Result> search(final Query query, final int maxResults)
  throws IOException {
    return this.search(query, maxResults, 0);
  }

  /**
   * Searches the index.
   * @param query The query to match responses and requests by
   * @param maxResults The maximum number of results to get
   * @param offset The offset of the first result to get
   * @return The results
   * @throws IOException On searching the index
   */
  public List<Result> search(
      final Query query, final int maxResults, final int offset)
  throws IOException {
    final SearchResponse<ResponseRecord> search = this.getClient().search(
        query.build(maxResults).from(offset).build(), ResponseRecord.class);
    final HitsMetadata<ResponseRecord> hits = search.hits();

    final List<Result> results = new ArrayList<>();
    for (final Hit<ResponseRecord> hit : hits.hits()) {
      final Result result = Result.fromHit(hit, query.getFrom(), query.getTo());
      if (!result.hasEmptySnippet()) { results.add(result); }
    }
    return results;
  }

  /////////////////////////////////////////////////////////////////////////////
  // HELPERS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Gets the response with the specified ID while resolving revisits.
   * @param id The response or revisit ID
   * @return The response
   * @throws IOException On searching the index
   */
  protected GetResponse<ResponseRecord> resolveResponse(final String id)
  throws IOException {
    final GetResponse<ResponseRecord> getResponse = this.getClient().get(
        get -> get.index(INDEX_NAME).id(id),
        ResponseRecord.class);
    if (getResponse.found()) {
      final ResponseRecord response = getResponse.source();
      final String revisited = response.getRevisited();
      if (revisited != null) {
        return this.resolveResponse(revisited);
      } else {
        return getResponse;
      }
    } else {
      return null;
    }
  }
  

  /////////////////////////////////////////////////////////////////////////////
  // JSON BINDINGS
  /////////////////////////////////////////////////////////////////////////////
  
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
