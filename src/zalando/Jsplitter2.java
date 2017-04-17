package zalando;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


public class Jsplitter2 {
	public Pattern p = Pattern.compile("[^(http|https)://][a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*");//example http://lefashionimage.blogspot.com
	public int fileSize = 10;
	public int fileCounter = 1;
	public void readFile(){
		
		try(BufferedReader br = new BufferedReader(new FileReader("files/raw_html.json"))) {
		    String line = br.readLine();

			fileWriter = new FileWriter("files/tmp/part"+ fileCounter +".json");
			fileWriter.write("[");
			fileWriter.write(System.getProperty( "line.separator" ));
		    while (line != null) {
		    	JSONObject obj = (JSONObject)JSONValue.parse(line);
		    	String urlFromRaw = obj.get("url").toString();
			    if(fileSize==0){
			    	fileWriter.write(System.getProperty( "line.separator" ));
			    	fileWriter.write("]");
			    	fileWriter.close();
					fileWriter = new FileWriter("files/tmp/part"+ ++fileCounter +".json");
					fileWriter.write("[");
					fileWriter.write(System.getProperty( "line.separator" ));
			    }
		    	
		    	splitter(obj, urlFromRaw,fileWriter);
		        line = br.readLine();
		    }
		}
		catch (Exception e) {
			System.out.print("error: " + e.getMessage());
		}
	}
	public String jsonFileName = "";
	public JSONObject jsonFile;
	public JSONArray jsonList;
	public int counter = 0;
	public boolean isNewURL = false;
	int c = 0;
	FileWriter fileWriter = null;
	BufferedWriter bufferedWriter = null;
	public void splitter(JSONObject obj, String urlFromRaw, FileWriter bufw) throws IOException{
		
		if (fileSize>0) {

			//System.out.println("Writing " + obj.toJSONString());
			bufw.write(obj.toJSONString());
			bufw.write(System.getProperty( "line.separator" ));
			if(fileSize>1)
			bufw.write(",");
			bufw.write(System.getProperty( "line.separator" ));
			fileSize--;
		}else{
			fileSize = 10;
		}
	}
}
