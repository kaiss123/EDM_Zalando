package zalando.classifier.pipes;

import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
import zalando.classifier.Start;
import zalando.classifier.main.ImageParser;
import zalando.classifier.main.SimilarityUtil;

import java.io.StringReader;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class BPPipe {
	
	private String url;
	private String html;
	
	public BPPipe(String...strings) {
		super();
		// TODO Auto-generated constructor stub
		System.err.println("Default Pipe active");
		this.url = strings[0];
		this.html = strings[1];
	}

	public JSONObject process() {
		// TODO Auto-generated method stub
			
		try 
		{
			InputSource input = new InputSource(new StringReader(this.html));
			BoilerpipeSAXInput is = new BoilerpipeSAXInput(input);
			TextDocument doc = is.getTextDocument();
			ArticleExtractor ex = new ArticleExtractor();
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
			String titlePipe = doc.getTitle();
			if (titlePipe == null) {
				titlePipe = "";
			}
			
				JSONObject obj = new JSONObject();
				NormalizedLevenshtein nls = new NormalizedLevenshtein();
				double lev = nls.distance(StringUtils.deleteWhitespace(titlePipe), StringUtils.deleteWhitespace(titleGold));
				if (titleGold == "" || titlePipe == "") {
					lev = 0.0;
				}
			String levFine = String.format("%.2f", lev);

			String docText = ex.getText(doc);
				double cosine = SimilarityUtil.consineTextSimilarity(StringUtils.split(docText), StringUtils.split(goldObj.get("text").toString()));
				String cosineFine = String.format("%.2f", cosine);

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
				String startOfDoc = docText.split(" ")[0] + " " + docText.split(" ")[1]; 
				ImageParser ip = new ImageParser(this.html, startOfDoc);
				JSONArray ipArray = ip.resultFromTextDoc();
				if (ipArray != null) {
					obj.put("images_alt_tags", ipArray);
				}
				
				return obj;
		} 
		catch (BoilerpipeProcessingException e) 
		{	
			System.out.println("boilerpipe error:" + e.getMessage());
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			System.out.println("sax error:" + e.getMessage());
		}
		return null;
		
	}
}