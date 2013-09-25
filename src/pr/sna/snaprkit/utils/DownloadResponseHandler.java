package pr.sna.snaprkit.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;

import pr.sna.snaprkit.Global;

public class DownloadResponseHandler implements ResponseHandler<Boolean>
{
	private String mFileName;
	public DownloadResponseHandler(String fileName)
	{
		mFileName = fileName;
	}
	@Override
	public Boolean handleResponse(HttpResponse response)
	{
		InputStream instream = null;
		FileOutputStream outstream = null;
		try
		{
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
			{
				instream = response.getEntity().getContent();
				outstream = new FileOutputStream(mFileName);
				int len;
    		    byte[] tmp = new byte[2048];
    		    while ((len = instream.read(tmp)) != -1)
    		    {
    		    	outstream.write(tmp, 0, len);
    		    }
			}
			return Boolean.TRUE;
		}
		catch (IOException e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with error " + e);
		}
		finally
		{
			if (outstream != null)
			{
				try
				{
					outstream.close();
				}
				catch (IOException e)
				{
				}
			}
		}
		return Boolean.FALSE;
	}
}