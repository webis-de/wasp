package de.webis.warc.ui;

import java.io.PrintWriter;
import java.util.List;

import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import de.webis.warc.index.Query;
import de.webis.warc.index.Result;

public class ResultPageRenderer {
  
  protected static final DateTimeFormatter MACHINE_READABLE_FORMATTER =
      ISODateTimeFormat.ordinalDateTime();
  
  protected static final DateTimeFormatter HUMAN_READABLE_FORMATTER =
      ISODateTimeFormat.dateHourMinute();
  
  protected static final DateTimeFormatter REPLAY_FORMATTER =
      DateTimeFormat.forPattern("yyyyMMDDhhmmss");
  
  protected final int replayPort;
  
  protected final String replayCollection;
  
  public ResultPageRenderer() {
    this(SearchServlet.DEFAULT_REPLAY_PORT, SearchServlet.DEFAULT_REPLAY_COLLECTION);
  }
  
  public ResultPageRenderer(
      final int replayPort, final String replayCollection) {
    if (replayCollection == null) {
      throw new NullPointerException("collection");
    }
    this.replayPort = replayPort;
    this.replayCollection = replayCollection;
  }

  public void render(final PrintWriter output) {
    this.renderHeader(output);
    this.renderQueryBox(output, null);
    this.renderFooter(output);
  }
  
  public void render(final PrintWriter output,
      final Query query,
      final List<Result> page, final int pageNumber) {
    this.renderHeader(output);
    this.renderQueryBox(output, query);
    output.append("<div class='results'>\n");
    for (final Result result : page) {
      this.render(output, result);
    }
    output.append("/<div>\n");
    this.renderFooter(output);
  }
  
  protected void renderHeader(final PrintWriter output) {
    // TODO
  }
  
  protected void renderFooter(final PrintWriter output) {
    // TODO
    output.flush();
  }
  
  protected void renderQueryBox(final PrintWriter output, final Query query) {
    // TODO
  }
  
  protected void render(final PrintWriter output, final Result result) {
    output.append(String.format(
        "<div class='result'>\n" +
        "  <h5><a href='%s'>%s</a></h5><br/>\n" +
        "  <span class='uri'>%s</span> <time datetime='%s'>%s</time><br/>\n" +
        "  <span class='snippet'>%s</span>\n" +
        "</div>\n",
        this.getReplayUri(result),
        result.getTitle(),
        result.getUri(),
        this.getMachineReadableTime(result.getInstant()),
        this.getHumanReadableTime(result.getInstant()),
        result.getSnippet()));
  }
  
  protected String getReplayUri(final Result result) {
    return String.format(
        "http://localhost:%d/%s/%s/%s",
        this.replayPort, this.replayCollection,
        this.getReplayTime(result.getInstant()), result.getUri());
  }
  
  protected String getReplayTime(final Instant instant) {
    return instant.toString(REPLAY_FORMATTER);
  }
  
  protected String getMachineReadableTime(final Instant instant) {
    return instant.toString(MACHINE_READABLE_FORMATTER);
  }
  
  protected String getHumanReadableTime(final Instant instant) {
    return instant.toString(HUMAN_READABLE_FORMATTER);
  }
  
  public static void main(final String[] args) {
    
  }

}
