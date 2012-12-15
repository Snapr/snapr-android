package pr.sna.snaprkit.utils;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import pr.sna.snaprkit.Global;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;

public class UrlUtils
{
	// Converts a regular URL to an AJAX URL
	// A regular URL is file://mnt/sdcard/snaprkit_html/page.html?a=x&b=y
	// An AJAX URL with menu base: file://mnt/sdcard/snaprkit_html/index.html#/page.html?a=x&b=y
	// An AJAX URL without menu base: file://mnt/sdcard/snaprkit_html/page.html#?a=x&b=y
	public static String ajaxUrl(String url)
	{
		// Parse the URL
		Uri uri = Uri.parse(url);
		
		// Get the page
		String page = uri.getLastPathSegment();
		
		// Change to index.html#/page.html
		url = url.replaceFirst(page, "index.html" + "#/" + page);
		url = url.replace("#/index.html", "#/"); // Avoid index.html#.index.html
		
		// Return
		return url;
	}

	// Converts a SNAPR AJAX URL to a regular URL
	// An AJAX URL is file://mnt/sdcard/snaprkit_html/index.html#/page.html?a=x&b=y
	//             or file://mnt/sdcard/snaprkit_html/page.html#?a=x&b=y
	// A normal URL is file://mnt/sdcard/snaprkit_html/page.html?a=x&b=y
	public static String normalUrl(String ajaxUrl)
	{
		// Load URL into Uri
		Uri ajaxUri = Uri.parse(ajaxUrl);
		
		// Create a new URI out of the Ajax fragment
		String fragment = ajaxUri.getEncodedFragment();
		
		// Check fragment
		if ((fragment == null) || (fragment.length() == 0))
		{
			// No fragment, hence not an Ajax URL and already a normal URL
			return ajaxUrl;
		}
		
		// Parse fragment
		Uri fragmentUri = Uri.parse(fragment);
		
		// Create normal url
		Uri.Builder normalUriBuilder = new Uri.Builder();
		normalUriBuilder.scheme(ajaxUri.getScheme());                     // http://
		normalUriBuilder.encodedAuthority(ajaxUri.getEncodedAuthority()); // name@domain.com
		normalUriBuilder.encodedPath(ajaxUri.getEncodedPath());           // /folder/file.html
		normalUriBuilder.encodedQuery(fragmentUri.getEncodedQuery());     // ?param=value&param2=value2
		
		// Convert to string
		String normalUrl = normalUriBuilder.toString();
		
		// See if we need to replace the html page
		String newLastPathSegment = fragmentUri.getLastPathSegment(); 
		if ((newLastPathSegment != null) && (newLastPathSegment.length() > 0))
		{
			// Get current last path segment
			String currentLastPathSegment = ajaxUri.getLastPathSegment();
			
			// Check result
			if((currentLastPathSegment != null) && (currentLastPathSegment.length() > 0))
			{
				// Perform replace using encoded versions
				normalUrl = normalUrl.replace(Uri.encode(currentLastPathSegment), Uri.encode(newLastPathSegment));
			}
		}
		
		// Return
		return normalUrl;
	}
	
	public static boolean isFullUrl(String url)
	{
		// Check for null
		if (url == null) return false;
		
		// Parse the URL
		Uri uri = Uri.parse(url);
		
		return  (uri.getScheme()!= null && !uri.getScheme().equals("")); 
	}
	
	public static String getFullLocalUrl(String url)
	{
		return Global.URL_BASE + url;
	}
	
	public static String relativeUrlToFullUrl(String url)
	{
		// Check for null or blank
		if (url == null || url.equals(""))
		{
			return null;
		}
		
		// Check for full url
		if (isFullUrl(url)) return url;
		
		// Check for validity
		if (url.startsWith("index.html#"))
		{
			return Global.URL_BASE + url;
		}
		else
		{
			// Assume we have a build page name
			return null;
		}
	}
	
	public static boolean isLocalUrl(String url)
	{
		// Check for null
		if (url == null) return false;
		
		// Parse the URL
		Uri uri = Uri.parse(url);
		
		// Check protocol
		if (uri.getScheme()!= null && ((uri.getScheme().equals("file")) || (uri.getScheme().equals("snapr")) || url.startsWith("about:")))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	// Finds the NameValuePair with the specified key name from a list of NameValuePairs
	public static NameValuePair getNameValuePairByName(List<NameValuePair> pairs, String name)
	{
		// Declare
		NameValuePair nv;
		
		// Loop through
		for (int i=0; i< pairs.size(); i++)
		{
			nv = pairs.get(i);
			if(nv.getName() == name) return nv;
		}
		
		// Return
		return null;
	}
	
	// Removes the GET parameters from the provided URL
	// e.g. http://www.example.com?a=x&b=y becomes 
	//      http://www.example.com
	public static String getBareUrl(String url)
    {
    	// Handle blank input
    	if(url == null) return null;
    	
    	// Get index of ?
    	int index = url.indexOf("?");
    	
    	// Check index
    	if(index == -1)
    		// No ? found - return existing
    		return url;
    	else
    		// Found ? - return substring
    		return url.substring(0, index);
    }
	    
	public static void addBasicUrlParams(Vector<BasicNameValuePair> params, Context context)
	{
		// Add the app mode
		params.add(new BasicNameValuePair(Global.PARAM_APPMODE, "android"));
		params.add(new BasicNameValuePair(Global.PARAM_ENVIRONMENT, Global.ENVIRONMENT));

		// Customize some parameters based on logged in status
		String[] userInfo = new String[3];
		UserInfoUtils.loadUserInfo(context, userInfo);
		String displayUserName = userInfo[0];
		String snaprUserName = userInfo[1];
		String accessToken = userInfo[2];
		if (UserInfoUtils.haveUserInfo(displayUserName, snaprUserName, accessToken))
		{
			// Create URL that performs login
			params.add(new BasicNameValuePair(Global.PARAM_DISPLAY_USERNAME, displayUserName));
			params.add(new BasicNameValuePair(Global.PARAM_SNAPR_USER, snaprUserName));
			params.add(new BasicNameValuePair(Global.PARAM_ACCESS_TOKEN, accessToken));
		}
		else
		{
			// We have no username and password, so create URL that indicates
			// new user
			params.add(new BasicNameValuePair(Global.PARAM_NEW_USER, "true"));
		}
	}
	
	// Takes a base URL that may contain GET parameters and adds the GET parameters in params
	// In case a parameter already exists, uses replace boolean to determine action
    public static String createUrl(String baseUrl, Vector<BasicNameValuePair> params, boolean replace)
    {    
    	// Create Url from final params
    	String encodedParams = URLEncodedUtils.format(params, "UTF-8");
    	encodedParams = encodedParams.replace("+", "%20"); // Encode spaces as %20
    	String newUrl = baseUrl + "?" + encodedParams;

    	// Return a string with the new URL
    	return newUrl;
    }
    
    // Takes a file path like /sdcard/file.txt and converts it to a 
    // URL like file:///sdcard/file.txt
    public static String fileNameToUrl(String fileName)
    {
    	if (fileName == null || fileName.length() == 0)
    		return null;
    	else
    	{
    		return "file://" + fileName;
    	}
    }
    
    // Takes a URL like file:///sdcard/file.txt and converts it to a 
    // file path like /sdcard/file.txt  
    public static String urlToFileName(String url)
    {
    	if (url == null || url.length() == 0)
    	{
    		return null;
    	}
    	else if (url.startsWith("file://"))
    	{
    		return url.substring(7);
    	}
    	else
    	{
    		return url;
    	}
    }
    
    public static String getUrlContent(String url)
    {
    	// Call the Snapr API
    	String response = null;
    	try
    	{
    		// Connect to server and get JSON response
    		DefaultHttpClient httpclient = new DefaultHttpClient();
    		HttpUriRequest request = new HttpGet(url);
    		ResponseHandler<String> responseHandler = new BasicResponseHandler();
    		response = httpclient.execute(request, responseHandler);
    	}
    	catch (Exception e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with error " + e);
    	}
    	
    	return response;
    }
    
    public static String getPreviousUrl(WebView webView)
    {
    	// Declare
    	String url = null;
    	
    	// Get the history
    	WebBackForwardList history = webView.copyBackForwardList();
    	
    	// Get the current index
    	int index = history.getCurrentIndex();
    	
    	// Try to retrieve the url at the previous index
    	try
    	{
	    	if (index > 0)
	    	{
	    		WebHistoryItem item = history.getItemAtIndex(index-1);
	    		url = item.getUrl();
	    	}
    	}
    	catch (Exception e)
    	{
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with error " + e);
    	}
    	
    	// Return
    	return url;
    }
    
    public static String getRealPathFromURI(Context context, Uri contentUri) {
    	Cursor cursor = null;
    	try {
    		String[] proj = { MediaStore.Images.Media.DATA };
	        cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
	        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	        cursor.moveToFirst();
	        return cursor.getString(column_index);
    	} catch (Exception e) {
    		e.printStackTrace();
    		return null;
    	} finally {
    		if (cursor != null && !cursor.isClosed()) cursor.close();
    	}
    }
    
    public static String imageUri2Path(Uri imageUri)
    {
    	return ((imageUri != null) ? imageUri.toString().substring(7) : null);
    }
    
    public static Uri imagePath2Uri(String imagePath)
    {
    	if (imagePath == null) return null;
    	File file = new File(imagePath);
    	return Uri.fromFile(file);
    }
    
    /* This function fixes a query param decoding bug in all Android versions prior 
     * to Android 4.0 where '+' characters are not translated into ' ' in 
     * calls to android.net.Uri.getQueryParameter. This bug was officially fixed in 
     * Android 4.0 and is documented in the Android docs for android.net.Uri.getQueryParam. 
     * This fix takes the form of a function because unfortunately android.net.Uri 
     * is an abstract class that cannot be extended, meaning that a more elegant 
     * object-oriented fix is not possible
    */
    public static String getQueryParameter(Uri uri, String key)
    {
    	String value = uri.getQueryParameter(key);
    	return (value==null)?value:value.replace("+", " ");
    }
    
    /**
     * Removes the param-value pair with the specified param name from an encodedQuery
     * 
     * @param paramName - Name of parameter whose parameter-value pair should be removed
     * @param encodedQuery - Encoded query from which we should remove the param-value pair
     *                       An encoded query is the part that comes after ? in an URL,
     *                       and whose parameter values have not been decoded.
     * @return
     */
    public static String removeParamValuePairFromEncodedQuery(String paramName, String encodedQuery)
    {
    	String result = "";
		String[] paramPairs = encodedQuery.split("&");
		for (String paramPair : paramPairs)
		{
			if (!paramPair.startsWith(paramName+"="))
			{
				result += paramPair + "&";
			}
		}
		if (result.length() > 0) result = result.substring(0, result.length()-1);
		
		return result;
    }
    
    /**
     * Returns a set of the unique names of all query parameters. Iterating
     * over the set will return the names in order of their first occurrence.
     *
     * @throws UnsupportedOperationException if this isn't a hierarchical URI
     *
     * @return a set of decoded names
     * 
     * Lifted from Android 4.1.1 source code for android.net.Uri and modified 
     * to work with uri parameter
     */
    public static Set<String> getQueryParameterNames(Uri uri) {
        if (uri.isOpaque()) {
            throw new UnsupportedOperationException("This isn't a hierarchical URI.");
        }

        String query = uri.getEncodedQuery();
        if (query == null) {
            return Collections.emptySet();
        }

        Set<String> names = new LinkedHashSet<String>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;

            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }

            String name = query.substring(start, separator);
            names.add(Uri.decode(name));

            // Move start to end of name.
            start = end + 1;
        } while (start < query.length());

        return Collections.unmodifiableSet(names);
    }
    
    public static void appendParams(Vector<BasicNameValuePair> existingParams, String newParams)
    {
	    String url = "snapr://dummy?" + newParams;
		Uri uri = Uri.parse(url);
		Set<String> paramNames = UrlUtils.getQueryParameterNames(uri);
		for (String paramName: paramNames)
		{
			// Get the parameter value
			String paramValue = uri.getQueryParameter(paramName);
			
			// Add a new basic name value pair
			existingParams.add(new BasicNameValuePair(paramName, paramValue));
		}
    }
    
    /**
     * Takes an URL in the form snapr://redirect?redirect_url=encoded_url&param1=x&param2=y
     * and transforms it into an url like decode(encoded_url)&param1=x&param2=y
     * @param snaprRedirectUrl - The full snapr://redirect URL
     * @param additionalParams - Additional parameters to append in addition to supplied params in the url
     * @return The redirect url
     */
    public static String createRedirectUrlFromSnaprRedirectUrl(String snaprRedirectUrl, String additionalParams)
    {
    	// Check params
    	if (snaprRedirectUrl == null) return null;
    	
    	// Remove the redirect_url param from the URL's set of params
    	Uri uri = Uri.parse(snaprRedirectUrl);
    	
    	if (uri == null || uri.getScheme() == null || !uri.getScheme().equals(Global.SCHEME_SNAPR)) return null;
    	
    	String encodedQuery = uri.getEncodedQuery();
    	String encodedQuery2 = UrlUtils.removeParamValuePairFromEncodedQuery(Global.PARAM_REDIRECT_URL, encodedQuery);
	
    	// Set the url to the embedded parameter
    	String redirectUrl = UrlUtils.getQueryParameter(uri, Global.PARAM_REDIRECT_URL);
    	if (encodedQuery2.length() > 0)
    	{
    		redirectUrl = redirectUrl + "&" + encodedQuery2;
    		if (!redirectUrl.endsWith("?")) redirectUrl += "&";
    	}
    	
    	// Add the additional params
    	if (additionalParams != null && additionalParams.length() > 0)
    	{
    		if (!redirectUrl.endsWith("?")) redirectUrl += "&";
    		redirectUrl += additionalParams;
    	}
    	
    	return redirectUrl;
    }
    
    /**
     * Takes a string to be entered into the webview URL as a javascript: call and sanitizes it by escaping
     * apostrophes. Apostrophes are transformed into their corresponding HTML code (&#39)
     * @param js String containing JavaScript
     * @return Escaped JavaScript string
     */
    public static String jsEscape(String js)
    {
    	return js.replace("'", "&#39;");
    }
}
