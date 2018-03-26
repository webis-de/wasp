package de.webis.warc;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import edu.cmu.lemurproject.WarcRecord;

/**
 * Reader for WARC files that passes all read records to a consumer.
 * <p>
 * Use the {@link #run()} or {@link #start()} methods to begin reading.
 * </p><p>
 * If the archive is still being filled, use {@link OpenWarcReader} instead.
 * </p>
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class WarcReader extends Thread implements AutoCloseable {
  
  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(WarcReader.class.getName());
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  protected final Consumer<WarcRecord> consumer;
  
  protected final Path inputFile;
  
  protected final DataInputStream input;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTORS
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Creates a new reader for an archive.
   * @param inputFile The archive file
   * @param consumer Consumer for the WARC records that are read
   * @throws IOException When the file can not be opened
   */
  public WarcReader(
      final Path inputFile, final Consumer<WarcRecord> consumer)
  throws IOException {
    if (consumer == null) { throw new NullPointerException(); }
    this.consumer = consumer;
    this.inputFile = inputFile;
    this.input = this.openDataInputStream(inputFile);
  }
  
  protected DataInputStream openDataInputStream(final Path inputFile)
  throws IOException {
    final InputStream inputStream = this.openFileInputStream(inputFile);
    if (inputFile.toString().endsWith(".gz")) {
      return new DataInputStream(new GZIPInputStream(inputStream));
    } else {
      return new DataInputStream(inputStream);
    }
  }
  
  protected FileInputStream openFileInputStream(final Path inputFile)
  throws IOException {
    final File file = inputFile.toFile();
    LOG.fine("Open file: " + inputFile);
    return new FileInputStream(file);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  @Override
  public void run() {
    try {
      WarcRecord record = WarcRecord.readNextWarcRecord(this.input);
      while (record != null) {
        this.consumer.accept(record);
        record = WarcRecord.readNextWarcRecord(this.input); 
      }
      LOG.fine("Finished " + this);
      this.close();
    } catch (final IOException exception) {
      LOG.log(Level.SEVERE,
          "Error while reading from " + this.inputFile, exception);
    }
  }
  
  @Override
  public void close() throws IOException {
    LOG.fine("Close file " + this.inputFile);
    this.input.close();
  }
  
  @Override
  public String toString() {
    return this.inputFile + " -> " + this.consumer;
  }

}
