		package zalando.classifier.pipes;

		import java.io.StringReader;
		import java.util.ArrayList;
		import java.util.regex.Pattern;

		import org.apache.commons.lang3.StringEscapeUtils;
		import org.apache.commons.lang3.StringUtils;
		import org.apache.xerces.dom.AttrNSImpl;
		import org.cyberneko.html.parsers.DOMParser;
		import org.json.simple.JSONArray;
		import org.json.simple.JSONObject;
		import org.w3c.dom.NamedNodeMap;
		import org.w3c.dom.Node;
		import org.xml.sax.InputSource;

		import com.rometools.rome.feed.rss.Image;

		import info.debatty.java.stringsimilarity.NormalizedLevenshtein;
		import zalando.classifier.Start;
		import zalando.classifier.main.ImageParser;
		import zalando.classifier.main.SimilarityUtil;

		/**
		 * @author Carsten
		 *
		 * Die Manualwordpress Pipe sucht nach vorab definierte Tags.
		 * Während der Suche werden ungewollte Tags und Attribute eines Tags entfernt, 
		 * da diese zu Probleme bei der Verarbeitung führen können.
		 *
		 */
		public class BloggerPipe {
			
			private String url;
			private String html;
			private ArrayList<String> unwantedTags;
			private ArrayList<String> unwantedAtts;
			private ArrayList<Pattern> unwantedCss;
			
			
			/**
			 * der Konstruktor
			 * 
			 * @param url enthält die url zu dem jeweiligen blogeintrag
			 * @param html der komplette html code des blogeintrags
			 */
			public BloggerPipe(String url, String html) {
				super();
				this.url = url;
				this.html = html;
				
				System.err.println("BloggerPipe active");
				unwantedTags = new ArrayList<>();
				unwantedTags.add("script");
				unwantedTags.add("#comment");
				unwantedTags.add("style");
				
				unwantedAtts = new ArrayList<>();
				unwantedAtts.add("onclick");
				unwantedAtts.add("href");
				
				unwantedCss = new ArrayList<>();
				unwantedCss.add(Pattern.compile(".*comment.*"));
			}
			
			
			/**
			 * Process verarbeitet die eigentlichen Daten und erzeugt ein
			 * JSONObject mit den Vergleichswerten zum Goldstandard.
			 * 
			 * 
			 * @return JSONObject welches extrahierten Text und Titel, die Daten aus dem Goldstandard sowie die Vergleichswerte enthält.
			 */
			public JSONObject process()
			{	
				DOMParser parser = new DOMParser();
				InputSource is = new InputSource(new StringReader(this.html));
				try {
					parser.parse(is);
					Node doc = parser.getDocument();
					
					//das durch den DOMParser erstellte Document wird nach den identifizierten Tags durchsucht 
					//und anschließend werden ungewollte Tags/Attribute aus dem Ergebnis entfernt
					doc = this.getNodesOfInterest(doc);
					if (doc == null) {
						return null;
					}
					
					//Ab hier beginnt der Vergleich mit dem Goldstandard
					JSONObject goldObj = Start.gold.get(this.url);
					if (goldObj == null) 
					{
						return null;
					}
					String titleGold = goldObj.get("title").toString();
					String text = goldObj.get("text").toString();
					if (titleGold == null) {
						titleGold = "";
					}
					String titlePipe = this.getTitleFromUrl();
					if (titlePipe == null) {
						titlePipe = "";
					}
					//toDO nicht alle unprintable Sachen l�schen evtl, Linebreaks
					JSONObject obj = new JSONObject();
					NormalizedLevenshtein nls = new NormalizedLevenshtein();
					double lev = nls.distance(StringUtils.deleteWhitespace(titlePipe), StringUtils.deleteWhitespace(titleGold));
					String levFine = String.format("%.2f", lev);
					if(doc != null){
						//.replaceAll("\\s+", " ")
						//String docText = doc.getTextContent().replaceAll("(\\r?\\n)+", "\n\n");
						String docText = doc.getTextContent();
						docText = StringEscapeUtils.unescapeJava(docText);
						double cosine = SimilarityUtil.consineTextSimilarity(StringUtils.split(docText), StringUtils.split(goldObj.get("text").toString()));
						String cosineFine = String.format("%.2f", cosine);
						//COMPARING END
						
						//toDO Collect more Meta Informations
						//like author, url, domain, date, img-alt-tag think about more
						//
						
						//Das JSONObject wird entsprechend der vorab erzeugten Daten aufgebaut
						JSONObject pipeObj = new JSONObject();
						pipeObj.put("title", titlePipe);
						pipeObj.put("text", docText);
									
						JSONObject simObj = new JSONObject();
						simObj.put("title", levFine);
						simObj.put("text", cosineFine);
						
						obj.put("source", this.url);
						obj.put("pipe", pipeObj);
						obj.put("gold", goldObj);
						obj.put("similarity", simObj);
						
						ImageParser ip = new ImageParser(doc);
						JSONArray ipArray = ip.result();
						if (ipArray != null) {
							obj.put("images_alt_tags", ipArray);
						}
					}

					return obj;
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return null;
			}
			//toDo find more matches to make result better
			//check Images
			//
			
			/**
			 * Diese Methode dient dazu den Node rekursiv zu suchen, der dem RegEx (post|entry|main)?[-_]?(content|body) entspricht.
			 * Diese Methode sollte nur verwendet werden, um das Ergebnis der getArticleIdNode() Methode zu verbessern.
			 * Um einen besseren Recall zu erzeugen, wird diese Methode auch genutzt, falls getArticleIdNode() kein Ergebnis liefert.
			 * 
			 * 
			 * @param node der zu durchsuchende Node
			 * @return der gefundene Node oder Null
			 */
			private Node getPostBodyNode(Node node)
			{
				Pattern p = Pattern.compile("(post|entry|main)?[-_]?(content|body)", Pattern.CASE_INSENSITIVE);
				
				//bei der Analyse der Zalando-Daten stellte sich raus das lediglich div bzw. article Tags von relevanz sind
				if (node.getNodeName().equalsIgnoreCase("div") || node.getNodeName().equalsIgnoreCase("article")) {
					
					//für alle div bzw. article Tags werden die Attribute durchsucht und geprüft ob das id bzw. class
					//Attribute das RegEx Pattern matcht.
					NamedNodeMap atts = node.getAttributes();
					for (int i = 0; i < atts.getLength(); i++) {
						Object att = atts.item(i);
						if (att instanceof AttrNSImpl) {
							AttrNSImpl realAtt = (AttrNSImpl)att;
							if (realAtt.getName().equalsIgnoreCase("id") ||
								realAtt.getName().equalsIgnoreCase("class")) 
							{
								if (p.matcher(realAtt.getValue()).matches()) 
								{
									return node;
								}
							}
						}
					}
				}
				//Falls bisher kein Match für das RegEx Pattern gefunden wurde werden die Kinder des
				//Nodes durchsucht
				Node child = node.getFirstChild();
		        while (child != null) {
		            Node noi = this.getPostBodyNode(child);
		            if (noi != null) {
						return noi;
					}
		            child = child.getNextSibling();
		        }
		        return null;
			}
			
			/**
			 * Diese Methode dient dazu den Node rekursiv zu suchen, der dem RegEx (post|article)[-_]\\d+ entspricht.
			 * Bei der Analyse der Zalando-Daten hat sich gezeigt, das man 70% der Wordpress-blogs mit diesem RegEx Pattern 
			 * abdecken kann.
			 * 
			 * @param node der zu durchsuchende Node
			 * @return der gefundene Node oder Null
			 */
			private Node getArticleIdNode(Node node)
			{
				Pattern p = Pattern.compile("(post)[-_](body)(.*?)", Pattern.CASE_INSENSITIVE);
				if (node.getNodeName().equalsIgnoreCase("div") || node.getNodeName().equalsIgnoreCase("article"))
		        {
					//für alle div bzw. article Tags werden die Attribute durchsucht und geprüft ob das id bzw. class
					//Attribute das RegEx Pattern matcht.
		        	NamedNodeMap atts = node.getAttributes();
					for (int i = 0; i < atts.getLength(); i++) 
					{
						Object att = atts.item(i);
						if (att instanceof AttrNSImpl) 
						{
							AttrNSImpl realAtt = (AttrNSImpl)att;
							if (realAtt.getName().equalsIgnoreCase("id") ||
								realAtt.getName().equalsIgnoreCase("class")) 
							{
								System.out.println("name: " + realAtt.getName());
								System.out.println("val: " +realAtt.getValue());
								if (p.matcher(realAtt.getValue()).matches()) 
								{
									System.out.println("MACHTED");
									return node;
								}
							}
						}
					}
		        }
				//Falls bisher kein Match für das RegEx Pattern gefunden wurde werden die Kinder des
				//Nodes durchsucht
		        Node child = node.getFirstChild();
		        while (child != null) {
		            Node noi = this.getArticleIdNode(child);
		            if (noi != null) {
						return noi;
					}
		            child = child.getNextSibling();
		        }
		        return null;
			}
			
			//------------------------//
			
			private Node getArticleNode(Node node)
			{
				Pattern p = Pattern.compile("post([-_]\\d+)?", Pattern.CASE_INSENSITIVE);
				
				//bei der Analyse der Zalando-Daten stellte sich raus das lediglich div bzw. article Tags von relevanz sind
				if (node.getNodeName().equalsIgnoreCase("article")) {
					
					//für alle div bzw. article Tags werden die Attribute durchsucht und geprüft ob das id bzw. class
					//Attribute das RegEx Pattern matcht.
					NamedNodeMap atts = node.getAttributes();
					for (int i = 0; i < atts.getLength(); i++) {
						Object att = atts.item(i);
						if (att instanceof AttrNSImpl) {
							AttrNSImpl realAtt = (AttrNSImpl)att;
							if (realAtt.getName().equalsIgnoreCase("class")) 
							{
								if (p.matcher(realAtt.getValue()).matches()) 
								{
									return node;
								}
							}
						}
					}
				}
				//Falls bisher kein Match für das RegEx Pattern gefunden wurde werden die Kinder des
				//Nodes durchsucht
				Node child = node.getFirstChild();
		        while (child != null) {
		            Node noi = this.getPostBodyNode(child);
		            if (noi != null) {
						return noi;
					}
		            child = child.getNextSibling();
		        }
		        return null;
			}
			
			private Node getEntryNode(Node node)
			{
				Pattern pClass = Pattern.compile("(post|entry)[-_](body|content)", Pattern.CASE_INSENSITIVE);
				Pattern pId = Pattern.compile("post[-_]body[-_]\\d+", Pattern.CASE_INSENSITIVE);
				Pattern pItemprop = Pattern.compile("articleBody", Pattern.CASE_INSENSITIVE);
				
				//bei der Analyse der Zalando-Daten stellte sich raus das lediglich div bzw. article Tags von relevanz sind
				if (node.getNodeName().equalsIgnoreCase("div")) {
					
					//für alle div bzw. article Tags werden die Attribute durchsucht und geprüft ob das id bzw. class
					//Attribute das RegEx Pattern matcht.
					NamedNodeMap atts = node.getAttributes();
					for (int i = 0; i < atts.getLength(); i++) {
						Object att = atts.item(i);
						if (att instanceof AttrNSImpl) {
							AttrNSImpl realAtt = (AttrNSImpl)att;
							if (realAtt.getName().equalsIgnoreCase("id")) 
								if (pId.matcher(realAtt.getValue()).matches()) 
									return node;
							
							if (realAtt.getName().equalsIgnoreCase("class")) 
								if (pClass.matcher(realAtt.getValue()).find()) 
									return node;
							
							if (realAtt.getName().equalsIgnoreCase("itemprop")) 
								if (pItemprop.matcher(realAtt.getValue()).find()) 
									return node;
						}
					}
				}
				//Falls bisher kein Match für das RegEx Pattern gefunden wurde werden die Kinder des
				//Nodes durchsucht
				Node child = node.getFirstChild();
		        while (child != null) {
		            Node noi = this.getEntryNode(child);
		            if (noi != null) {
						return noi;
					}
		            child = child.getNextSibling();
		        }
		        return null;
			}
			
			private Node getLinkNode(Node node)
			{
				Pattern p = Pattern.compile("name=\"\\d+\"", Pattern.CASE_INSENSITIVE);
				if (node.getNodeName().equalsIgnoreCase("a"))
		        {
					//für alle div bzw. article Tags werden die Attribute durchsucht und geprüft ob das id bzw. class
					//Attribute das RegEx Pattern matcht.
		        	NamedNodeMap atts = node.getAttributes();
					for (int i = 0; i < atts.getLength(); i++) 
					{
						Object att = atts.item(i);
						if (att instanceof AttrNSImpl) 
						{
							AttrNSImpl realAtt = (AttrNSImpl)att;
							if (realAtt.getName().equalsIgnoreCase("name")) 
							{
								if (p.matcher(realAtt.toString()).matches()) 
								{
									return node.getParentNode();
								}
							}
						}
					}
		        }
				//Falls bisher kein Match für das RegEx Pattern gefunden wurde werden die Kinder des
				//Nodes durchsucht
		        Node child = node.getFirstChild();
		        while (child != null) {
		            Node noi = this.getLinkNode(child);
		            if (noi != null) {
						return noi;
					}
		            child = child.getNextSibling();
		        }
		        return null;
			}
			
			
			/**
			 * Diese Methode dient dazu entsprechenden Methoden zur Filterung aufzurufen.
			 * 
			 * @param node der Node der durchsucht werden soll
			 * @return der gefilterterte Node oder null
			 */
			private Node getNodesOfInterest(Node node)
			{
				Node linkNode = getLinkNode(node);
				if (linkNode != null) {
					Node entryNode = getEntryNode(linkNode);
					if (entryNode != null) {
						removeTags(entryNode);
						return entryNode;
					} 
					else {
						removeTags(linkNode);
						return linkNode;
					}
				}
				else {
					Node articleNode = getArticleNode(node);
					if (articleNode != null) {
						Node entryNode = getEntryNode(articleNode);
						if (entryNode != null) {
							removeTags(entryNode);
							return entryNode;
						} 
						else {
							removeTags(articleNode);
							return articleNode;
						}
					}
				}
				//Node finden, der tag enthält, welches 70% aller Wordpress Blogs hat.
//				Node articleIdNode = getArticleIdNode(node);
//				Node postBodyNode = null;
//				if (articleIdNode != null) {
//					//Falls dieses gefunden wurde, versuchen den Content noch zu verfeiern
//					postBodyNode = getPostBodyNode(articleIdNode);
//					//gefunden Node säubern und zurückgeben
//					if (postBodyNode != null) {
//						removeTags(postBodyNode);
//						return postBodyNode;
//					}
//					else {
//						removeTags(articleIdNode);
//						return articleIdNode;
//					}
//						
//				}
//				else
//				{
//					//Falls der aktuelle Blog nicht zu den 70% gehört, versuchen den content trotzdem zu finden
//					//wird nur gemacht um den Recall zu erhöhen
//					postBodyNode = getPostBodyNode(node);
//					if (postBodyNode != null) {
//						removeTags(postBodyNode);
//						return postBodyNode;
//					}
//				}
				//Solange rekursiv suchen, bis ein node gefunden wurde oder null zurückgeben, falls nichts vorhanden ist
				Node child = node.getFirstChild();
				while (child != null) {
					Node noi = this.getNodesOfInterest(child);
					if (noi != null) {
						return noi;
					}
					child = child.getNextSibling();
				}
				return null;
			}
			
			/**
			 * Die Methode erzeugt aus der URL den Titel
			 * 
			 * @return Titel des Blogs
			 */
			private String getTitleFromUrl()
			{
				String[] parts = this.url.split("/");
				String lastPart = parts[parts.length-1];
				if (lastPart.equalsIgnoreCase("")) {
					lastPart = parts[parts.length-2];
				}
				lastPart = lastPart.replace(".html", "");
				lastPart = lastPart.replace("-", " ");
				lastPart = lastPart.replace("_", " ");
				
				return lastPart.trim();
			}
			
			
			/**
			 * Diese Methode dient dazu, Tags und Attribute aus einem Node zu entfernen, 
			 * die ungewollt sind bzw. Probleme bei der weiteren Verarbeitung machen
			 * 
			 * @param node Node aus dem Tags entfernt werden sollen
			 */
			private void removeTags(Node node)
			{
				if(node != null){
					NamedNodeMap atts = node.getAttributes();
					if (atts != null) {
						for (int i = 0; i < atts.getLength(); i++) {
							Object att = atts.item(i);
							if (att instanceof AttrNSImpl) {
								AttrNSImpl realAtt = (AttrNSImpl)att;
								if (unwantedAtts.contains(realAtt.getName())) {
									atts.removeNamedItem(realAtt.getName());
								}
								for (Pattern pattern : unwantedCss) {
									if (pattern.matcher(realAtt.getNodeValue()).find()) {
										removeSiblings(node);
										return;
									}
								}
							}
						}
					}	
				if (this.unwantedTags.contains(node.getNodeName().toLowerCase())) {
					node.setTextContent("");
				}
				Node child = node.getFirstChild();
		        while (child != null) {
		        	removeTags(child);
		            child = child.getNextSibling();
		        }
				}
			}
			
			/**
			 * Dient dazu alle Nachfolger zu löschen, um so das Ergebnis zu verfeinern.
			 * 
			 * @param node Der Node dessen Nachfolger entfernt werden sollen
			 */
			private void removeSiblings(Node node)
			{
				Node sib = node.getNextSibling();
				if (sib != null) {
					removeSiblings(sib);
				}
				node.getParentNode().removeChild(node);
			}
		}
