package zalando.classifier.pipes;

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

import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import zalando.classifier.Start;
import zalando.classifier.main.ImageParser;
import zalando.classifier.main.RssChecker;
import zalando.classifier.main.SimilarityUtil;

/**
 * @author Carsten
 *
 * Die RssPipe dient dazu für eine übergebene URL zu prüfen,
 * ob der Blogeintrag im RSS Feed gefunden werden kann und
 * gibt den Inhalt dafür zurück.
 *
 */
public class RssPipe {
	
	private String url;
	private boolean isBlogger;
	
	public RssPipe(String url, boolean isBlogger) {
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
	public JSONObject process()
	{	
		try {
			RssChecker checker = new RssChecker(new URI(this.url), this.isBlogger);
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
			
			//Ab hier beginnt der Vergleich mit dem Goldstandard,
			//um hier weiter debuggen zu können, müssen aktuelle Daten im RSS-Goldstandard vorhanden sein, 
			//die noch im RSS Feed des jeweiligen Blogs sind. 
			JSONObject goldObj = Start.gold.get(this.url);
			if (goldObj == null) 
			{
				return null;
			}
			String titleGold = goldObj.get("title").toString();
			String text = goldObj.get("text").toString();
			if (titleGold == null) {
				titleGold = "";
			}
			String titlePipe = content.getTitle();
			if (titlePipe == null) {
				titlePipe = "";
			}
			JSONObject obj = new JSONObject();
			NormalizedLevenshtein nls = new NormalizedLevenshtein();
			double lev = nls.distance(StringUtils.deleteWhitespace(titlePipe), StringUtils.deleteWhitespace(titleGold));
			String levFine = String.format("%.2f", lev);
			if(doc != null){
				docText = StringEscapeUtils.unescapeJava(docText);
				double cosine = SimilarityUtil.consineTextSimilarity(StringUtils.split(docText), StringUtils.split(goldObj.get("text").toString()));
				String cosineFine = String.format("%.2f", cosine);
				//COMPARING END
				
				//toDO Collect more Meta Informations
				//like author, url, domain, date, img-alt-tag think about more
				//
				JSONObject pipeObj = new JSONObject();
				pipeObj.put("title", titlePipe);
				pipeObj.put("text", docText);
							
				JSONObject simObj = new JSONObject();
				simObj.put("title", levFine);
				simObj.put("text", cosineFine);
				
				obj.put("source", this.url);
				obj.put("pipe", pipeObj);
				obj.put("gold", goldObj);
				obj.put("similarity", simObj);
				
				ImageParser ip = new ImageParser(doc);
				JSONArray ipArray = ip.result();
				if (ipArray != null) {
					obj.put("images_alt_tags", ipArray);
				}
			}
			
			return obj;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("failed for: " + this.url);
		}
		return null;
	}
}
