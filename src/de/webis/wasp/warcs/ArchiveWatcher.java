package de.webis.wasp.warcs;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cmu.lemurproject.WarcRecord;

/**
 * Creates a {@link Thread} that watches a web archives repository for new
 * content and passes the new records to a consumer.
 * <p>
 * If archives exist already in the directory, they are read in order of their
 * last modified dates (if set so in the constructor). In this case, it will
 * monitor the latest archive for changes, but not the others!
 * </p><p>
 * Currently, this treats every file (or directory) within the target directory
 * as an archive and tries to read from it.
 * </p>
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class ArchiveWatcher
extends Thread
implements AutoCloseable {
  
  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(ArchiveWatcher.class.getName());
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  private final Path directory;
  
  private final WatchService watchService;
  
  private final Consumer<WarcRecord> consumer;
  
  private WarcRecordReader reader;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTION
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Create a new watcher for given directory.
   * @param directory The directory that contains the archive files
   * @param readExistingRecords Whether records that already exist in the
   * archives in the directory should be read
   * @param consumer The consumer to which the records will be passed
   * @throws IOException On reading records
   */
  public ArchiveWatcher(
      final Path directory, final boolean readExistingRecords,
      final Consumer<WarcRecord> consumer)
  throws IOException {
    if (consumer == null) { throw new NullPointerException(); }
    this.directory = directory;
    this.consumer = consumer;
    this.reader = null;

    this.initForDirectory(readExistingRecords);

    this.watchService = FileSystems.getDefault().newWatchService();
    this.getDirectory().register(this.getWatchService(),
        StandardWatchEventKinds.ENTRY_CREATE);
  }
  
  private void initForDirectory(final boolean readExistingRecords)
  throws IOException {
    final File[] children = this.getDirectory().toFile().listFiles();
    Arrays.sort(children, new Comparator<File>() {
      @Override
      public int compare(final File o1, final File o2) {
        return Long.compare(o1.lastModified(), o2.lastModified());
      }
    });

    if (readExistingRecords) {
      // Read what should be closed files
      if (children.length >= 2) {
        for (final File child
            : Arrays.copyOfRange(children, 0, children.length - 1)) {
          try (final WarcRecordReader reader = new WarcRecordReader(
              this.getDirectory().resolve(child.getName()),
              this.getConsumer())) {
            reader.run();
          }
        }
      }
    }
    
    // Read what may be the open file
    if (children.length >= 1) {
      this.openFile(this.getDirectory().resolve(
          children[children.length - 1].getName()), readExistingRecords);
    }
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // GETTERS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Gets the directory being watched.
   * @return The directory
   */
  public Path getDirectory() {
    return this.directory;
  }

  /**
   * Gets the service watching for changes in the directory.
   * @return The service
   */
  protected WatchService getWatchService() {
    return this.watchService;
  }

  /**
   * Gets the consumer to which WARC records are passed to.
   * @return The consumer
   */
  public Consumer<WarcRecord> getConsumer() {
    return this.consumer;
  }

  /**
   * Gets the current WARC record reader.
   * @return The reader
   */
  protected WarcRecordReader getReader() {
    return this.reader;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // SETTER
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Sets the WARC record reader.
   * @param reader The reader
   */
  public void setReader(final WarcRecordReader reader) {
    this.reader = reader;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  @Override
  public void run() {
    final Path directory = this.getDirectory();
    try {
      while (true) {
        final WatchKey key = this.getWatchService().take();
        for (final WatchEvent<?> event : key.pollEvents()) {
          final WatchEvent.Kind<?> kind = event.kind();
          if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            final Path inputFile = directory.resolve((Path) event.context());
            LOG.fine("New file created in " + directory + ": " + inputFile);
            this.openFile(inputFile, true);
          } else if (kind == StandardWatchEventKinds.OVERFLOW) {
            LOG.warning("Overflow detected when watching " + directory);
          } else {
            LOG.warning("Unknown watch event kind '" + kind + "' when watching "
                + directory);
          }
        }
        
        if (!key.reset()) {
          LOG.severe(
              "Directory " + directory + " can no longer be watched");
          break;
        }
      }
    } catch (final InterruptedException exception) {
      LOG.log(Level.SEVERE,
          "Interrupted watching " + directory, exception);
    } catch (final IOException exception) {
      LOG.log(Level.SEVERE, "Error watching " + directory, exception);
    }
  }
  
  @Override
  public void close() throws IOException {
    this.closeFile();
  }

  /**
   * Closes the currently opened file, if any.
   * @throws IOException On closing the file
   * @see {@link #openFile(Path, boolean)}
   */
  protected void closeFile() throws IOException {
    synchronized (this) {
      final WarcRecordReader reader = this.getReader();
      if (reader != null) {
        this.setReader(null);
        reader.close();
      }
    }
  }

  /**
   * Starts reading from a new file, keeping watch if records are appended.
   * @param inputFile The file to read
   * @param consumeExistingRecords Whether to also pass existing records to the
   * consumer
   * @throws IOException On opening the file
   */
  protected void openFile(
      final Path inputFile, final boolean consumeExistingRecords)
  throws IOException {
    synchronized (this) {
      this.closeFile();
      final WarcRecordReader reader = new ContinuousWarcRecordReader(
          inputFile, consumeExistingRecords, this.getConsumer(), 1000);
      this.setReader(reader);
      reader.start();
    }
  }

}
