/*
Copyright 2011 Bricolsoft Consulting

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package pr.sna.snaprkit.utils.http;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

public class HttpRetriever
{	
	// Members
	DefaultHttpClient httpclient = null;
	String mUserName = null;
	String mPassword = null;
	
	// PreemptiveAuth support class
    public class PreemptiveAuthHttpRequestInterceptor implements HttpRequestInterceptor
    {
    	@Override
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException
        {
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
            CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
            HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            
            if (authState.getAuthScheme() == null)
            {
                AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());
                Credentials creds = credsProvider.getCredentials(authScope);
                if (creds != null)
                {
                	authState.setAuthScheme(new BasicScheme());
                    authState.setCredentials(creds);
                }
            }
        }
    }
	
	// Constructors
    public HttpRetriever()
    {
    	init();
    }
	
	private void init()
	{
		// Create http client
		httpclient = new DefaultHttpClient();
	}
	
	// Set credentials for basic auth
	public void setCredentials(String userName, String password)
	{
		// Set members
		mUserName = userName;
		mPassword = password;
		
		// Set basic auth user and pass
		if (mUserName != null && mPassword != null)
    	{
    		// Set the credentials
			httpclient.getCredentialsProvider().setCredentials(
	        		new AuthScope(null, -1),
	        		new UsernamePasswordCredentials(mUserName, mPassword));
			
			// Enable preemptive HTTP Basic Auth
	        httpclient.addRequestInterceptor(new PreemptiveAuthHttpRequestInterceptor(), 0);
    	}
	}
	
	// Clear credentials for basic auth
	public void clearCredentials()
	{
		// Set members
		mUserName = null;
		mPassword = null;
		
		// Clear httpclient credentials
		httpclient.getCredentialsProvider().clear();
		
		// Remove pre-emptive HTTP Basic Auth
		httpclient.clearRequestInterceptors();
	}
	
	// Document retrieval function
	public String retrieve(String url) throws HttpRetrieverException
    {
    	// Declare
    	String response = null;
    	
		// Connect to server and get JSON response
		HttpGet request = new HttpGet(url);
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
    	try
		{
			response = httpclient.execute(request, responseHandler);
		}
    	catch (ClientProtocolException e)
		{
			throw new HttpRetrieverException("Could not retrieve HTTP data due to HTTP protocol error!", e);
		}
    	catch (IOException e)
		{
    		throw new HttpRetrieverException("Could not retrieve HTTP data due to invalid HTTP response!", e);
		}
    	
    	// Return
    	return response;
    }
}