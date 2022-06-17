package de.webis.wasp;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.webis.wasp.index.Index;
import de.webis.wasp.index.WarcIndexer;
import de.webis.wasp.warcs.ArchiveWatcher;

/**
 * Service to index WARC records.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class WarcIndexingService
extends ArchiveWatcher {

  private final Index index;

  /**
   * Creates a new WARC indexing service.
   * @param directory The directory that contains the archive files
   * @param port The port of the index to add new WARC records to
   * @throws IOException On reading records
   */
  public WarcIndexingService(final Path directory, final int port)
  throws IOException {
    this(directory, new Index(port));
  }

  /**
   * Creates a new WARC indexing service.
   * @param directory The directory that contains the archive files
   * @param index The index to add new WARC records to
   * @throws IOException On reading records
   */
  public WarcIndexingService(final Path directory, final Index index)
  throws IOException {
    super(directory, false, new WarcIndexer(index));
    this.index = index;
  }

  @Override
  public void close() throws IOException {
    super.close();
    this.index.close();
  }
  
  
  /**
   * Starts the service
   * @param args directory [index-port]
   * @throws IOException On reading or indexing
   */
  public static void main(final String[] args) throws IOException {
    final ConsoleHandler handler = new ConsoleHandler();
    handler.setLevel(Level.FINE);
    final Logger logger = Logger.getLogger("de.webis.wasp");
    logger.addHandler(handler);
    logger.setLevel(Level.FINE);
    
    final Path directory = Paths.get(args[0]);
    final int port =
        args.length != 2 ? Index.DEFAULT_PORT : Integer.parseInt(args[1]);
    try (final WarcIndexingService service =
        new WarcIndexingService(directory, port)) {
      service.run();
    }
  }

}
