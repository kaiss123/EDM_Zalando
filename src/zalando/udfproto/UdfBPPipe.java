package zalando.udfproto;

import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;

import java.io.StringReader;
import org.json.simple.*;
import org.xml.sax.InputSource;

public class UdfBPPipe {
	
	private String url;
	private String html;
	
	public UdfBPPipe(String...strings) {
		super();
		// TODO Auto-generated constructor stub
		System.err.println("Default Pipe active");
		this.url = strings[0];
		this.html = strings[1];
	}

	public JSONObject process() throws Exception {
		// TODO Auto-generated method stub

		try 
		{
			InputSource input = new InputSource(new StringReader(this.html));
			BoilerpipeSAXInput is = new BoilerpipeSAXInput(input);
			TextDocument doc = is.getTextDocument();
			ArticleExtractor ex = new ArticleExtractor();
			String titlePipe = doc.getTitle();
			if (titlePipe == null) {
				titlePipe = "";
			}			
			JSONObject obj = new JSONObject();

			String docText = ex.getText(doc);

			obj.put("url", this.url);
			obj.put("extracted_text", docText);
			obj.put("extracted_title", titlePipe);

			String startOfDoc = docText.split(" ")[0] + " " + docText.split(" ")[1]; 
			UdfImageParser ip = new UdfImageParser(this.html, startOfDoc);
			JSONArray ipArray = ip.resultFromTextDoc();
			if (ipArray != null) {
				obj.put("extracted_alttags", ipArray);
			}

			return obj;
		} 
		catch (Exception e) 
		{	
			throw new Exception(e);
		}
	}
}