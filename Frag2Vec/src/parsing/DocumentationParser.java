package parsing;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class DocumentationParser {
	
	public static String normalize(String doc) {
		doc = doc.replace("<link>", "<code>");
		doc = doc.replace("</link>", "</code>");
		Document html = Jsoup.parse(doc);
		Element body = html.getElementsByTag("body").first();
		StringBuilder sb = new StringBuilder();
		normalize(body, sb);
		doc = sb.toString();
		doc = doc.replace(" &lt; ", " is less than ");
		doc = doc.replace(" &gt; ", " is greater than ");
		doc = doc.replace(" &lt;= ", " is less than or equal to ");
		doc = doc.replace(" &gt;= ", " is greater than or equal to ");
		
		return doc;
	}

	private static void normalize(Node node, StringBuilder sb) {
		if (node instanceof TextNode) {
			TextNode text = (TextNode) node;
			sb.append(text.text());
		} else if (node instanceof Element) {
			for (int i = 0; i < node.childNodeSize(); i++) {
				Node child = node.childNodes().get(i);
				StringBuilder csb = new StringBuilder();
				normalize(child, csb);
				sb.append(csb.toString());
			}
		}
	}
}
