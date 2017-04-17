package zalando.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import zalando.classifier.main.Classifier;
import zalando.classifier.main.MyBlockingQueue;
import zalando.classifier.sourcer.SourceInput;
import zalando.classifier.sourcer.SourceOutput;

public class Start {

	public static HashMap<String, JSONObject> gold;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String everything = null;
		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("files/gold/final.json")), "UTF8"))) {
//		try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("files/gold/final_rss.json")), "UTF8"))) {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) 
		    {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    everything = sb.toString();
		}
		catch (Exception e) {
			System.out.print("error: " + e.getMessage());
		}
		
		if (everything != null) 
		{
			createGoldFromString(everything);
			
			
			MyBlockingQueue inputQ = new MyBlockingQueue("BQInput", 300);
			MyBlockingQueue outputQ = new MyBlockingQueue("BQOutput", 300);
			Thread SourceInputThread = new Thread(new SourceInput("SourceInT1", inputQ));
			Thread SourceOutputThread = new Thread(new SourceOutput("SourceOutT1", outputQ, inputQ));
			
			Thread ClassifierThread1 = new Thread(new Classifier("ClassifierT1", inputQ, outputQ));
			Thread ClassifierThread2 = new Thread(new Classifier("ClassifierT2", inputQ, outputQ));
			Thread ClassifierThread3 = new Thread(new Classifier("ClassifierT3", inputQ, outputQ));
			Thread ClassifierThread4 = new Thread(new Classifier("ClassifierT4", inputQ, outputQ));
			Thread ClassifierThread5 = new Thread(new Classifier("ClassifierT5", inputQ, outputQ));
			Thread ClassifierThread6 = new Thread(new Classifier("ClassifierT6", inputQ, outputQ));
			Thread ClassifierThread7 = new Thread(new Classifier("ClassifierT7", inputQ, outputQ));
			Thread ClassifierThread8 = new Thread(new Classifier("ClassifierT8", inputQ, outputQ));
			Thread ClassifierThread9 = new Thread(new Classifier("ClassifierT9", inputQ, outputQ));
			Thread ClassifierThread10 = new Thread(new Classifier("ClassifierT10", inputQ, outputQ));
			
			ClassifierThread1.start();
//			ClassifierThread2.start();
//			ClassifierThread3.start();
//			ClassifierThread4.start();
//			ClassifierThread5.start();
//			ClassifierThread6.start();
//			ClassifierThread7.start();
//			ClassifierThread8.start();
//			ClassifierThread9.start();
//			ClassifierThread10.start();
			SourceInputThread.start();
			
			SourceOutputThread.start();
		}

	}
	
	private static void createGoldFromString(String gold)
	{
		JSONArray array = (JSONArray)JSONValue.parse(gold);
		HashMap<String, JSONObject> goldCopy = new HashMap<>();
		for (Object object : array) 
		{
			JSONObject obj = (JSONObject)object;
			goldCopy.put(obj.get("source").toString(), (JSONObject)obj.get("gold"));
			
		}
		Start.gold = goldCopy;
	}
	
}
