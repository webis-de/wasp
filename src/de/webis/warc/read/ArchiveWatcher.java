package de.webis.warc.read;

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
public class ArchiveWatcher extends Thread implements AutoCloseable {
  
  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(ArchiveWatcher.class.getName());
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final Path directory;
  
  protected final WatchService watchService;
  
  protected final Consumer<WarcRecord> consumer;
  
  protected WarcReader reader;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Create a new watcher for given directory.
   * @param directory The directory that contains the archive files
   * @param readExistingArchives Whether archives that already exist in the
   * directory should be read
   * @param consumer The consumer to which the records will be passed
   * @throws IOException
   */
  public ArchiveWatcher(
      final Path directory, final boolean readExistingArchives,
      final Consumer<WarcRecord> consumer)
  throws IOException {
    if (consumer == null) { throw new NullPointerException(); }
    this.directory = directory;
    this.consumer = consumer;
    this.reader = null;
    
    if (readExistingArchives) {
      this.readAllFilesInDirectory();
    }

    this.watchService = FileSystems.getDefault().newWatchService();
    this.directory.register(this.watchService,
        StandardWatchEventKinds.ENTRY_CREATE);
  }
  
  protected void readAllFilesInDirectory() throws IOException {
    final File[] children = this.directory.toFile().listFiles();
    Arrays.sort(children, new Comparator<File>() {
      @Override
      public int compare(final File o1, final File o2) {
        return Long.compare(o1.lastModified(), o2.lastModified());
      }
    });

    // Read what should be closed files
    if (children.length >= 2) {
      for (final File child
          : Arrays.copyOfRange(children, 0, children.length - 1)) {
        try (final WarcReader reader = new WarcReader(
            this.directory.resolve(child.getName()), this.consumer)) {
          reader.run();
        }
      }
    }
    
    // Read what may be the open file
    if (children.length >= 1) {
      this.openFile(this.directory.resolve(
          children[children.length - 1].getName()));
    }
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  @Override
  public void run() {
    try {
      while (true) {
        final WatchKey key = this.watchService.take();
        for (final WatchEvent<?> event : key.pollEvents()) {
          final WatchEvent.Kind<?> kind = event.kind();
          if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            final Path inputFile = this.directory.resolve((Path) event.context());
            LOG.fine("New file created in " + this.directory + ": " + inputFile);
            this.openFile(inputFile);
          } else if (kind == StandardWatchEventKinds.OVERFLOW) {
            LOG.warning("Overflow detected when watching " + this.directory);
          } else {
            LOG.warning("Unknown watch event kind '" + kind + "' when watching "
                + this.directory);
          }
        }
        
        if (!key.reset()) {
          LOG.severe(
              "Directory " + this.directory + " can no longer be watched");
          break;
        }
      }
    } catch (final InterruptedException exception) {
      LOG.log(Level.SEVERE,
          "Interrupted watching " + this.directory, exception);
    } catch (final IOException exception) {
      LOG.log(Level.SEVERE, "Error watching " + this.directory, exception);
    }
  }
  
  @Override
  public void close() throws IOException {
    this.closeFile();
  }
  
  protected void closeFile() throws IOException {
    if (this.reader != null) {
      this.reader.close();
      this.reader = null;
    }
  }
  
  protected void openFile(final Path inputFile) throws IOException {
    this.closeFile();
    this.reader = new OpenWarcReader(inputFile, this.consumer, 1000);
    this.reader.start();
  }

}
