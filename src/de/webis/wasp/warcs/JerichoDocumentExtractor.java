package de.webis.wasp.warcs;

import java.util.function.Function;

import de.webis.wasp.warcs.GenericHtmlWarcRecordConsumer.Document;
import net.htmlparser.jericho.CharacterReference;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

/**
 * A document extractor using Jericho HTML parser.
 *
 * @author johannes.kiesel@uni-weimar.de
 *
 */
public class JerichoDocumentExtractor
implements Function<String, Document> {

  /**
   * The single instance of the extractor.
   */
  public static final JerichoDocumentExtractor INSTANCE =
      new JerichoDocumentExtractor();

  protected JerichoDocumentExtractor() { }

  @Override
  public Document apply(final String html) {
    final Source source = new Source(html);
    
    final Renderer renderer = new Renderer(source);
    renderer.setMaxLineLength(0);
    renderer.setIncludeHyperlinkURLs(false);
    renderer.setIncludeAlternateText(true);
    final String content = renderer.toString();

    final Element titleElement =
        source.getFirstElement(HTMLElementName.TITLE);
    final String title = titleElement == null
        ? null
        : CharacterReference.decodeCollapseWhiteSpace(
            titleElement.getContent());
    
    return new Document(title, content);
  }

}
