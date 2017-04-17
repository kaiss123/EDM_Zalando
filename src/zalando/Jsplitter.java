package zalando;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Jsplitter {

	//https://regex101.com/
	public Pattern p = Pattern.compile("[^(http|https)://][a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*");//example http://lefashionimage.blogspot.com
	public void readFile(){
		try(BufferedReader br = new BufferedReader(new FileReader("files/raw_html.json"))) {
		    String line = br.readLine();
			
		    while (line != null) {
		    	JSONObject obj = (JSONObject)JSONValue.parse(line);
		    	String urlFromRaw = obj.get("url").toString();


		    	
		    	//System.out.println(urlFromRaw.toString());
		    	splitter(obj, urlFromRaw);
		        line = br.readLine();
		    }
		}
		catch (Exception e) {
			System.out.print("error: " + e.getMessage());
		}
	}
	public String jsonFileName = "www.alealimay.com123";
	public JSONObject jsonFile;
	public JSONArray jsonList;
	public int counter = 0;
	public boolean isNewURL = false;
	int c = 0;
	public void splitter(JSONObject obj, String urlFromRaw){
		
    	Matcher m = p.matcher(urlFromRaw);
    	//look if we found pattern in actual item
    	if (m.find()) {
    		String baseUrlFromRaw = m.group(0);


    		if(jsonFileName.equals(baseUrlFromRaw)){
    			//write in actual json file
    			//if(jsonList != null)
    			//jsonList.add(obj);
    			jsonFile.put(++c, obj);
    			System.err.println(counter + " " + jsonFile.size());
    			isNewURL = true;
    			//System.err.println("same");
    		}else{
    			//flush file and start new one
    			if(isNewURL){
    				isNewURL = false;
        			FileWriter fileWriter = null;
        			BufferedWriter bufferedWriter = null;
        			try {
        				
        				fileWriter = new FileWriter("files/tmp/"+ jsonFileName +".json");
        				bufferedWriter = new BufferedWriter(fileWriter);
        				System.out.println("Writing " + jsonFileName);
        				bufferedWriter.write(jsonFile.toJSONString());


        			} catch (IOException e) {
        				e.printStackTrace();
        			}finally {
        				if (bufferedWriter != null && fileWriter != null) {
    	    				try {
    	    					bufferedWriter.close();
    	    					fileWriter.close();
    	    				} catch (IOException e) {
    	    					e.printStackTrace();
    	    				}
        				}
        			}
    			}
    			jsonFileName = baseUrlFromRaw;
    			jsonFile = new JSONObject();
    			jsonFile.put(++c, obj);
    			//jsonList.add(obj);
    			//jsonList.trimToSize();
    			counter++;
    			obj = null;
    		}
    	   // System.out.println("extracted: " + baseUrlFromRaw);
    	}
	}
}
