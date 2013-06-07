package pr.sna.snaprkit.utils.http;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

public class MyBasicResponseHandler implements ResponseHandler<String>
{
	/**
	 * Returns the response body as a String if the response was successful (a
	 * 2xx status code). If no response body exists, this returns null. If the
	 * response was unsuccessful (>= 300 status code), throws an
	 * {@link HttpResponseException}.
	*/
	
	// Members
	private int mIgnoredStatusCodes[] = null;
	
	public MyBasicResponseHandler(int ignoredStatusCodes[])
	{
		mIgnoredStatusCodes = ignoredStatusCodes;
	}
	 		
	public String handleResponse(final HttpResponse response) throws HttpResponseException, IOException
	{
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() >= 300)
		{
			if (!isInList(HttpStatus.SC_UNPROCESSABLE_ENTITY, mIgnoredStatusCodes))
			{
				throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
			}
		}
		HttpEntity entity = response.getEntity();
		return entity == null ? null : EntityUtils.toString(entity);
	}
	
	private boolean isInList(int value, int[] list)
	{
		if (list == null) return false;
		
		for (int i=0; i<list.length; i++)
		{
			if (list[i] == value) return true;
		}
		
		return false;
	}
	
}