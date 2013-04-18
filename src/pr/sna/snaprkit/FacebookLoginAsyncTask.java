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

public class FacebookLoginAsyncTask extends AbstractErrorHandlingAsyncTask<FacebookLoginInfo, Void, UserInfo>
{
	// Members
	private OnSnaprFacebookLoginListener mListener;
	private FacebookLoginInfo mLoginInfo;

	FacebookLoginAsyncTask(OnSnaprFacebookLoginListener listener)
	{
		mListener = listener;
	}
	
	@Override
	protected UserInfo computeResult(FacebookLoginInfo... params) throws HttpRetrieverException, JSONException, SnaprApiException
	{
		// Extract the data
		FacebookLoginInfo loginInfo = mLoginInfo = params[0];
		
		// Get URL for post to endpoint
		String url = getFacebookReadUrl(loginInfo.mToken, loginInfo.mClientId, loginInfo.mCreate, loginInfo.mMinAge, loginInfo.mTokenExpirationDate, loginInfo.mTokenPermissions);
		
		// Post to endpoint and get result
		HttpRetriever retriever = new HttpRetriever();
		String jsonString = retriever.retrieve(url);
		
		// Parse result
		JSONObject json = new JSONObject(jsonString);
		JSONObject response = json.getJSONObject("response");
		boolean success = json.getBoolean("success");
		UserInfo userInfo = null;
		if (success)
		{
			String accessToken = response.getString("access_token");
			String displayUserName = response.getString("display_username");
			String snaprUser = response.getString("snapr_user");
			
			userInfo = new UserInfo();
			userInfo.mAccessToken = accessToken;
			userInfo.mDisplayUserName = displayUserName;
			userInfo.mSnaprUserName = snaprUser;
		}
		else
		{
			JSONObject error = response.getJSONObject("error");
			String errorType = error.getString("type");
			String errorMessage  = error.getString("message");
			throw new SnaprApiException(errorType, errorMessage);
		}
		
		return userInfo;
	}

	@Override
	protected void onResult(UserInfo result)
	{
		if (mListener != null) mListener.onSnaprFacebookLogin(result, mLoginInfo.mRedirectUrl);
	}

	@Override
	protected void onError(Throwable e)
	{
		if (mListener != null) mListener.onSnaprFacebookLoginError(e, mLoginInfo.mRedirectUrl);
	}
	
	public interface OnSnaprFacebookLoginListener
	{
		public void onSnaprFacebookLogin(UserInfo userInfo, String redirectUrl);
		public void onSnaprFacebookLoginError(Throwable e, String redirectUrl);
	}
	
	public static String getFacebookReadUrl(String token, String clientId, String create, String minAge, Date expirationDate, List<String> permissions)
	{
    	// Declare
        String url;
    	Vector<BasicNameValuePair> params;
        
        // Add login parameters
    	params = new Vector<BasicNameValuePair>();
    	params.add(new BasicNameValuePair(Global.PARAM_TOKEN, token));
    	params.add(new BasicNameValuePair(Global.PARAM_CLIENT_ID, clientId));
    	params.add(new BasicNameValuePair(Global.PARAM_CREATE, create));
    	params.add(new BasicNameValuePair(Global.PARAM_MIN_AGE, minAge));
    	long tokenExpires = (expirationDate.getTime() - new Date().getTime()) / 1000;
    	params.add(new BasicNameValuePair(Global.PARAM_TOKEN_EXPIRES, tokenExpires+""));
    	String tokenPermissions = ListToString(permissions); 
    	params.add(new BasicNameValuePair(Global.PARAM_TOKEN_PERMISSIONS, tokenPermissions));
        
        // Create the URL
        url = UrlUtils.createUrl(UrlUtils.currentServerUrl(Global.URL_FACEBOOK_LOGIN_BASE), params, false);
        
        // Return
        return url;
	}
	
	public static String ListToString(List<String> in)
	{
		String out = "";
		
		for (String s: in)
		{
			out = out + "," + s;
		}
		
		if (out.length() > 0)
		{
			return out.substring(1);
		}
		else
		{
			return out;
		}
	}
}