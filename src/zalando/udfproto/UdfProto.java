package zalando.udfproto;

import java.io.IOException;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;
import org.json.simple.JSONObject;

public class UdfProto extends EvalFunc<String>
{
	@Override
	public String exec(Tuple inputTuple) throws IOException {

		if (inputTuple == null || inputTuple.size() != 2) {
			return null;
		}
		try
		{
			String url = (String) inputTuple.get(0);
			String html = (String) inputTuple.get(1);

			JSONObject result;

			String selector = new UdfIdentificator().evaluate(url, html);

			switch (selector)
			{
			case "manual_wordpress":
			{
				UdfManualWordpressPipe mwp = new UdfManualWordpressPipe(url, html);
				result = mwp.process();

				if(result != null)
					result.put("selector", selector);
				else
					result = this.defaultPipeProcess(url, html);
				break;
			}
			case "blogger":
			{
				UdfBloggerPipe blogger_pipe = new UdfBloggerPipe(url, html);
				result = blogger_pipe.process();

				if(result != null)
					result.put("selector", selector);
				else 
					result = this.defaultPipeProcess(url, html);
				break;
			}
			case "rssBlogger":
			case "rss":
			{
				boolean isBlogger = !selector.equalsIgnoreCase("rss");
				UdfRssPipe rp = new UdfRssPipe(url, isBlogger);
				result = rp.process();

				if (result != null) 
					result.put("selector", selector);
				else 
					result = this.defaultPipeProcess(url, html);
				break;
			}
			default:
				result = this.defaultPipeProcess(url, html);
				break;
			}

			return result.toJSONString();
		}
		catch (Exception e)
		{
			throw new IOException("Caught exception processing input row ", e);
		}
	}
	
	private JSONObject defaultPipeProcess(String urlFromRaw, String htmlFromRaw) throws Exception
	{
		UdfBPPipe bp_pipe = new UdfBPPipe(urlFromRaw, htmlFromRaw);
		JSONObject result = bp_pipe.process();

		if(result != null)
			result.put("selector", "default");
		
		return result;
	}
}
