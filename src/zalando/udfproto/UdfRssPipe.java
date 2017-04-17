package zalando.udfproto;

import java.io.StringReader;
import java.net.URI;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.cyberneko.html.parsers.DOMParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.rometools.rome.feed.synd.SyndEntry;

/**
 * @author Carsten
 *
 * Die RssPipe dient dazu für eine übergebene URL zu prüfen,
 * ob der Blogeintrag im RSS Feed gefunden werden kann und
 * gibt den Inhalt dafür zurück.
 *
 */
public class UdfRssPipe {
	
	private String url;
	private boolean isBlogger;
	
	public UdfRssPipe(String url, boolean isBlogger) {
		super();
		this.url = url;
		this.isBlogger = isBlogger;
		if (isBlogger)
			System.err.println("Rss Blogger Pipe active");
		else
			System.err.println("Rss Pipe active");
	}
	
	/**
	 * Process verarbeitet die Daten die sie vom RssChecker erhält.
	 * Für die Auswahl des Inhalts gilt folgende Regel:
	 * Falls content:encoded im item vorhanden ist, wird dies genommen,
	 * wenn nicht dann wird die description gewählt.
	 * 
	 * @return JSONObject JSONObject welches extrahierten Text und Titel, die Daten aus dem Goldstandard sowie die Vergleichswerte enthält.
	 */
	public JSONObject process() throws Exception
	{	
		try {
			UdfRssChecker checker = new UdfRssChecker(new URI(this.url), this.isBlogger);
			SyndEntry content = checker.getContent();
			
			DOMParser parser = new DOMParser();
			String str = null;
			if (content.getContents().size() > 0) {
				str = content.getContents().get(0).getValue();
			} else if (content.getDescription() != null) {
				str = content.getDescription().getValue();
			}
			InputSource is = new InputSource(new StringReader(str));
			parser.parse(is);
			Node doc = parser.getDocument();
			String docText = doc.getFirstChild().getTextContent();
			String titlePipe = content.getTitle();
			if (titlePipe == null) {
				titlePipe = "";
			}
			JSONObject obj = new JSONObject();
			if(doc != null){
				//toDO Collect more Meta Informations
				//like author, url, domain, date, img-alt-tag think about more
				//
											
				obj.put("url", this.url);
				obj.put("extracted_text", docText);
				obj.put("extracted_title", titlePipe);
				
				UdfImageParser ip = new UdfImageParser(doc);
				JSONArray ipArray = ip.result();
				if (ipArray != null) {
					obj.put("extracted_alttags", ipArray);
				}
			}
			
			return obj;
			
		}
		catch (Exception e) 
		{	
			throw new Exception(e);
		}
	}
}
