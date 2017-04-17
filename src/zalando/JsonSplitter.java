package zalando;

import java.io.FileNotFoundException;
import java.io.IOException;

public class JsonSplitter {

	public static void main(String[] args) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		Jsplitter2 splitter = new Jsplitter2();
		splitter.readFile();
//    	TextFileSorter s = new TextFileSorter();
//    	s.sort(new FileInputStream("files/raw_html.json"), new FileOutputStream("files/tmp/jsonFileName.json"));
	}

}
