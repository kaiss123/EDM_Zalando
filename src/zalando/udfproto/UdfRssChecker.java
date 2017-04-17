package zalando.udfproto;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.dom.AttrNSImpl;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;


/**
 * Der RssChecker prüft anhand einer übergebenen URL ob der Blogeintrag noch im RSS-Feed
 * vorhanden ist und lädt den entspechenden Eintrag aus dem Feed.
 * 
 * @author Carsten
 *
 */
public class UdfRssChecker {
	
	private URI postUri;
	private URI feedUri;
	private boolean isBlogger;
	private boolean feedAvailable;
	private boolean checkedForAvailable;
	private SyndEntry feedContent;
	
	public UdfRssChecker(URI postUri, boolean isBlogger) {
		super();
		this.postUri = postUri;
		this.feedUri = null;
		this.isBlogger = isBlogger;
		this.checkedForAvailable = false;
		this.feedAvailable = false;
		this.feedContent = null;
	}
	
	/**
	 * Dient der Überprüfung, ob unter der URL ein RSS-Feed gefunden werden kann
	 * und falls ja, ob der Blogeintrag im Feed vorhanden ist. Wenn er vorhanden ist,
	 * wird außerdem das Item zur Verfügung gestellt.
	 * 
	 * @return true wenn der Blogeintrag im Feed vorhanden ist, sonst false
	 */
	public boolean rssFeedAvailable() throws Exception
	{
		if (!this.checkedForAvailable) {
			this.findFeedUri();
			if (this.feedUri == null) {
				this.feedAvailable = false;
			}
			
			try {
				
				SyndFeedInput input = new SyndFeedInput();
				SyndFeed feed = input.build(new XmlReader(this.feedUri.toURL()));
				for (Iterator<SyndEntry> iterator = feed.getEntries().iterator(); iterator.hasNext();) {
					SyndEntry item = iterator.next();
					if (new URI(item.getLink()).equals(this.postUri)) {
						String content = null;
						//Falls das item sowohl "description" als auch "contents" enthält,
						//sollte "contents" gewählt werden, da dort meist der komplette html code des Eintrags
						//enthalten ist.
						if (item.getContents().size() > 0) {
							content = item.getContents().get(0).getValue();
						} else if (item.getDescription() != null) {
							content = item.getDescription().getValue();
						}
						else
						{
							this.feedAvailable = false;
							break;
						}
						
						//Manche Feeds liefern den Inhalt nur in gekürzter Form aus,
						//weshalb nach Möglichkeiten gesucht werden sollte,
						//die trotzdem den vollen Feed erzeugen können
						
//						if (content.contains(this.postUri.toString()) ||
//							content.contains("&#8230;") ||
//							content.contains("&hellip;") ||
//							content.equalsIgnoreCase("")) {
//							this.feedAvailable = false;
//							break;
//						}
						this.feedAvailable = true;
						this.feedContent = item;
						break;
					}
				}
			}
			catch (Exception e) 
			{	
				throw new Exception(e);
			}
			this.checkedForAvailable = true;
		}
		
		return this.feedAvailable;
	}
	
	public SyndEntry getContent() throws Exception
	{
		if (!this.checkedForAvailable) {
			this.rssFeedAvailable();
		}
		
		return this.feedContent;
	}
	
	/**
	 * Diese methode ermittelt die RSS-Feed URI, indem der Link zerlegt wird,
	 * die Landingpage des Blogs geladen wird und anschließend das Rss-Tag gesucht wird.
	 */
	private void findFeedUri() throws Exception
	{
		if (!this.checkedForAvailable) {
			URI uri = null;
			
			try {
				URI hostUrl;
				//falls der link kein Scheme enthält, wird http verwendet
				if (this.postUri.getScheme() == null) {
					hostUrl = new URI("http://" + this.postUri.getHost());
				}
				else {
					hostUrl = new URI(this.postUri.getScheme() + "://" + this.postUri.getHost());
				}
				DOMParser parser = new DOMParser();
				
				HttpURLConnection httpcon = (HttpURLConnection) hostUrl.toURL().openConnection();
			    //Manche Blogs liefern kein Ergebnis, wenn kein User-Agent gesetzt ist.
				httpcon.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
			    String html = IOUtils.toString(httpcon.getInputStream());
				InputSource is = new InputSource(new StringReader(html));
				parser.parse(is);
				Node doc = parser.getDocument();
				Node noi = findRSSNode(doc, isBlogger);
				if (noi != null) {
					NamedNodeMap atts = noi.getAttributes();
					if (atts != null) {
						URI feedUri = new URI(atts.getNamedItem("href").getNodeValue().toString());
						if (feedUri.getScheme() == null) {
							feedUri = new URI("http", feedUri.getHost(), feedUri.getPath(), feedUri.getFragment());
						}
						uri = feedUri;
					}
				}
			}
			catch (Exception e) 
			{	
				throw new Exception(e);
			}
			
			this.feedUri = uri;
		}
	}
	
	
	/**
	 * Diese Methode durchsucht den HTML-Node nach link-Tags und versucht den Rss-Alternate-Lionk zu finden.
	 * Blogger besitzt einen speziellen Rss-Link, weshalb nach Blogger und nicht Blogger unterschieden wird.
	 * 
	 * @param node der zu durchsuchende Node
	 * @param isBlogger flag ob der Blog das CMS Blogger nutzt, sollte vom Classifier gesetzt werden
	 * @return den gefunden Node oder null
	 */
	private Node findRSSNode(Node node, boolean isBlogger)
	{
		Node child = node.getFirstChild();
		String relValue = "alternate";
		String typeValue = "application/rss+xml";
		if (isBlogger) {
			relValue = "service.post";
			typeValue = "application/atom+xml";
		}
        while (child != null) {
        	
        	//da wir nach dem RSS suchen sind lediglich link-Tags von relevanz
        	if(child.getNodeName().equalsIgnoreCase("link")) {
        		NamedNodeMap atts = child.getAttributes();
        		if (atts != null) {
        			boolean isAlternate = false;
        			boolean isRSS = false;
        			boolean isWrong = false;
        			String rssLink = null;
        			
        			for (int i = 0; i < atts.getLength(); i++) {
        				Object att = atts.item(i);
        				if (att instanceof AttrNSImpl) {
        					AttrNSImpl realAtt = (AttrNSImpl)att;
        					if (realAtt.getName().equalsIgnoreCase("rel") && 
        						realAtt.getValue().equalsIgnoreCase(relValue)) {
								isAlternate = true;
								continue;
							}
        					if (realAtt.getName().equalsIgnoreCase("type") && 
            					realAtt.getValue().equalsIgnoreCase(typeValue)) {
    								isRSS = true;
    								continue;
    						}
        					//Viele Blogs liefern auch Feeds zu den Kommentaren,
        					//dieser muss abgefangen werden
        					if (realAtt.getName().equalsIgnoreCase("title") && 
                				realAtt.getValue().toLowerCase().contains("comment")) {
        							isWrong = true;
        							continue;
        					}
        				}
        			}
        			if (isRSS && isAlternate && !isWrong) {
						return child;
					}
				}
        	}
           	Node noi = null;
        	noi = findRSSNode(child, isBlogger);
        	if (noi != null) {
				return noi;
			}
            child = child.getNextSibling();
        }
		return null;
	}
	
}
