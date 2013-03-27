package pr.sna.snaprkit;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import pr.sna.snaprkit.utils.UrlUtils;
import pr.sna.snaprkit.utils.http.HttpRetriever;
import pr.sna.snaprkit.utils.http.HttpRetrieverException;

public class FacebookPublishAsyncTask extends AbstractErrorHandlingAsyncTask<FacebookPublishInfo, Void, Void>
{
	// Members
	private OnSnaprFacebookPublishListener mListener;
	private FacebookPublishInfo mPublishInfo;

	FacebookPublishAsyncTask(OnSnaprFacebookPublishListener listener)
	{
		mListener = listener;
	}
	
	@Override
	protected Void computeResult(FacebookPublishInfo... params) throws HttpRetrieverException, JSONException
	{
		// Extract the data
		FacebookPublishInfo publishInfo = mPublishInfo = params[0];
		
		// Get URL for post to endpoint
		String url = getFacebookPublishUrl(publishInfo.mToken, publishInfo.mSnaprToken, publishInfo.mTokenExpirationDate, publishInfo.mTokenPermissions);
		
		// Post to endpoint and get result
		HttpRetriever retriever = new HttpRetriever();
		String jsonString = retriever.retrieve(url);
		
		// Parse result
		JSONObject json = new JSONObject(jsonString);
		boolean success = json.getBoolean("success");
		
		// Check for error
		if (!success)
		{
			JSONObject error = json.getJSONObject("error");
			String errorType = error.getString("type");
			String errorMessage  = error.getString("message");
			throw new SnaprApiException(errorType, errorMessage);
		}
		
		return null;
	}

	@Override
	protected void onResult(Void result)
	{
		if (mListener != null) mListener.onSnaprFacebookPublish(mPublishInfo.mRedirectUrl);
	}

	@Override
	protected void onError(Throwable e)
	{
		if (mListener != null) mListener.onSnaprFacebookPublishError(e, mPublishInfo.mRedirectUrl);
	}
	
	public interface OnSnaprFacebookPublishListener
	{
		public void onSnaprFacebookPublish(String redirectUrl);
		public void onSnaprFacebookPublishError(Throwable e, String redirectUrl);
	}
	
	public static String getFacebookPublishUrl(String token, String snaprToken, Date expirationDate, List<String> permissions)
	{
    	// Declare
        String url;
    	Vector<BasicNameValuePair> params;
        
        // Add login parameters
    	params = new Vector<BasicNameValuePair>();
    	params.add(new BasicNameValuePair(Global.PARAM_TOKEN, token));
    	params.add(new BasicNameValuePair(Global.PARAM_ACCESS_TOKEN, snaprToken));
    	long tokenExpires = (expirationDate.getTime() - new Date().getTime()) / 1000;
    	params.add(new BasicNameValuePair(Global.PARAM_TOKEN_EXPIRES, tokenExpires+""));
    	String tokenPermissions = FacebookLoginAsyncTask.ListToString(permissions); 
    	params.add(new BasicNameValuePair(Global.PARAM_TOKEN_PERMISSIONS, tokenPermissions));
        
        // Create the URL
        url = UrlUtils.createUrl(Global.URL_FACEBOOK_PUBLISH, params, false);
        
        // Return
        return url;
	}
}