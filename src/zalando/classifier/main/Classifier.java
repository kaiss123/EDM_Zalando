package zalando.classifier.main;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;

import zalando.classifier.pipes.BPPipe;
import zalando.classifier.pipes.BloggerPipe;
import zalando.classifier.pipes.ManualWordpressPipe;
import zalando.classifier.pipes.RssPipe;

public class Classifier implements Runnable{
	private MyBlockingQueue inputQueue;
	private MyBlockingQueue outputQueue;
	private Identificator identificator = new Identificator();
	//private SourceInput input = new SourceInput();
	public int counter = 0;
	public String name = "";
	public Classifier(String name, MyBlockingQueue inputQueue, MyBlockingQueue outputQueue) {
		super();
		this.name = name;
		this.inputQueue = inputQueue;
		this.outputQueue = outputQueue;

	}

	public void init(){

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println(this.name + " started...");
		init();
		try {
			process();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public ArrayList<String> manualWPPipeCounter = new ArrayList<String>();
	public ArrayList<String> bloggerPipeCounter = new ArrayList<String>();
	public ArrayList<String> defaultPipeCounter = new ArrayList<String>();
	public ArrayList<String> rssPipeCounter = new ArrayList<String>();
	public ArrayList<String> rssBloggerPipeCounter = new ArrayList<String>();

	public void process() throws InterruptedException{
		for(;;){
			JSONObject obj = inputQueue.poll(5, TimeUnit.SECONDS);
			if (obj == null) 
			{
				synchronized (this.inputQueue) {
					this.inputQueue.notify();
				}
				break;
			}
			System.err.println(this.name + ": Taking Item form Q for processing");

			String urlFromRaw = obj.get("url").toString();
			String htmlFromRaw = obj.get("html").toString();	
			if (htmlFromRaw.isEmpty()) {
				HttpURLConnection httpcon;
				try {
					httpcon = (HttpURLConnection) new URL(urlFromRaw).openConnection();
					httpcon.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
					htmlFromRaw = IOUtils.toString(httpcon.getInputStream());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			String selector = identificator.evaluate(urlFromRaw, htmlFromRaw);

			switch (selector) {
			case "manual_wordpress":
			{
				ManualWordpressPipe mwp = new ManualWordpressPipe(urlFromRaw, htmlFromRaw);
				JSONObject result = mwp.process();
				
				if(result != null)
				{
					result.put("selector", selector);
					outputQueue.put(result);
					manualWPPipeCounter.add(urlFromRaw);
				}
				else {
					this.defaultPipeProcess(urlFromRaw, htmlFromRaw);
				}
				break;
			}
			case "blogger":
			{
				BloggerPipe blogger_pipe = new BloggerPipe(urlFromRaw, htmlFromRaw);
				JSONObject result = blogger_pipe.process();
				
				if(result != null){
					result.put("selector", selector);
					outputQueue.put(result);
					bloggerPipeCounter.add(urlFromRaw);
				}
				else {
					this.defaultPipeProcess(urlFromRaw, htmlFromRaw);
				}
				break;
			}
			case "rssBlogger":
			case "rss":
			{
				boolean isBlogger = !selector.equalsIgnoreCase("rss");
				RssPipe rp = new RssPipe(urlFromRaw, isBlogger);
				JSONObject result = rp.process();

				if (result != null) {
					result.put("selector", selector);
					outputQueue.put(result);
					if (isBlogger) {
						rssBloggerPipeCounter.add(urlFromRaw);
					} else {
						rssPipeCounter.add(urlFromRaw);
					}
				}
				else {
					this.defaultPipeProcess(urlFromRaw, htmlFromRaw);
				}
				break;
			}
			default:
			{
				//anstatt in der pipe jedes JSONObj zu schreiben, geben wir es in den Classifier
				//zurueck, damit der das schreibt, weil er asyncron alle processed Objs kriegen soll
				//er schreibt es in die OutputQueue. 
				this.defaultPipeProcess(urlFromRaw, htmlFromRaw);
				break;
			}
			} 

		}
		System.out.println("Default: " +defaultPipeCounter.size());
		System.out.println("Wordpress: " +manualWPPipeCounter.size());
		System.out.println("Blogger: " +bloggerPipeCounter.size());
		System.out.println("RSS: " +rssPipeCounter.size());
		System.out.println("RSS Blogger: " +rssBloggerPipeCounter.size());
	}

	private void defaultPipeProcess(String urlFromRaw, String htmlFromRaw)
	{
		try {
			BPPipe bp_pipe = new BPPipe(urlFromRaw, htmlFromRaw);
			JSONObject result = bp_pipe.process();
			
			if(result != null){
				result.put("selector", "default");
				this.outputQueue.put(result);	
				defaultPipeCounter.add(urlFromRaw);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
