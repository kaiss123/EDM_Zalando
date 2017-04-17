package zalando.classifier.main;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

public class MyBlockingQueue extends ArrayBlockingQueue<JSONObject> {
	public String name = "";
	public MyBlockingQueue(String name, int capacity) {
		super(capacity);
		// TODO Auto-generated constructor stub
		this.name = name;
	}

	private static final long serialVersionUID = 1L;
	Logger log = Logger.getLogger(MyBlockingQueue.class.getName());
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return super.size();
	}

	@Override
	public void put(JSONObject e) throws InterruptedException {
		// TODO Auto-generated method stub
		super.put(e);
		System.out.println(this.name + ": added one Item. Size now: " + this.size());
	}

	@Override
	public JSONObject take() throws InterruptedException {
		// TODO Auto-generated method stub
		return super.take();
	}

	@Override
	public JSONObject poll() {
		// TODO Auto-generated method stub
		return super.poll();
	}

	@Override
	public JSONObject peek() {
		// TODO Auto-generated method stub
		return super.peek();
	}
	

}
