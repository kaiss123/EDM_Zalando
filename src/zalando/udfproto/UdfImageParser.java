package zalando.udfproto;

import java.io.StringReader;

import org.apache.xerces.dom.AttrNSImpl;
import org.apache.xerces.dom.TextImpl;
import org.cyberneko.html.parsers.DOMParser;
import org.json.simple.JSONArray;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Der ImageParser dient dazu ein JSONArray mit allen Alt-Tags zu erstellen.
 * Falls kein Node vorhanden ist, z.B. bei BPPipe so wird ein eins erstellt.
 * 
 * @author Carsten
 *
 */
public class UdfImageParser {
	
	private Node node;
	private String html;
	private String startOfDoc;
	private JSONArray imagesArray;
	private boolean processed, textProcessed;
	
	/**
	 * Konstruktor für bereits existierende Node-Objekte
	 * 
	 * @param node der Node aus dem die Alt-Tags extrahiert werden.
	 */
	public UdfImageParser(Node node)
	{
		super();
		this.node = node;
		this.imagesArray = new JSONArray();
		this.processed = false;
		this.textProcessed = false;
	}
	
	
	/**
	 * Konstruktor zur internen Erzeugung eines Nodes. Falls das HTML anders als bisher verarbeitet wird,
	 * muss zusätzlich noch der Anfang des extrahierten Texts übergeben werden.
	 * 
	 * Bei der Verwendung wurden bis jetzt die ersten beiden Wörter übergeben.
	 * Ein Resultat kann aber nur erzeugt werden, wenn diese beiden Wörter in einem Tag vorhanden sind.
	 * Falls startOfDoc über mehrere Tags verteilt ist, wird es zu keinem Ergebnis führen.
	 * Da die Resultate beim testen überwiegend aussagelos waren, wurden keine weiteren Möglichkeiten
	 * untersucht.
	 * 
	 * @param html der HTML der nach startOfDoc untersucht wird
	 * @param startOfDoc der zu suchende Start des extrahierten Texts
	 */
	public UdfImageParser(String html, String startOfDoc)
	{
		super();
		this.html = html;
		this.startOfDoc = startOfDoc;
		this.imagesArray = new JSONArray();
		this.processed = false;
		this.textProcessed = false;
	}
	
	/**
	 * Um nach allen Bildern zu suchen, muss zuerst ein Node erstellt werden.
	 * Dieser wird anschließend gefiltert und an den eigentlichen Processor weitergegeben.
	 */
	private void processText() throws Exception
	{
		DOMParser parser = new DOMParser();
		InputSource is = new InputSource(new StringReader(this.html));
		try {
			parser.parse(is);
			Node doc = parser.getDocument();
			doc = findNode(doc);
			process(doc);
		}
		catch (Exception e) 
		{	
			throw new Exception(e);
		}
	}
	
	/**
	 * Damit man den richtigen Node findet, wird der komplette Node durchsucht und versucht
	 * mit dem ersten zwei Wörtern des extrahierten Texts zu matchen. Wenn man über dem ganzen
	 * HTML die Alt-Tags extrahiert, enthält man viele unnötige Informationen. 
	 * 
	 * @param node der zu durchsuchende Node
	 * @return der gefundene Node oder null
	 */
	private Node findNode(Node node)
	{
		if (node.getNodeValue() != null) {
			if (node.getNodeValue().toLowerCase().contains(startOfDoc.toLowerCase())) {
				//bis jetzt waren der node value immer eine Instanz von TextImpl, weshalb der Parentnode
				//das umschließende Tag ist und deshalb der ParentNode vom ParentNode das richtige Ergebnis ist
				if (node instanceof TextImpl) 
					return node.getParentNode().getParentNode();
				else
					return node.getParentNode();
			}
		}
		Node child = node.getFirstChild();
		
        while (child != null) {
            Node noi = findNode(child);
            if (noi != null) {
				return noi;
			}
            child = child.getNextSibling();
        }
		return null;
	}
	
	
	/**
	 * Der Processor iteriert rekursiv über den Node und extrahiert Alt-Tags aus allen IMG-Tags
	 * und speichert sie in einem JSONArray.
	 * 
	 * @param node der zu durchsuchende Node
	 */
	private void process(Node node)
	{
		if (node == null) {
			return;
		}
		if (node.getNodeName() != null && node.getNodeName().equalsIgnoreCase("img")) {
			NamedNodeMap atts = node.getAttributes();
			for (int i = 0; i < atts.getLength(); i++) {
				Object att = atts.item(i);
				if (att instanceof AttrNSImpl) 
				{
					AttrNSImpl realAtt = (AttrNSImpl)att;
					//leere Alttags werden ignoriert
					if (realAtt.getName().equalsIgnoreCase("alt") && realAtt.getValue() != "") 
					{
						imagesArray.add(realAtt.getValue());
					}
				}
			}
		}
		processed = true;
        Node child = node.getFirstChild();
        while (child != null) {
            process(child);
            child = child.getNextSibling();
        }
	}
	
	
	/**
	 * Das Resultat für die Suche, falls schon der Node vorhanden war.
	 * 
	 * @return das JSONArray mit den Alt-Tags oder null
	 */
	public JSONArray result()
	{
		if (!processed) {
			this.process(this.node);
		}
		if (imagesArray.size() == 0) {
			return null;
		}
		return imagesArray;
	}
	
	/**
	 * Das Resultat für die Suche, falls schon der Node erzeugt werden muss.
	 * 
	 * @return das JSONArray mit den Alt-Tags oder null
	 */
	public JSONArray resultFromTextDoc() throws Exception
	{
		if (!textProcessed) {
			this.processText();
		}
		if (imagesArray.size() == 0) {
			return null;
		}
		return imagesArray;
	}
}
