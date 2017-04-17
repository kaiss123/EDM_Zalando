package zalando.classifier.sourcer;

import java.io.File;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.Iterator;

import org.json.simple.JSONObject;
import zalando.classifier.main.MyBlockingQueue;

public class SourceOutput implements Runnable {
	public String name = "";
	public MyBlockingQueue inputQueue;
	public MyBlockingQueue outputQueue;
	private Hashtable<String, FileWriter> outputDictionary;
	public String startPath = "files/output/";

	public SourceOutput(String name, MyBlockingQueue outputQueue, MyBlockingQueue inputQueue) {
		super();
		// TODO Auto-generated constructor stub
		this.name = name;
		this.outputQueue = outputQueue;
		this.inputQueue = inputQueue;
		this.outputDictionary = new Hashtable<>();
	}

	@Override
	public void run() {
		int counter = 0;
		System.out.println(this.name + " started... waiting for Input queue to finish");
		try {

			synchronized (this.inputQueue) {
				this.inputQueue.wait();
			}
			
			while (!outputQueue.isEmpty()) {
				System.out.println(this.name + "writing to file. " + outputQueue.size() + " left...");
				JSONObject obj = outputQueue.take();
				this.writeObjectToFile(obj);

			}
			for (Iterator iterator = this.outputDictionary.values().iterator(); iterator.hasNext();) {
				FileWriter writer = (FileWriter) iterator.next();
				writer.write("]");
				writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void writeObjectToFile(JSONObject object)
	{
		String selector = "default";
		if (object.containsKey("selector")) {
			selector = object.get("selector").toString();
			object.remove("selector");
		}
		
		try {
			
			FileWriter writer = this.outputDictionary.get(selector);
			if (writer == null) {
				String directory = startPath + selector;
				new File(directory).mkdirs();
				writer = new FileWriter(directory + "/result.json");
				writer.write("[");
				this.outputDictionary.put(selector, writer);
			} else {
				writer.write(",");
				writer.write(System.getProperty("line.separator"));
			}
			writer.write(object.toJSONString());
			writer.write(System.getProperty("line.separator"));
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
