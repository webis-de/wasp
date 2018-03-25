package de.webis.wasp.warc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.logging.Logger;

import edu.cmu.lemurproject.WarcRecord;

public class BlockingWarcReader extends WarcReader {

  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(BlockingWarcReader.class.getName());

  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final long pollIntervalMillis;

  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////

  public BlockingWarcReader(
      final Path inputFile, final Consumer<WarcRecord> consumer,
      final long pollIntervalMillis)
  throws IOException {
    super(inputFile, consumer);
    this.pollIntervalMillis = pollIntervalMillis;
  }
  
  @Override
  protected FileInputStream openFileInputStream(final Path inputFile)
  throws IOException {
    final File file = inputFile.toFile();
    LOG.fine("Open file: " + inputFile);
    return new BlockingFileInputStream(file);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  @Override
  public void close() throws IOException {
    super.close();
    this.interrupt();
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // HELPER CLASSES
  /////////////////////////////////////////////////////////////////////////////
  
  protected class BlockingFileInputStream extends FileInputStream {

    public BlockingFileInputStream(final File file)
    throws FileNotFoundException {
      super(file);
    }
    
    @Override
    public int available() throws IOException {
      int available = super.available();
      try {
        while (available == 0) {
          Thread.sleep(BlockingWarcReader.this.pollIntervalMillis);
          available = super.available();
        }
      } catch (final InterruptedException exception) {
        LOG.fine("Interrupted " + this);
      }
      return available;
    }
    
    @Override
    public int read() throws IOException {
      int read = super.read();
      try {
        while (read == -1) {
          Thread.sleep(BlockingWarcReader.this.pollIntervalMillis);
          read = super.read();
        }
      } catch (final InterruptedException exception) {
        LOG.fine("Interrupted " + this);
      }
      return read;
    }

    public int read(byte b[]) throws IOException {
      int read = super.read(b);
      try {
        while (read == -1) {
          Thread.sleep(BlockingWarcReader.this.pollIntervalMillis);
          read = super.read(b);
        }
      } catch (final InterruptedException exception) {
        LOG.fine("Interrupted " + this);
      }
      return read;
    }

    public int read(byte b[], int off, int len) throws IOException {
      int read = super.read(b, off, len);
      try {
        while (read == -1) {
          Thread.sleep(BlockingWarcReader.this.pollIntervalMillis);
          read = super.read(b, off, len);
        }
      } catch (final InterruptedException exception) {
        LOG.fine("Interrupted " + this);
      }
      return read;
    }
    
  }

}
