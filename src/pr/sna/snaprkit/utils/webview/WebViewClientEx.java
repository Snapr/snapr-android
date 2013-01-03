/*
Copyright 2012 Bricolsoft Consulting

   ----------------------------------------------------------------------------

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

   ----------------------------------------------------------------------------

   Fix for Android webview issue 17535: parameters and anchors do not 
   work when using HTML files located in the file:///android_asset/ folder. 
   The issue affects all Android versions from 3.0-4.0: 
   http://code.google.com/p/android/issues/detail?id=17535
*/

package pr.sna.snaprkit.utils.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceResponse;

import java.io.IOException;
import java.io.InputStream;

import pr.sna.snaprkit.Global;

@SuppressLint("NewApi")
public class WebViewClientEx extends WebViewClient
{
	// Constants
	private static final String ANDROID_ASSET = "file:///android_asset/";
	private static final int SDK_INT_JELLYBEAN = 16;
	
	// Members
	private Context mContext;

	// Constructors
	public WebViewClientEx(Context context)
	{
		init(context);
	}
    
	private void init(Context context)
	{
		mContext = context;
	}
	
	@Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url)
    {
        if(isAffectedOsVersion() && url.startsWith(ANDROID_ASSET) && (url.contains("?") || url.contains("#")))
        {
            return generateWebResourceResponse(url);
        }
        else
        {
            return super.shouldInterceptRequest(view, url);
        }
    }

    private WebResourceResponse generateWebResourceResponse(String url)
    {
        if (url.startsWith(ANDROID_ASSET))
        {
            String niceUrl = url;
            niceUrl = url.replaceFirst(ANDROID_ASSET, "");
            if(niceUrl.contains("?"))
            {
                niceUrl = niceUrl.split("\\?")[0];
            }
            if(niceUrl.contains("#"))
            {
                niceUrl = niceUrl.split("#")[0];
            }

            String mimetype = null;
            if(niceUrl.endsWith(".html") || niceUrl.endsWith(".htm"))
            {
                mimetype = "text/html";
            }

            try
            {
                AssetManager assets = mContext.getAssets();
                Uri uri = Uri.parse(niceUrl);
                InputStream stream = assets.open(uri.getPath(), AssetManager.ACCESS_STREAMING);
                WebResourceResponse response = new WebResourceResponse(mimetype, "UTF-8", stream);
                return response;
            }
            catch (IOException e)
            {
                if (Global.LOG_MODE) Global.log("WebViewClientEx.generateWebResourceResponse(): " + e);
            }
        }
        return null;
    }
    
    private static boolean isAffectedOsVersion()
	{
		return (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && android.os.Build.VERSION.SDK_INT < SDK_INT_JELLYBEAN);
	}
}
