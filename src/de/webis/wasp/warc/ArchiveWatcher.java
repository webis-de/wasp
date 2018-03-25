package de.webis.wasp.warc;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cmu.lemurproject.WarcRecord;

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
  
  public ArchiveWatcher(
      final Path directory, final Consumer<WarcRecord> consumer)
  throws IOException {
    if (consumer == null) { throw new NullPointerException(); }
    this.directory = directory;
    this.consumer = consumer;
    this.reader = null;
    
    this.readAllFilesInDirectory();

    this.watchService = FileSystems.getDefault().newWatchService();
    this.directory.register(this.watchService,
        StandardWatchEventKinds.ENTRY_CREATE);
  }
  
  protected void readAllFilesInDirectory() throws IOException {
    for (final String childName : this.directory.toFile().list()) {
      final Path child = this.directory.resolve(childName);
      try (final WarcReader reader = new WarcReader(child, this.consumer)) {
        reader.run();
      }
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
    this.reader = new BlockingWarcReader(inputFile, this.consumer, 1000);
    this.reader.start();
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // MAIN
  /////////////////////////////////////////////////////////////////////////////
  
  public static void main(final String[] args) throws IOException {
    final Logger packageLogger = Logger.getLogger("de.webis.wasp.warc");
    packageLogger.setLevel(Level.FINE);
    final Handler handler = new ConsoleHandler();
    handler.setLevel(Level.FINE);
    packageLogger.addHandler(handler);
    
    final Path directory = Paths.get("/home/dogu3912/tmp/warcprox/archive");
    final Consumer<WarcRecord> consumer = record -> {
      System.out.println(record.getHeaderMetadataItem(Warcs.HEADER_TARGET_URI));
    };
    try (final ArchiveWatcher watcher = new ArchiveWatcher(directory, consumer)) {
      watcher.run();
    }
  }

}
