package de.webis.warc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cmu.lemurproject.WarcRecord;

/**
 * A {@link WarcReader} that will wait for new content even when it reached the
 * end of the archive.
 * <p>
 * This class should be used for archives that are still filled. When you use
 * {@link #close()}, this reader will still continue to read until it
 * encounters the end of the file the next time.
 * </p>
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class OpenWarcReader extends WarcReader {

  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(OpenWarcReader.class.getName());

  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final long pollIntervalMillis;
  
  protected boolean closed;

  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Creates a new reader for an archive that is still being filled.
   * @param inputFile The archive file
   * @param consumer Consumer for the WARC records that are read
   * @param pollIntervalMillis On encountering the end of archive, poll the file
   * in this interval to check when it has more content 
   * @throws IOException When the file can not be opened
   */
  public OpenWarcReader(
      final Path inputFile, final Consumer<WarcRecord> consumer,
      final long pollIntervalMillis)
  throws IOException {
    super(inputFile, consumer);
    this.closed = false;
    this.pollIntervalMillis = pollIntervalMillis;
  }
  
  @Override
  protected FileInputStream openFileInputStream(final Path inputFile)
  throws IOException {
    final File file = inputFile.toFile();
    LOG.fine("Open file: " + inputFile);
    return new OpenFileInputStream(file);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  @Override
  public void close() throws IOException {
    LOG.fine("Closing " + this.inputFile);
    this.closed = true;
  }

  protected void closeStream() throws IOException {
    super.close();
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // HELPER CLASSES
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Modification of {@link FileInputStream} that does waits at the end of the
   * file for more content to appear.
   *
   * @author johannes.kiesel@uni-weimar.de
   */
  protected class OpenFileInputStream extends FileInputStream {

    public OpenFileInputStream(final File file)
    throws FileNotFoundException {
      super(file);
    }
    
    @Override
    public int available() throws IOException {
      int available = super.available();
      try {
        while (available == 0 && !OpenWarcReader.this.closed) {
          Thread.sleep(OpenWarcReader.this.pollIntervalMillis);
          available = super.available();
        }
      } catch (final InterruptedException exception) {
        LOG.log(Level.WARNING, "Interrupted " + this, exception);
      }
      
      if (OpenWarcReader.this.closed) {
        OpenWarcReader.this.closeStream();
      }
      return available;
    }
    
    @Override
    public int read() throws IOException {
      int read = super.read();
      try {
        while (read == -1 && !OpenWarcReader.this.closed) {
          Thread.sleep(OpenWarcReader.this.pollIntervalMillis);
          read = super.read();
        }
      } catch (final InterruptedException exception) {
        LOG.log(Level.WARNING, "Interrupted " + this, exception);
      }

      if (OpenWarcReader.this.closed) {
        OpenWarcReader.this.closeStream();
      }
      return read;
    }

    @Override
    public int read(byte b[]) throws IOException {
      int read = super.read(b);
      try {
        while (read == -1 && !OpenWarcReader.this.closed) {
          Thread.sleep(OpenWarcReader.this.pollIntervalMillis);
          read = super.read(b);
        }
      } catch (final InterruptedException exception) {
        LOG.log(Level.WARNING, "Interrupted " + this, exception);
      }

      if (OpenWarcReader.this.closed) {
        OpenWarcReader.this.closeStream();
      }
      return read;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
      int read = super.read(b, off, len);
      try {
        while (read == -1 && !OpenWarcReader.this.closed) {
          Thread.sleep(OpenWarcReader.this.pollIntervalMillis);
          read = super.read(b, off, len);
        }
      } catch (final InterruptedException exception) {
        LOG.log(Level.WARNING, "Interrupted " + this, exception);
      }

      if (OpenWarcReader.this.closed) {
        OpenWarcReader.this.closeStream();
      }
      return read;
    }
    
  }

}
