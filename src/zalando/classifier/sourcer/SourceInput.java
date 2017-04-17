package zalando.classifier.sourcer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import zalando.classifier.main.MyBlockingQueue;

public class SourceInput implements Runnable{

	private MyBlockingQueue inputQueue;
	public String startPath = "files/";
	public String name = "";
	public SourceInput(String name, MyBlockingQueue inputQueue) {
		super();
		// TODO Auto-generated constructor stub
		this.name = name;
		this.inputQueue = inputQueue;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println(this.name + " started...");
		try {
			listDir(startPath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(this.getClass().getSimpleName() + ": Trying to put Json in Queue");
	}
	public void read(String filePath) throws Exception{
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line = br.readLine();

			while (line != null) {
				if (line.length() > 5) {//to skip ,][{} and stuff
					JSONObject obj = (JSONObject) JSONValue.parse(line);
					
					inputQueue.put(obj);
					
				}
				line = br.readLine();
			}
		} catch (Exception e) {
			System.out.print("error: " + e.getMessage());
		}
	}

	public void listDir(String path) throws Exception{
		File dir = new File(path);
		File[] files = dir.listFiles();
		if (files != null) { // Erforderliche Berechtigungen etc. sind vorhanden
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					System.out.print("Directory found skipping");
				} else {
//					System.out.println(this.name + ": Found new File! Opening... " + files[i].getPath());
//					//System.err.println("Peek Queue " + this.inputQueue.peek());
//					read(files[i].getPath());
					if (!files[i].getName().contains("rss")) {
						System.out.println(this.name + ": Found new File! Opening... " + files[i].getPath());
						//System.err.println("Peek Queue " + this.inputQueue.peek());
						read(files[i].getPath());
					}
				}
			}
		}
	}


}
