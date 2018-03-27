package de.webis.html;

import java.util.function.Function;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;

public class JerichoExtractor implements Function<String, String> {
  
  public static final JerichoExtractor INSTANCE = new JerichoExtractor();
  
  protected JerichoExtractor() { }

  @Override
  public String apply(final String html) {
    try {
      final Source source = new Source(html);
      final Segment segment = new Segment(source, 0, html.length());
      final Renderer renderer = new Renderer(segment);
      renderer.setMaxLineLength(0);
      renderer.setIncludeHyperlinkURLs(false);
      renderer.setIncludeAlternateText(true);
      return renderer.toString();
    } catch (final Error error) {
      return null;
    }
  }

}
