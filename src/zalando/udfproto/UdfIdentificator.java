package zalando.udfproto;

import java.net.URI;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

public class UdfIdentificator {

	final ArrayList<Pair<Pattern, Integer>> WordPress_PatternArrayList = new ArrayList<>();
	final ArrayList<Pair<Pattern, Integer>> Blogger_PatternArrayList = new ArrayList<>();

	public UdfIdentificator() {
		super();	
		//LIST OF ALL AVAILABLE PATTERN
		Pair<Pattern, Integer> findOneDomain = Pair.of(Pattern.compile("[^(http|https)://][a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*"), 1);
		Pair<Pattern, Integer> wp_main1 = Pair.of(Pattern.compile("wp[-_][a-zA-Z0-9]*"), 1);
		Pair<Pattern, Integer> test = Pair.of(Pattern.compile(".*?\\bhi\\b.*?"), 1);
		Pair<Pattern, Integer> wp_main2 = Pair.of(Pattern.compile("(wp)"), 1);
		Pair<Pattern, Integer> wp_article_div = Pair.of(Pattern.compile("<((article)|(div)).*id=\"post[-_]\\d+.*\">", Pattern.DOTALL), 10);

		Pair<Pattern, Integer> blogger_main1 = Pair.of(Pattern.compile("blogger[a-zA-Z0-9-_.]+"), 1);

		//TODO
		//RegEx f�r Spezialisierung-Wordpress aufbauen. Spezialisierung == CMS
		//Wordpress Pattern Liste mit Inhalt dieser Regex
		//falles eines matched -> manualWP
		//wiederhole f�r alle SPezialisierungen
		//jedes switch-case hat eine eigene patternliste zum abarbeiten

		//WORDPRESS PATTERN
		WordPress_PatternArrayList.add(wp_main1);
		WordPress_PatternArrayList.add(wp_article_div);

		//BLOGGER PATTERN
		Blogger_PatternArrayList.add(blogger_main1);

	}
	public boolean isBlogger;
	public boolean isWP;

	public String evaluate(String...strings) throws Exception
	{

		isBlogger = false;
		isWP = false;

		int wpCount = 0;
		int bloggrCount = 0;

		String ident = "";
		for (String element : strings) {
			for (Pair<Pattern,Integer> pair : WordPress_PatternArrayList) {
				Matcher match = pair.getLeft().matcher(element);
				while (match.find() && ident.equalsIgnoreCase("")){
					wpCount += 1 * pair.getRight();
				}
			}
		}

		for (String element : strings) {
			for (Pair<Pattern,Integer> pair : Blogger_PatternArrayList) {
				Matcher match = pair.getLeft().matcher(element);
				while (match.find() && ident.equalsIgnoreCase("")){
					bloggrCount += 1 * pair.getRight();
				}
			}
		}


		if (wpCount >= bloggrCount){
			isWP = true;
			ident = "manual_wordpress";
		}

		if (bloggrCount > wpCount){
			isBlogger = true;
			ident = "blogger";
		}

		try {
			UdfRssChecker checker = new UdfRssChecker(new URI(strings[0]), isBlogger);
			boolean feed = checker.rssFeedAvailable();

			if (feed) {
				ident = "rss";
				if (this.isBlogger) {
					ident = "rssBlogger";
				}
			}

		}
		catch (Exception e) 
		{	
			throw new Exception(e);
		}

		if (ident.equalsIgnoreCase("")) {
			ident = "default";
		}

		return ident;
	}
}
