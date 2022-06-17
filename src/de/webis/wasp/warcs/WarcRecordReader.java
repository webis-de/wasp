package de.webis.wasp.warcs;

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
 * If the archive is still being filled, use {@link ContinuousWarcRecordReader}
 * instead.
 * </p>
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class WarcRecordReader
extends Thread
implements AutoCloseable {
  
  /////////////////////////////////////////////////////////////////////////////
  // LOGGING
  /////////////////////////////////////////////////////////////////////////////
  
  private static final Logger LOG =
      Logger.getLogger(WarcRecordReader.class.getName());
  
  /////////////////////////////////////////////////////////////////////////////
  // MEMBERS
  /////////////////////////////////////////////////////////////////////////////
  
  private final Consumer<WarcRecord> consumer;
  
  private final Path inputFile;
  
  private final DataInputStream input;
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTION
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Creates a new reader for an archive.
   * @param inputFile The archive file
   * @param consumer Consumer for the WARC records that are read
   * @throws IOException When the file can not be opened
   */
  public WarcRecordReader(
      final Path inputFile, final Consumer<WarcRecord> consumer)
  throws IOException {
    if (consumer == null) { throw new NullPointerException(); }
    this.consumer = consumer;
    this.inputFile = inputFile;
    this.input = this.openDataInputStream();
  }

  /**
   * Opens a data input stream to the reader's file, applying GZip decompression
   * if the file ends on <code>.gz</code>.
   * @return The input stream
   * @throws IOException On opening the file
   */
  protected DataInputStream openDataInputStream()
  throws IOException {
    final InputStream inputStream = this.openFileInputStream();
    if (this.getInputFile().toString().toLowerCase().endsWith(".gz")) {
      return new DataInputStream(new GZIPInputStream(inputStream));
    } else {
      return new DataInputStream(inputStream);
    }
  }

  /**
   * Opens an input stream to the reader's file.
   * @return The input stream
   * @throws IOException On opening the file
   */
  protected FileInputStream openFileInputStream()
  throws IOException {
    final File file = this.getInputFile().toFile();
    LOG.fine("Open file: " + file);
    return new FileInputStream(file);
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // CONSTRUCTION
  /////////////////////////////////////////////////////////////////////////////

  /**
   * Gets the file this reader reads from.
   * @return The file
   */
  public Path getInputFile() {
    return this.inputFile;
  }

  /**
   * Gets the consumer to which WARC records are passed to.
   * @return The consumer
   */
  public Consumer<WarcRecord> getConsumer() {
    return this.consumer;
  }

  /**
   * Gets the input stream.
   * @return The stream
   */
  protected DataInputStream getInput() {
    return this.input;
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // FUNCTIONALITY
  /////////////////////////////////////////////////////////////////////////////
  
  @Override
  public void run() {
    final DataInputStream input = this.getInput();
    try {
      WarcRecord record = WarcRecord.readNextWarcRecord(input);
      while (record != null) {
        this.consume(record);
        record = WarcRecord.readNextWarcRecord(input); 
      }
      LOG.fine("Finished " + this);
      this.close();
    } catch (final IOException exception) {
      LOG.log(Level.SEVERE,
          "Error while reading from " + this.getInputFile(), exception);
    }
  }
  
  @Override
  public void close() throws IOException {
    LOG.fine("Close file " + this.getInputFile());
    this.getInput().close();
  }
  
  @Override
  public String toString() {
    return this.getInputFile() + " -> " + this.getConsumer();
  }
  
  /////////////////////////////////////////////////////////////////////////////
  // HELPERS
  /////////////////////////////////////////////////////////////////////////////
  
  /**
   * Passes the record to the consumer.
   * @param record The record
   * @see #getConsumer()
   */
  protected void consume(final WarcRecord record) {
    this.getConsumer().accept(record);
  }

}
