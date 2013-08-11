package pr.sna.snaprkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import org.apache.http.client.HttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.FacebookOperationCanceledException;
import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.OpenRequest;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.model.GraphUser;

import pr.sna.snaprkit.FacebookLoginAsyncTask.OnSnaprFacebookLoginListener;
import pr.sna.snaprkit.FacebookPublishAsyncTask.OnSnaprFacebookPublishListener;
import pr.sna.snaprkit.PictureAcquisitionManager.PictureAcquisitionListener;
import pr.sna.snaprkit.dummy.FeatherActivity;
import pr.sna.snaprkit.utils.CameraUtils;
import pr.sna.snaprkit.utils.ExceptionUtils;
import pr.sna.snaprkit.utils.LocalizationUtils;
import pr.sna.snaprkit.utils.UserInfoUtils;
import pr.sna.snaprkit.utils.FileUtils;
import pr.sna.snaprkit.utils.GeoManager;
import pr.sna.snaprkit.utils.GeoManager.GeoListener;
import pr.sna.snaprkit.utils.NetworkUtils;
import pr.sna.snaprkit.utils.UrlUtils;
import pr.sna.snaprkit.utils.webview.WebViewClientEx;
import pr.sna.snaprkitfx.SnaprImageEditFragmentActivity;
import pr.sna.snaprkitfx.SnaprImageEditFragmentActivity.LaunchMode;
import pr.sna.snaprkitfx.SnaprSetting;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
//import com.aviary.android.feather.FeatherActivity;
import android.widget.Button;

public class SnaprKitFragment extends Fragment implements OnSnaprFacebookLoginListener, OnSnaprFacebookPublishListener
{
	// Constants
	private static final int ACTION_REQUEST_FEATHER = 100;
	private static final String UPLOAD_MODE_WIFI_ONLY = "Wi-Fi Only";
	private static final String UPLOAD_MODE_ON = "On";
	
	// Members
	private View mView;
	private boolean mQueueUploadModeWifiOnly = false;
	private boolean mQueueUploadModeOn = true;
	private WebView mWebView;
	private ArrayList<UrlMapping> mActionMappings = new ArrayList<UrlMapping>();
	private String  mSnaprUserName;
	private String  mAccessToken;
	private String  mDisplayUserName;
	private PictureAcquisitionManager mPictureAcquisitionManager;
	private GeoManager mGeoManager; // Used for snapr://get_location
	private ConnectivityBroadcastReceiver mConnectivityReceiver;
	private UploadBroadcastReceiver mServiceCallbackReceiver;
	private boolean mMenuInitDone = false;
	private String mCurrentUrl = null;
	@SuppressWarnings("unused")
	private boolean mRestoredFromSavedState = false;
	private String mSharedPictureFileName = null;
	private SnaprKitListener mSnaprKitListener = null;
	private PictureAcquisitionListener mPictureAcquisitionListener;
	private Button mContextMenuButton;
	private String mContextMenuTitle = null;
	private String mContextMenuDestructiveItemLabel = null;
	private String mContextMenuCancelItemLabel = null;
	private String mContextMenuOtherItem1Label = null;
	private String mContextMenuOtherItem2Label = null;
	private String mContextMenuOtherItem3Label = null;
	private int mContextMenuActionId = -1;
	private double mLastPictureLatitude = 0;
	private double mLastPictureLongitude = 0;
	private Date mLastPictureDate = null;
	private String mLastDescription = null;
	private String mLastFoursquareVenueName = null;
	private String mLastFoursquareVenueId = null;
	private String mLastLocationName = null;
	
	private String mFilterPackPath;		// the location (under assets) where the filter packs will be loaded from
	private String mStickerPathPath;	// the location (under assets) where the sticker packs will be loaded from
	
	private Map<String, SnaprSetting> mSettings = new HashMap<String, SnaprSetting>();
	private Intent mPendingIntent;
	
	private Handler mHandler = new Handler();
	
	private Runnable mUpdateQueueSettings = new Runnable()
	{
		@Override
		public void run()
		{
			mWebView.loadUrl("javascript:queue_settings('" + getUploadModeString(mQueueUploadModeWifiOnly) + "', " + (!mQueueUploadModeOn) + ");");
	    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": UpdateQueueSettingsRunnable set JavaScript queue settings to Wifi-only " + mQueueUploadModeWifiOnly + " and upload mode on " + (!mQueueUploadModeOn));
		}
	};
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
    	// Save UI state changes to the savedInstanceState.
    	// This bundle will be passed to onCreate if the process is
    	// killed and restarted.
		
		// Save the WebViewActivity members
		outState.putBoolean("mMenuInitDone", mMenuInitDone);
		outState.putDouble("mLastPictureLatitude", mLastPictureLatitude);
		outState.putDouble("mLastPictureLongitude", mLastPictureLongitude);
		outState.putLong("mLastPictureDate", (mLastPictureDate == null)?0:mLastPictureDate.getTime());
		outState.putString("mCurrentUrl", mCurrentUrl);
		
		// Save the CameraManager members
		int imageSource = mPictureAcquisitionManager.getImageSource();
		outState.putInt("mImageSource", imageSource);
		
		Uri imageUri = mPictureAcquisitionManager.getImageUri();
		String imagePath = UrlUtils.imageUri2Path(imageUri);
		outState.putString("mImagePath", imagePath);
		
		Location location = mPictureAcquisitionManager.getLocation();
		outState.putDouble("mLatitude", (location!=null)?location.getLatitude():-1);
		outState.putDouble("mLongitude", (location!=null)?location.getLongitude():-1);
		
		outState.putBoolean("mIsActive", mPictureAcquisitionManager.isActive());
		outState.putBoolean("mIsGeolocationActive", mPictureAcquisitionManager.isGeolocationActive());
		
		outState.putBoolean("mNetworkProviderEnabled", mPictureAcquisitionManager.getNetworkProviderEnabled());
		outState.putBoolean("mGpsProviderEnabled", mPictureAcquisitionManager.getGpsProviderEnabled());
		outState.putBoolean("mWifiProviderEnabled", mPictureAcquisitionManager.getWifiProviderEnabled());
		
		outState.putLong("mPhotoTimestamp", mPictureAcquisitionManager.getPhotoTimestamp());
		
		outState.putString("mUserData", mPictureAcquisitionManager.getUserData());
		
		outState.putString("mStickerPathPath", mStickerPathPath);
		outState.putString("mFilterPackPath", mFilterPackPath);
		
		// Facebook session
		Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
        
        // Facebook status listener
        mStatusListener.saveStatus(outState);
		
		// Save the WebView history
		mWebView.saveState(outState);
	}	

	@Override public void onDestroy() {
		super.onDestroy();
		closeBroadcastReceivers();
	}
	
	
	
	@Override
	public void onStart()
	{
		super.onStart();
		Session.getActiveSession().addCallback(mStatusListener);
	}

	@Override
	public void onStop()
	{
		super.onStop();
		Session.getActiveSession().removeCallback(mStatusListener);
	}

	@Override
	public void onPause()
	{
		// Call base implementation
		super.onPause();
		
		// Shut down broadcast receivers
		closeBroadcastReceivers();
		
		// Save the queue settings
		saveQueueSettings();
	}

	@Override
	public void onResume()
	{
		// Call base implementation
		super.onResume();
		
    	// Initialize the connectivity and upload broadcast receivers
    	initBroadcastReceivers();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		if (mView == null)
		{
			// Inflate view
			mView = inflater.inflate(R.layout.snaprkit_main, container, false);
			
	        // Prepare the webview
			initWebView(mView, savedInstanceState);
			
			// Prepare the dummy button for context menu
			mContextMenuButton = (Button)mView.findViewById(R.id.buttonContextMenu);
			registerForContextMenu(mContextMenuButton);
		}
		else
		{
			ViewGroup parent = (ViewGroup)mView.getParent();
			parent.removeView(mView);
		}
		
		return mView;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		
		// Different handling for logging mode vs normal app
		if (Global.LOG_DISK)
		{
			inflater.inflate(R.menu.snaprkit_debug, menu);
		}
		else
		{
			super.onCreateOptionsMenu(menu, inflater);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Different handling for logging mode vs normal app
		if (Global.LOG_MODE)
		{
		    if (item.getItemId() == R.id.clearLogs)
			{
				clearLogs();
			}
			else if (item.getItemId() == R.id.sendCurrentLog)
			{
				sendLog(Global.FILE_LOG.getAbsolutePath());
			}
			else if (item.getItemId() == R.id.sendPreviousLog)
			{
				sendPreviousLog();
			}
		    return true;
		}
		else
		{
			return super.onOptionsItemSelected(item);
		}
	}

	private PictureAcquisitionListener getPictureAcquisitionListener() {
		if (mPictureAcquisitionListener != null) return mPictureAcquisitionListener;
		mPictureAcquisitionListener = new PictureAcquisitionListener()
	    {
			@Override
			public void onPictureAcquired(Context context, String fileName, int dataSource, double latitude, double longitude, long timeStamp)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + " with filename " + fileName);
				
				// Save the latitude and longitude
				mLastPictureLatitude = latitude;
				mLastPictureLongitude = longitude;
				mLastPictureDate = new Date();
				
				// Get settings from config
				float imageAspectRatio = pr.sna.snaprkit.utils.Configuration.getInstance().getImageAspectRatio();
				
				// Display pic options page
				if (fileName != null)
				{
					// Process differently based on data source
					if (dataSource == PictureAcquisitionManager.PICTURE_SOURCE_CAMERA)
					{
						// Check if we should use a picture editor (FX module or Aviary)
						if (Global.USE_FX_MODULE)
						{
							// For camera, we already have a copy of the picture, so edit in place
							SnaprImageEditFragmentActivity.Builder builder = new SnaprImageEditFragmentActivity.Builder(new File(fileName), new File(fileName), true, timeStamp);
							builder.setStickerPackPath(mStickerPathPath);
							builder.setFilterPackPath(mFilterPackPath);
							builder.setSettings(mSettings);
							builder.setImageAspectRatio(imageAspectRatio);
							String launchMode = getLaunchMode(mPictureAcquisitionManager.getUserData());
							if (launchMode != null) builder.setLaunchMode(Global.LAUNCH_MODE_STICKERS.equals(launchMode)?LaunchMode.STICKERS:LaunchMode.FILTERS);
							displayPhotoEdit(getActivity(), builder);
						}
						else
						{
							// Display the share options
							displayPhotoShareOptions(fileName, mLastPictureLatitude, mLastPictureLongitude, null, null, null, null, mPictureAcquisitionManager.getUserData());
						}
					}
					else
					{
						// Check if we should use a picture editor (FX module or Aviary)
						if (Global.USE_FX_MODULE)
						{
							// For gallery provide output filename so module makes copy of source picture
							File outputFile = new File(FileUtils.getDCIMCameraDirectory() + "/" + CameraUtils.getPictureName());
							SnaprImageEditFragmentActivity.Builder builder = new SnaprImageEditFragmentActivity.Builder(new File(fileName), outputFile);
							builder.setStickerPackPath(mStickerPathPath);
							builder.setFilterPackPath(mFilterPackPath);
							builder.setSettings(mSettings);
							builder.setImageAspectRatio(imageAspectRatio);
							String launchMode = getLaunchMode(mPictureAcquisitionManager.getUserData());
							if (launchMode != null) builder.setLaunchMode(Global.LAUNCH_MODE_STICKERS.equals(launchMode)?LaunchMode.STICKERS:LaunchMode.FILTERS);
							displayPhotoEdit(getActivity(), builder);
						}
						else
						{
							// Display the share options
							displayPhotoShareOptions(fileName, mLastPictureLatitude, mLastPictureLongitude, null, null, null, null, mPictureAcquisitionManager.getUserData());
						}
					}
				}
				else
				{
					if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Got filename NULL");
					if (mWebView.canGoBack()) mWebView.goBack();
				}
				
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
			}
	    };
	    return mPictureAcquisitionListener;
	}
	
	// Read the launch mode from the camera / gallery params
	private String getLaunchMode(String encodedParams)
	{
		// Get calling arguments and determine if we must set launch mode
		String url = "snapr://dummy/?" + mPictureAcquisitionManager.getUserData();
		Uri uri = Uri.parse(url);
		String launchMode = uri.getQueryParameter(Global.PARAM_FX_MOD);
		return launchMode;
	}
	
    // GeoListener to support snapr://get_location
    private GeoListener mGeoListener = new GeoListener()
    {
		@Override
		public void onLocation(Location location, int caller)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + " from caller " + caller);
			
			// Display the location in log    	
	        Double latitude = location.getLatitude();
	        Double longitude = location.getLongitude();
	        Float  accuracy = location.getAccuracy();
	        if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Got location: Provider: " + location.getProvider() + " Accuracy: " + accuracy.toString() + " Latitude: " + latitude.toString() + " Longitude" + longitude.toString());
	        //displayLocation(location);
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod() + " from caller " + caller);
		}

		@Override
		public void onTerminate(Location location, int caller)
		{
			// We don't need to do anything on termination
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + " from caller " + caller);
			
			if (caller == Global.GEOLOCATION_GET_LOCATION)
			{
				onFinishedSnaprGetLocation(location);
			}
			else
			{
				onFinishedPhotoShareLocation(location);
			}
			
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod() + " from caller " + caller);
		}

		@Override
		public void onTimeOut(Location location, int caller)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + " from caller " + caller);
			
			if (caller == Global.GEOLOCATION_GET_LOCATION)
			{
				onFinishedSnaprGetLocation(location);
			}
			else
			{
				onFinishedPhotoShareLocation(location);
			}
			
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod() + " from caller " + caller);
		}
    };

	public class ConnectivityBroadcastReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
			
			// Get network info for item that just completed
			NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			
			// Check result
			if(info != null)
			{
				// Get connectivity state
				boolean connected = (info.getState() == NetworkInfo.State.CONNECTED);
	    		
	    		// Is the network connected?
	    		if (connected)
	    		{
	    			// Is the queue set to Wifi only?
	    			if (mQueueUploadModeWifiOnly)
	    			{
	    				// Check network type
	    				if(info.getType() == ConnectivityManager.TYPE_WIFI)
	    				{
	    					// Update the settings
	    					if (mMenuInitDone)
		    				{
	    						updateQueueSettings(true, mQueueUploadModeWifiOnly);
		    				}
	    				}
	    			}
	    			else
	    			{
	    				// Update settings
	    				if (mMenuInitDone)
	    				{
	    					updateQueueSettings(true, mQueueUploadModeWifiOnly);
	    				}
	    			}
	    		}			
			}
			
			if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
		}
    }
    
	public class UploadBroadcastReceiver extends BroadcastReceiver
	{		
		@SuppressLint("UseValueOf")
		@Override
		public void onReceive(Context context, Intent intent)
		{
			// Log
			if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + " with intent action " + intent.getAction().toString());
			
			if (intent.getAction().equals(Global.INTENT_BROADCAST_UPLOAD))
			{
				// Get data from the intent
				int broadcast = intent.getIntExtra(Global.PARAM_ACTION, -1);
				
				// Log
				if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received boadcast with id " + broadcast);
				
				// See the type of broadcast
				if (broadcast == Global.BROADCAST_UPLOAD_STARTED ||
					broadcast == Global.BROADCAST_UPLOAD_CANCELED ||
					broadcast == Global.BROADCAST_UPLOAD_COMPLETED)
				{
					// Log
					if (broadcast == Global.BROADCAST_UPLOAD_STARTED)
					{
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for upload started");	
					}
					else if (broadcast == Global.BROADCAST_UPLOAD_CANCELED)
					{
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for upload cancelled");
					}
					else if (broadcast == Global.BROADCAST_UPLOAD_COMPLETED)
					{
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for upload completed");
					}
					
					// Get num uploads
					int numUploads = intent.getIntExtra(Global.PARAM_NUM_UPLOADS, -1);
					
					// Log
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Number of uploads is " + numUploads);
					
					// Check number of uploads
					if (numUploads == -1)
					{
						// Log 
						if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Error - Upload intent did not contain the number of uploads");
					}
					else
					{						
						// Call JavaScript
						mWebView.loadUrl("javascript:upload_count(" + new Integer(numUploads).toString() + ");");
						
						// Log
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Called JavaScript with upload count " + numUploads);
					}
					
					// For upload cancelled we need to call the upload_cancelled JS function
					if (broadcast == Global.BROADCAST_UPLOAD_CANCELED)
					{
						String localId = intent.getStringExtra(Global.PARAM_LOCAL_ID);
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Sending upload_cancelled('" + localId + "')");
						mWebView.loadUrl("javascript:upload_cancelled('" + localId + "');");
					}
					
					// For upload completed call the upload_complete function
					// For upload completed, also see if we need to notify of signups
					if (broadcast == Global.BROADCAST_UPLOAD_COMPLETED)
					{
						// Call upload_completed JS
						String localId = intent.getStringExtra(Global.PARAM_LOCAL_ID);
						String snaprId = intent.getStringExtra(Global.PARAM_SNAPR_ID);
						String signupsNeeded = intent.getStringExtra(Global.PARAM_SIGNUPS_NEEDED);
						String uploadCompletedParams = buildUploadCompletedParameters(localId, snaprId, signupsNeeded);
						//String uploadCompletedParams = "{localId:'" + localId + "', snapr_id:'" + snaprId + "', to_link:['facebook']}";
						
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Sending upload_completed(" + UrlUtils.jsEscape(uploadCompletedParams) + ")");
						mWebView.loadUrl("javascript:upload_completed(" + UrlUtils.jsEscape(uploadCompletedParams) + ")");
					}
				}
				else if (broadcast == Global.BROADCAST_UPLOAD_PROGRESS)
				{
					// Log
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for upload progress...");
					
					// Check if we must show progress
					/*
					if (mSendUploadProgress == true)
					{
					*/
						// Extract intent data
						String jsonData = intent.getStringExtra(Global.PARAM_JSON_DATA);
						
						// Create JSON for upload progress
						try
						{
							// Get queue size
							JSONObject json = new JSONObject(jsonData);
							JSONArray uploads = json.getJSONArray("uploads");
							int numUploads = uploads.length();
														
							// Call JavaScript
							if (numUploads > 0)
							{
								if (Global.LOG_MODE)
								{
									Global.log( " -> " + Global.getCurrentMethod() + ": Outputting progress JSON...");
									Global.log( " -> " + Global.getCurrentMethod() + ": javascript:upload_progress('" + UrlUtils.jsEscape(jsonData) + "', 'json_text');");
								}
								
								mWebView.loadUrl("javascript:upload_progress('" + UrlUtils.jsEscape(jsonData) + "','json_text');");
							}
						}
						catch(Exception e)
						{
							// Log 
							if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Error - Failed to create upoad progress JSON and send it to JavaScript");
						}
					/*
					}
					else
					{
						if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Not showing upload progress...");
					}
					*/
				}
				else if (broadcast == Global.BROADCAST_UPLOAD_STATUS)
				{					
					// Log
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for upload status");
					
					// Log
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Updating queue based on upload status");

					// Create JSON for upload progress
					try
					{
					
						// Extract intent data
						String jsonData = intent.getStringExtra(Global.PARAM_JSON_DATA);
						
						// Get queue size
						JSONObject json = new JSONObject(jsonData);
						JSONArray uploads = json.getJSONArray("uploads");
						int numUploads = uploads.length();
													
						// Call JavaScript
						if (numUploads > 0)
						{
							// Update the progress
							if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Outputting progress JSON..." + jsonData);
							mWebView.loadUrl("javascript:upload_count(" + numUploads + ");");
							mWebView.loadUrl("javascript:upload_progress('" + UrlUtils.jsEscape(jsonData) + "','json_text');");
							mWebView.loadUrl("javascript:upload_count(" + numUploads + ");");
						}
					}
					catch (Exception e)
					{
						if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with error " + e);
					}
				}
				else if (broadcast == Global.BROADCAST_UPLOAD_ERROR)
				{
					// Extract params
					String errorMessage = "";
					String localId = intent.getStringExtra(Global.PARAM_LOCAL_ID);
					Exception exception = (Exception) intent.getSerializableExtra(Global.PARAM_EXCEPTION);
					
					// Take action based on exception type
					if (exception instanceof SnaprApiException)
					{
						String errorType = ((SnaprApiException) exception).getType();
						errorMessage = ((SnaprApiException) exception).getMessage();
						
						if (errorType != null)
						{							
							if (errorType.equals("authentication.authentication_required"))
							{
								// Clear queue
								clearQueue();
								
								// Update the status
								updateQueueStatus();
								
								// Invalidate credentials
								clearUserInfo();
								
								// Logout
								mWebView.loadUrl("javascript:logout();");
								
								// Override message 
								errorMessage = getString(R.string.snaprkit_error_upload_invalid_login);
								
								// Show message
								//showUploadError(errorMessage);
							}
							else if (errorType.equals("validation.duplicate_upload"))
							{
								// Cancel the upload
								cancelUpload(localId);
								updateQueueStatus();
								
								// Override message
								errorMessage = getString(R.string.snaprkit_error_upload_duplicate_image);
								
								// Show message
								//showUploadError(errorMessage);
							}
							else if (errorType.equals("validation.corrupt_file"))
							{
								// Cancel the upload
								cancelUpload(localId);
								updateQueueStatus();
								
								// Override message
								errorMessage = getString(R.string.snaprkit_error_upload_corrupt_image);
						        
								// Show message
								//showUploadError(errorMessage);
							}
							else
							{
								// Determine how to handle error
								// For apps which don't display a queue, we cancel the upload
								// For apps which have a queue, we pause the queue
								if (pr.sna.snaprkit.utils.Configuration.getInstance().getAutoClearFailedUploads())
								{
									// Cancel the upload
									cancelUpload(localId);
									updateQueueStatus();
								}
								else
								{
									// Set the queue to paused
									updateQueueSettings(false, mQueueUploadModeWifiOnly);
								}
								
								// Display alert dialog
								//showUploadError(errorMessage);
							}
						}
					}
					else if (exception instanceof HttpResponseException)
					{
						// Set the queue to paused
						updateQueueSettings(false, mQueueUploadModeWifiOnly);
						
						// Get the status code
						int httpStatusCode = ((HttpResponseException) exception).getStatusCode(); 
						
						// Override message
						errorMessage = (httpStatusCode >= 500)?getString(R.string.snaprkit_error_upload_server_error):
								getString(R.string.snaprkit_error_upload_connect);
						
						// Show message
						//showUploadError(errorMessage);
					}
					else if (exception instanceof ConnectTimeoutException)
					{
						// Set the queue to paused
						updateQueueSettings(false, mQueueUploadModeWifiOnly);
						
						// Override message
						errorMessage = getString(R.string.snaprkit_error_upload_connect_timeout);
						
						// Show message
						//showUploadError(errorMessage);
					}
					else if (exception instanceof IOException)
					{
						// Set the queue to paused
						updateQueueSettings(false, mQueueUploadModeWifiOnly);
						
						// Other types of IOExceptions (HttpResponseException and ConnectionTimeOutException) handled above
						errorMessage = getString(R.string.snaprkit_error_upload_connect);
						
						// Show message
						//showUploadError(errorMessage);
					}
					else
					{
						// Another type of error
						
						// Set the queue to paused
						updateQueueSettings(false, mQueueUploadModeWifiOnly);
						
						// Get error message
						errorMessage = getString(R.string.snaprkit_error_upload);
						
						// Show message
						//showUploadError(errorMessage);
					}
					
					// Call upload_failed
					mWebView.loadUrl("javascript:upload_failed('" + localId + "', '" + UrlUtils.jsEscape(errorMessage) + "');");
				}
				else
				{
					// Log
					if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Received broadcast for an unknown event");
				}
			}
			
			// Log
			if (Global.LOG_MODE) Global.log( " <- " + Global.getCurrentMethod());
		}
	}
	
	private String buildUploadCompletedParameters(String localId, String snaprId, String signupsNeeded)
	{
		String[] signups = signupsNeeded.split(",");
		
		JSONObject uploadCompletedParameters = new JSONObject();
		try
		{
			uploadCompletedParameters.put(Global.PARAM_LOCAL_ID, localId);
			uploadCompletedParameters.put(Global.PARAM_SNAPR_ID, snaprId);
			JSONArray signupsArray = new JSONArray();
			for (String s: signups)
			{
				signupsArray.put(s);
			}
			uploadCompletedParameters.put(Global.PARAM_TO_LINK, signupsArray);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			return "";
		}

		return uploadCompletedParameters.toString();
	}
	
	/**
     * Function called from onPictureAcquired or after we return from Aviary 
     */
	private void displayPhotoEdit(Context context, SnaprImageEditFragmentActivity.Builder builder)
	{
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
		if (Global.USE_FX_MODULE)
		{
			Intent intent = SnaprImageEditFragmentActivity.getIntentForStartActivity(context, builder); 
			if (getActivity() != null) startActivityForResult(intent, SnaprImageEditFragmentActivity.EDIT_IMAGE);
			else mPendingIntent = intent;
		}
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	}
	
	/**
     * Function called from onPictureAcquired 
     */
	private void displayPhotoShareOptions(String fileName, double latitude, double longitude, String description, String foursquareVenueId, String foursquareVenueName, String locationName, String extraParams)
	{
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
		
		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": After pic taken queueOn " + mQueueUploadModeOn + " queueWifi " + mQueueUploadModeWifiOnly);
		
		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Got filename " + fileName);
		
		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Got extraParams " + extraParams);
		
		String redirectUrl = getSnaprUrl(UrlUtils.getFullLocalUrl(Global.URL_UPLOAD));
		String url = getSharePictureUrl(fileName, latitude, longitude, redirectUrl, description, foursquareVenueId, foursquareVenueName, locationName, extraParams);
		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Redirecting to " + url);
		mWebView.loadUrl(url);
		
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	}
	
    /**
     * Function called after we receive location for snapr://get_location via 
     * either termination or timeout 
     */
    private void onFinishedSnaprGetLocation(Location location)
    {	
		// Log location
		if (location != null)
		{
			// Log
			double latitude = location.getLatitude();
			double longitude = location.getLongitude();
			float accuracy = location.getAccuracy();
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Terminated with location " + latitude + ", " + longitude + " and accuracy " + accuracy);
			
			// Communicate with JavaScript
			mWebView.loadUrl("javascript:set_location(" + latitude + ", " + longitude + ")");
		}
		else
		{
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Terminated with location null");
			
			// Communicate with JavaScript
			mWebView.loadUrl("javascript:location_error('" + R.string.snaprkit_error_location_retrieval_failure + "')");
		}
		
		// Close the pending dialog
		mGeoManager.cancelLocationPendingDialog();
    }
    
    private void onFinishedPhotoShareLocation(Location location)
    {
    	// Declare
    	double latitude = 0;
    	double longitude = 0;
    	
    	if (location != null)
		{
			// Log
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			float accuracy = location.getAccuracy();
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Terminated with location " + latitude + ", " + longitude + " and accuracy " + accuracy);
			
			// Get the shared picture data
			SharedPictureInfo info = getSharedPictureInfo();
			String fileName = info.getFileName();
			
			// Get the URL
	        String url = getSharePictureUrl(fileName, latitude, longitude, null, null, null, null, null, mPictureAcquisitionManager.getUserData());
	        
	        // Go to URL
	        mWebView.loadUrl(url);
		}
		else
		{
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Terminated with location null");
		}
    	
    	// Close the pending dialog
		mGeoManager.cancelLocationPendingDialog();
    }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		super.onActivityResult(requestCode, resultCode, data);
		
		// Facebook processing
		Session.getActiveSession().onActivityResult(getActivity(), requestCode, resultCode, data);
		
		switch (requestCode)
		{
		    case PictureAcquisitionManager.TAKE_PICTURE:
		    {
		    	// Process result
		    	mPictureAcquisitionManager.setPictureAcquisitionListener(getPictureAcquisitionListener());
		    	mPictureAcquisitionManager.onActivityResult(requestCode, resultCode, data);
		        
		        break;
		    }
		    
		    case PictureAcquisitionManager.GPS_SETTINGS_SCREEN:
		    {
		    	mPictureAcquisitionManager.setPictureAcquisitionListener(getPictureAcquisitionListener());
		    	mPictureAcquisitionManager.onActivityResult(requestCode, resultCode, data);
		    	
		    	break;
		    }
		    
		    case PictureAcquisitionManager.SELECT_PICTURE:
		    {
		    	// Process result
		    	mPictureAcquisitionManager.setPictureAcquisitionListener(getPictureAcquisitionListener());
		    	mPictureAcquisitionManager.onActivityResult(requestCode, resultCode, data);
		        
		        break;
		    }
		    
		    case WebViewExternalActivity.DISPLAY_EXTERNAL_URL:
		    {		    	
		    	if (resultCode == Activity.RESULT_OK)
		        {
		    		// Extract the info
		        	String url = data.getStringExtra(Global.PARAM_URL);
		        	
		        	// Log
		        	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Returned from external URL display with redirect URL " + url);
		        	
		        	// Navigate to page
		        	if (url != null)
		        	{
		        		Action action = getActionForUrl(url);
		        		action.run(url);
		        	}
		        }
		    	else
		    	{
		    		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Returned from external URL display with a non-OK error code");
		    	}
		    	break;
		    }
		    
		    // FX module photo edit complete
		    case SnaprImageEditFragmentActivity.EDIT_IMAGE:
		    {
		    	if (Global.USE_FX_MODULE)
		    	{	
			    	if (resultCode == Activity.RESULT_OK)
			        {
				    	// Get the filename
						String fileName = data.getStringExtra(SnaprImageEditFragmentActivity.EXTRA_FILEPATH);
						
						// Get the analytics
						ArrayList<String> analytics = data.getStringArrayListExtra(SnaprImageEditFragmentActivity.EXTRA_ANALYTICS);
						
						// Send analytics data out through listener
						if (mSnaprKitListener != null && analytics != null)
						{
							for (int i = 0; i< analytics.size(); i++)
							{
								String url = analytics.get(i);
								mSnaprKitListener.onSnaprKitParent(url);
							}
						}
						
						// Display the share options
						displayPhotoShareOptions(fileName, mLastPictureLatitude, mLastPictureLongitude, mLastDescription, mLastFoursquareVenueId, mLastFoursquareVenueName, mLastLocationName, mPictureAcquisitionManager.getUserData());
			        }
			    	else
			    	{
			    		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Returned from FX module with a non-OK error code");
			    		mWebView.loadUrl(getStartupUrl());
			    	}
		    	}
		    	break;
		    }
		    
		    case ACTION_REQUEST_FEATHER:
		    {
		    	// Enable Aviary Feather based on settings
		    	if (Global.USE_AVIARY_SDK)
		    	{
			    	// Get the URI
			    	String fileName = null;
			    	
			    	// Check activity result
			    	if (resultCode == Activity.RESULT_OK)
			    	{
			    		// Get the image uri
				    	Uri imageUri = data.getData();
			    		
				    	// Transform the URI to a filename
				    	fileName = UrlUtils.getRealPathFromURI(SnaprKitFragment.this.getActivity(), imageUri);
				    	
				    	// Re-tag the picture (due to Aviary dropping the latitude and longitude)
				    	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Retagging picture with latitude and longitude...");
				    	Location location = new Location("");
				    	location.setTime((mLastPictureDate!=null)?mLastPictureDate.getTime():0);
				    	location.setLatitude(mLastPictureLatitude);
				    	location.setLongitude(mLastPictureLongitude);
				    	CameraUtils.geotagPicture(fileName, location);
			    	}
			    	
			    	// Display the picture options
			    	if (fileName != null)
			    	{
			    		displayPhotoShareOptions(fileName, mLastPictureLatitude, mLastPictureLongitude, mLastDescription, mLastFoursquareVenueId, mLastFoursquareVenueName, mLastLocationName, mPictureAcquisitionManager.getUserData());
			    	}
			    	else
			    	{
			    		if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Got filename NULL");
			    	}
		    	}
		    	
		    	break;
		    }
	    }
		
	}
    
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// Handle screen orientation changes
		super.onConfigurationChanged(newConfig);
	}
	
    /** Called when the fragment is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {	
    	// Create regular suspects
    	super.onCreate(savedInstanceState);
    	
    	// Load the configuration
        pr.sna.snaprkit.utils.Configuration.init(SnaprKitFragment.this.getActivity());
        Global.LOG_MODE = pr.sna.snaprkit.utils.Configuration.getInstance().getLoggingEnabled();
        
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Set the options menu
    	if (Global.LOG_MODE) setHasOptionsMenu(true);
    	
    	// Retain instance
    	setRetainInstance(true);
    	
        // Log
        if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    }
    
    
    
	@Override public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);
    	
    	// Init rest of the app
        init(savedInstanceState);
        
        if (Global.USE_FX_MODULE && mPendingIntent != null)
        {
        	startActivityForResult(mPendingIntent, SnaprImageEditFragmentActivity.EDIT_IMAGE);
        	mPendingIntent = null;
        }
    }
    
	private boolean onBackPressed()
	{
		if (mWebView.canGoBack())
		{
			if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Calling JavaScript back() function...");
			mWebView.loadUrl("javascript:back();");
			return true;
		}
		else
		{
			return false;
		}
	}

	// Checks whether the user info was loaded
    private boolean haveUserInfo()
    {
    	return UserInfoUtils.haveUserInfo(mDisplayUserName, mSnaprUserName, mAccessToken);
    }
    
    // Load shared preferences into class members
    private void loadQueueSettings()
    {
    	// Get the shared preferences object
        SharedPreferences prefs = getContext().getSharedPreferences(Global.SNAPR_PREFERENCES, Context.MODE_PRIVATE);
        
        // Load queue preferences
        mQueueUploadModeOn = prefs.getBoolean(Global.SNAPR_PREFERENCES_QUEUE_ON, true);
        mQueueUploadModeWifiOnly = prefs.getBoolean(Global.SNAPR_PREFERENCES_QUEUE_WIFI_ONLY, false);
    }
    
    private void saveQueueSettings()
    {
    	// Save queue preferences
		SharedPreferences prefs = getContext().getSharedPreferences(Global.SNAPR_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putBoolean(Global.SNAPR_PREFERENCES_QUEUE_ON, mQueueUploadModeOn);
		prefEditor.putBoolean(Global.SNAPR_PREFERENCES_QUEUE_WIFI_ONLY, mQueueUploadModeWifiOnly);
		prefEditor.commit();
    }
    
    /**
     * Create a SNAPR URL with the required login parameters
     * @param baseUrl Base page URL
     * @return Returns an URL based on the base URL and containing 
     * the login parameters
     */
    private String getSnaprUrl(String baseUrl)
    {
    	// Declare
        String url;
    	Vector<BasicNameValuePair> params;
        
        // Add login parameters
    	params = new Vector<BasicNameValuePair>();
    	params.add(new BasicNameValuePair(Global.PARAM_SNAPR_USER, mSnaprUserName));
    	params.add(new BasicNameValuePair(Global.PARAM_ACCESS_TOKEN, mAccessToken));
        
        // Create the URL
        url = UrlUtils.createUrl(baseUrl, params, false);
        
        // Convert to AJAX URL and return
        return UrlUtils.ajaxUrl(url);
    }
    
    /**
     * Build the startup URL
     * return Returns the startup URL
     */
    private String getStartupUrl()
    {
        // Declare
        String url;
    	Vector<BasicNameValuePair> params;
        
    	// Add appmode = android every time
        params = new Vector<BasicNameValuePair>();
        params.add(new BasicNameValuePair(Global.PARAM_APPMODE, "android"));
        params.add(new BasicNameValuePair(Global.PARAM_ENVIRONMENT, pr.sna.snaprkit.utils.Configuration.getInstance().getEnvironment()));
        params.add(new BasicNameValuePair(Global.PARAM_LANGUAGE, LocalizationUtils.getLanguageCode()));
        params.add(new BasicNameValuePair(Global.PARAM_LOCALE, LocalizationUtils.getLocaleCode()));
        
        // Customize some parameters based on logged in status
        if(haveUserInfo())
		{
        	// We have user info, so create URL that performs login
        	params.add(new BasicNameValuePair(Global.PARAM_DISPLAY_USERNAME, mDisplayUserName));
        	params.add(new BasicNameValuePair(Global.PARAM_SNAPR_USER, mSnaprUserName));
        	params.add(new BasicNameValuePair(Global.PARAM_ACCESS_TOKEN, mAccessToken));
		}
        else
        {
        	// We have no username and password, so create URL that indicates new user
        	params.add(new BasicNameValuePair(Global.PARAM_NEW_USER, "true"));
        }
        
        // Create the URL
        url = UrlUtils.createUrl(UrlUtils.getFullLocalUrl(Global.URL_MENU), params, false);
        
        if (Global.LOG_MODE) Global.log("URL1 is " + url + " and URL2 is " + UrlUtils.ajaxUrl(url));
        
        // Convert to AJAX URL and return
        return UrlUtils.ajaxUrl(url);
    }
    
    /**
     * Build the picture sharing URL
     * @return Returns the picture sharing URL
     */
    @SuppressLint({ "UseValueOf", "UseValueOf" })
	private String getSharePictureUrl(String imageName, double latitude, double longitude, String redirectUrl, String description, String foursquareVenueId, String foursquareVenueName, String locationName, String extraParams)
    {
        // Declare
        String url;
    	Vector<BasicNameValuePair> params;
        
    	// Add appmode = android every time
        params = new Vector<BasicNameValuePair>();
        params.add(new BasicNameValuePair(Global.PARAM_APPMODE, "android"));
        params.add(new BasicNameValuePair(Global.PARAM_ENVIRONMENT, pr.sna.snaprkit.utils.Configuration.getInstance().getEnvironment()));
        
        /*
        // Customize some parameters based on logged in status
        if(haveUserInfo())
		{
        	// We have user info, so create URL that performs login
        	params.add(new BasicNameValuePair(Global.PARAM_DISPLAY_USERNAME, mDisplayUserName));
        	params.add(new BasicNameValuePair(Global.PARAM_SNAPR_USER, mSnaprUserName));
        	params.add(new BasicNameValuePair(Global.PARAM_ACCESS_TOKEN, mAccessToken));
		}
        else
        {
        	// We have no username and password, so create URL that indicates new user
        	params.add(new BasicNameValuePair(Global.PARAM_NEW_USER, "true"));
        }
        */
        
        // Add the image name
        if(imageName.startsWith("file:///") == false)
        {
        	imageName = "file:///" + imageName;
        }
        params.add(new BasicNameValuePair(Global.PARAM_PHOTO_PATH, imageName));
        
        // Add latitude and longitude
        if((latitude != 0) && (longitude != 0))
        {
        	params.add(new BasicNameValuePair(Global.PARAM_LATITUDE, new Double(latitude).toString()));
        	params.add(new BasicNameValuePair(Global.PARAM_LONGITUDE, new Double(longitude).toString()));
        }
        
        // Add redirect URL
        if ((redirectUrl != null) && (redirectUrl.length() > 0))
        {
        	params.add(new BasicNameValuePair(Global.PARAM_REDIRECT_URL, redirectUrl));
        }
        
        // Add description
        if ((description != null) && (description.length() > 0))
        {
        	params.add(new BasicNameValuePair(Global.PARAM_DESCRIPTION, description));
        }
        
        // Add Foursquare venue name
        if ((foursquareVenueName != null) && (foursquareVenueName.length() > 0))
        {
        	params.add(new BasicNameValuePair(Global.PARAM_FOURSQUARE_VENUE_NAME, foursquareVenueName));
        }
        
        // Add Foursquare venue id
        if ((foursquareVenueId != null) && (foursquareVenueId.length() > 0))
        {
        	params.add(new BasicNameValuePair(Global.PARAM_FOURSQUARE_VENUE_ID, foursquareVenueId));
        }
        
        // Add location name
        if ((locationName != null) && (locationName.length() > 0))
        {
        	params.add(new BasicNameValuePair(Global.PARAM_LOCATION, locationName));
        }
        
        // Add additional params
        if (extraParams != null)
        {
        	UrlUtils.appendParams(params, extraParams);
        }
        
        // Create the URL
        String val = UrlUtils.getFullLocalUrl(Global.URL_PHOTO_SHARE);
        url = UrlUtils.createUrl(val, params, false);
        
        // Convert to AJAX URL and return
        return UrlUtils.ajaxUrl(url);
    }    

    // The action performed for snaprkit-parent:// URLs
    private Action snaprKitParentAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    		// Send info through listener
    		if (mSnaprKitListener !=null) mSnaprKitListener.onSnaprKitParent(url);
    	}
    };
    
    // The action performed for login
    private Action loginAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    		// Login URL - get user info and redirect to startup URL
			
			// Convert AJAX URL to normal URL - necessary for Uri parsing to work
			String normalUrl = UrlUtils.normalUrl(url);
			
			// Parse the URL parameters
			Uri uri = Uri.parse(normalUrl);
			mDisplayUserName = UrlUtils.getQueryParameter(uri, Global.PARAM_DISPLAY_USERNAME);
			mSnaprUserName = UrlUtils.getQueryParameter(uri, Global.PARAM_SNAPR_USER);
			mAccessToken = UrlUtils.getQueryParameter(uri, Global.PARAM_ACCESS_TOKEN);
			
			// Check if we have proper user info now that we loaded info from the URL
			if(haveUserInfo())
			{
				// Save user info
				UserInfoUtils.saveUserInfo(getContext(), mDisplayUserName, mSnaprUserName, mAccessToken);
			}
    	}
    };

    // The action performed for logout
    private Action logoutAction = new Action() {
    	@Override
    	public void run(String url)
    	{
    		// Clear the locally stored user info
    		UserInfoUtils.clearUserInfo(getContext());
    		
    		// Clear the FB native session
    		String facebookAppId = pr.sna.snaprkit.utils.Configuration.getInstance().getFacebookAppId();
    		if (facebookAppId != null && facebookAppId.length() > 0 )
    		{
    			Session session = Session.getActiveSession();
    			if (session != null && !session.isClosed())
    			{
    				session.closeAndClearTokenInformation();
    			}
    		}
    	}
    };
    
    protected void runCameraAction() {
    	cameraAction.run("snapr://camera");
    }
    
    // The action performed for camera shoot
    private Action cameraAction = new Action() {
    	@Override
    	public void run(String url)
    	{
			if (Global.LOG_MODE) Global.log("-> cameraAction()");
			
			// Start the camera and take picture
			if ((mPictureAcquisitionManager != null) && (mPictureAcquisitionManager.isActive() == false))
			{
				Uri launchUri = Uri.parse(url);
				mPictureAcquisitionManager.setUserData(launchUri.getEncodedQuery());
				mPictureAcquisitionManager.acquirePicture(PictureAcquisitionManager.PICTURE_SOURCE_CAMERA, getPictureAcquisitionListener());			
			}
			else
			{
				if (Global.LOG_MODE) Global.log("-> cameraAction(): Not opening camera because camera already open!");
			}
    	}
    };

    protected void runGalleryAction() {
    	photoGalleryAction.run("snapr://photo-library");
    }
    
    // The action performed for photo selection from gallery
    private Action photoGalleryAction = new Action() {
    	@Override
    	public void run(String url)
    	{
			if (Global.LOG_MODE) Global.log("-> photoGalleryAction()");

			// Start the gallery and let user select picture
			if ((mPictureAcquisitionManager != null) && (mPictureAcquisitionManager.isActive() == false))
			{
				Uri launchUri = Uri.parse(url);
				mPictureAcquisitionManager.setUserData(launchUri.getEncodedQuery());
				mPictureAcquisitionManager.acquirePicture(PictureAcquisitionManager.PICTURE_SOURCE_GALLERY, getPictureAcquisitionListener());			
			}
    	}
    };
    
    // The action performed for snapr://redirect
    private Action redirectAction = new Action() {
    	@Override
    	public void run(String url)
    	{
			if (Global.LOG_MODE) Global.log(Global.TAG, "redirectAction(): Started with URL " + url);
			
			String redirectUrl=UrlUtils.createRedirectUrlFromSnaprRedirectUrl(url, null);
			
			// Load our URL
			if (Global.LOG_MODE) Global.log("redirectAction(): Redirecting to " + redirectUrl);
			if (redirectUrl != null) mWebView.loadUrl(redirectUrl);
    	}
    };
    
    // Creates an upload localId based on current date time
    @SuppressLint("UseValueOf")
	private String getUploadLocalId()
    {
    	Random random = new Random();
    	int randomInt = random.nextInt();
    	if (randomInt < 0) randomInt = randomInt * -1;
    	return new Integer(randomInt).toString();
    }

    // The action performed for snapr://upload
    private Action uploadAction = new Action() {
    	@SuppressLint("SimpleDateFormat")
		@Override
    	public void run(String url)
    	{			
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Received URL " + url);
			
			// Extract the parameters from the query
			Uri uri = Uri.parse(url);
			String localId = UrlUtils.getQueryParameter(uri, Global.PARAM_LOCAL_ID);
			String description = UrlUtils.getQueryParameter(uri, Global.PARAM_DESCRIPTION);
			String status = UrlUtils.getQueryParameter(uri, Global.PARAM_STATUS);
			String tweet = UrlUtils.getQueryParameter(uri, Global.PARAM_TWEET);
			String facebookFeed = UrlUtils.getQueryParameter(uri, Global.PARAM_FACEBOOK_FEED);
			String facebookAlbum = UrlUtils.getQueryParameter(uri, Global.PARAM_FACEBOOK_ALBUM);
			String facebookAlbumName = UrlUtils.getQueryParameter(uri, Global.PARAM_FACEBOOK_ALBUM_NAME);
			String tumblr = UrlUtils.getQueryParameter(uri, Global.PARAM_TUMBLR);
			String foursquareCheckin = UrlUtils.getQueryParameter(uri, Global.PARAM_FOURSQUARE_CHECKIN);
			String photo = UrlUtils.getQueryParameter(uri, Global.PARAM_PHOTO);
			String redirectUrl = UrlUtils.getQueryParameter(uri, Global.PARAM_REDIRECT_URL);
			String foursquareVenueId = UrlUtils.getQueryParameter(uri, Global.PARAM_FOURSQUARE_VENUE);
			String appGroup = UrlUtils.getQueryParameter(uri, Global.PARAM_APP_GROUP);
			String publicGroup = UrlUtils.getQueryParameter(uri, Global.PARAM_PUBLIC_GROUP);
			String uploadParams = uri.getEncodedQuery();
			
			if (Global.LOG_MODE)
			{
				Global.log("Extracted the following params from the snapr://upload URL:");
				Global.log("localId: " + localId);
				Global.log("description: " + description);
				Global.log("status: " + status);
				Global.log("tweet: " + tweet);
				Global.log("facebookFeed: " + facebookFeed);
				Global.log("facebookAlbum: " + facebookAlbum);
				Global.log("facebookAlbumName: " + facebookAlbumName);
				Global.log("tumblr: " + tumblr);
				Global.log("foursquareCheckin: " + foursquareCheckin);
				Global.log("photo: " + photo);
				Global.log("redirectUrl: " + redirectUrl);
				Global.log("foursquareVenueId: " + foursquareVenueId);
				Global.log("appGroup: " + appGroup);
				Global.log("publicGroup: " + publicGroup);
				Global.log("uploadParams: " + uploadParams);
			}
			
			// Fix filename
			photo = photo.replace(":////", ":///");
			if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Filename is  " + photo);
			
			// Get the picture latitude / longitude from EXIF, or fallback to last latitude / longitude if EXIF writing failed
			String latitude = "";
			String longitude = "";
			ExifData exifData = CameraUtils.getExifData(UrlUtils.urlToFileName(photo));
			Location geoLocation = exifData.getLocation();
			if (geoLocation != null)
			{
				latitude = "" + geoLocation.getLatitude();
				longitude = "" + geoLocation.getLongitude();
			}
			else if (mLastPictureLatitude != 0 && mLastPictureLongitude != 0)
			{
				latitude = "" + mLastPictureLatitude;
				longitude = "" + mLastPictureLongitude;
			}
			
			// Get the picture date
			String date;
			String modifyDateTime = exifData.getModifyDateTimeString();
			String originalDateTime = exifData.getOriginalDateTimeString();
			if (originalDateTime != null && originalDateTime.length() != 0)
			{
				date = originalDateTime;
			}
			else if (modifyDateTime != null && modifyDateTime.length() != 0)
			{
				date = modifyDateTime;
			}
			else if (mLastPictureDate != null)
			{
				SimpleDateFormat sdf = new SimpleDateFormat(Global.DATE_FORMAT_API); 
				date = sdf.format(mLastPictureDate);
			}
			else
			{
				date = "";
			}
			
			// Set the location
			String location = "";
			
			// If the redirect URL does not exist, use default
			if (redirectUrl == null || redirectUrl.length() == 0)
			{
				redirectUrl = getSnaprUrl(UrlUtils.getFullLocalUrl(Global.URL_UPLOAD));
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Original URL was " + url);
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": No redirect URL in original URL ");
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Adding default redirect URL " + redirectUrl);
			}
			else
			{
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Original URL was " + url);
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found redirect URL " + redirectUrl);
			}
			
			// Create the localId if blank
			if (localId == null || localId.length() == 0 || localId.equals("undefined"))
			{
				localId = getUploadLocalId();
			}
			
			// Send the information to the upload service via intent
			Intent uploadIntent = new Intent(getContext(), UploadService.class);
	        uploadIntent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_ADD);
	        uploadIntent.putExtra(Global.PARAM_ACCESS_TOKEN, mAccessToken);
	        uploadIntent.putExtra(Global.PARAM_LOCAL_ID, localId);
	        uploadIntent.putExtra(Global.PARAM_PHOTO, photo);
	        uploadIntent.putExtra(Global.PARAM_DESCRIPTION, description);
	        uploadIntent.putExtra(Global.PARAM_LATITUDE, latitude);
	        uploadIntent.putExtra(Global.PARAM_LONGITUDE, longitude);
	        uploadIntent.putExtra(Global.PARAM_LOCATION, location);
	        uploadIntent.putExtra(Global.PARAM_DATE, date);
	        uploadIntent.putExtra(Global.PARAM_PRIVACY, status);
	        uploadIntent.putExtra(Global.PARAM_TWEET, isParamTrue(tweet));
	        uploadIntent.putExtra(Global.PARAM_FACEBOOK_FEED, isParamTrue(facebookFeed));
	        uploadIntent.putExtra(Global.PARAM_FACEBOOK_ALBUM, isParamTrue(facebookAlbum));
	        uploadIntent.putExtra(Global.PARAM_FACEBOOK_ALBUM_NAME, facebookAlbumName);
	        uploadIntent.putExtra(Global.PARAM_TUMBLR, isParamTrue(tumblr));
	        uploadIntent.putExtra(Global.PARAM_FOURSQUARE_CHECKIN, isParamTrue(foursquareCheckin));
	        uploadIntent.putExtra(Global.PARAM_FOURSQUARE_VENUE, foursquareVenueId);
	        uploadIntent.putExtra(Global.PARAM_APP_GROUP, appGroup);
	        uploadIntent.putExtra(Global.PARAM_PUBLIC_GROUP, publicGroup);
	        uploadIntent.putExtra(Global.PARAM_UPLOAD_PARAMS, uploadParams);
	        
	        getContext().startService(uploadIntent);
			
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());			
    	}
    };
    
    private boolean isParamTrue(String paramValue)
    {
    	return (paramValue!=null) && (paramValue.equals("checked") || paramValue.equals("true"));
    }
    
    // The action performed for snapr://upload_progress
    private Action uploadProgressAction = new Action() {
    	@Override
    	public void run(String url)
    	{			
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Received URL " + url);		
		
			// Update the queue status
			updateQueueStatus();
			
	        // Logs
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    private void updateQueueStatus()
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Requesting queue status update");
		Intent uploadIntent = new Intent(getContext(), UploadService.class);
		uploadIntent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_UPDATE_STATUS);
		getContext().startService(uploadIntent);
    }

    // The action performed for snapr://get_location
    private Action getLocationAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Received URL " + url);
    		
    		// Initialize the geo manager
    		mGeoManager = new GeoManager(getContext());
    		
    		// Determine caller
    		int caller;
    		if (url == null)
    		{
    			caller = Global.GEOLOCATION_PHOTO_SHARE;
    		}
    		else
    		{
    			caller = Global.GEOLOCATION_GET_LOCATION;
    		}
    		
    		// Request the location
    		mGeoManager.showLocationPendingDialog();
    		mGeoManager.getLocation(mGeoListener, caller);
    		
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };

    // The action performed for snapr://aviary
    private Action editPhotoAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    	    // Enable only if we have Aviary Feather
    		if (Global.USE_AVIARY_SDK)
    	    {
	    		// Log
	    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> editPhotoAction(): Received URL " + url);
	    
	    		// Get the photo url parameter
	    		Uri sourceUri = Uri.parse(url);
	    		String photoUrl = UrlUtils.getQueryParameter(sourceUri, Global.PARAM_PHOTO_URL);
	    		
	    		// Store the remaining params
	    		mLastDescription = UrlUtils.getQueryParameter(sourceUri, Global.PARAM_DESCRIPTION);
	    		mLastFoursquareVenueName = UrlUtils.getQueryParameter(sourceUri, Global.PARAM_FOURSQUARE_VENUE_NAME);
	    		mLastFoursquareVenueId = UrlUtils.getQueryParameter(sourceUri, Global.PARAM_FOURSQUARE_VENUE_ID);
	    		mLastLocationName = UrlUtils.getQueryParameter(sourceUri, Global.PARAM_LOCATION);
	    		
	    		// Create the photo uri
	    		Uri uri = Uri.parse(photoUrl);
	    		
	    		// Create the intent needed to start feather
	    		Intent newIntent = new Intent(getContext(), FeatherActivity.class);
	    		
	    		// Set the source image uri
	    		newIntent.setData( uri );
	    		
	    		// Pass the required api key/secret ( http://developers.aviary.com/geteffectskey )
	    		newIntent.putExtra("API_KEY", "d3fd1ff6b");
	    		newIntent.putExtra("API_SECRET", "1896d2192");
	    		
	    		// Pass the uri of the destination image file (optional)
	    		// This will be the same uri you will receive in the onActivityResult
	    		//newIntent.putExtra( "output", Uri.parse( "file://" + mOutputFile.getAbsolutePath() ) );
	    		
	    		// Format of the destination image (optional)
	    		newIntent.putExtra("output-format", Bitmap.CompressFormat.JPEG.name() );
	    		
	    		// Output format quality (optional)
	    		newIntent.putExtra("output-quality", 100 );
	    		
	    		// You can force feather to display only a certain tools
	    		//newIntent.putExtra("tools-list", new String[]{"EFFECTS", "CROP", "BRIGHTNESS", "CONTRAST", "SATURATION", "COLORS", "STICKERS", "TEXT", "DRAWING", "RED_EYE", "WHITEN", "BLEMISH", "ROTATE", "FLIP", "BLUR", "SHARPEN"} );
	    		
	    		// enable fast rendering preview
	    		newIntent.putExtra( "effect-enable-fast-preview", true );
	    		
	    		// Start feather
	    		startActivityForResult(newIntent, ACTION_REQUEST_FEATHER );
	    		
	    		// Log
	    		if (Global.LOG_MODE) Global.log(Global.TAG, " <- editPhotoAction()");
    	    }
    	}
    };
    
    private void startQueue()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	// Start the queue via intent
		Intent intent = new Intent(getContext(), UploadService.class);
        intent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_START);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_ON, mQueueUploadModeOn);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_WIFI_ONLY, mQueueUploadModeWifiOnly);
        getContext().startService(intent);
        
        // Log
        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    private void stopQueue()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	// Stop the queue via intent
		Intent intent = new Intent(getContext(), UploadService.class);
        intent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_STOP);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_ON, mQueueUploadModeOn);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_WIFI_ONLY, mQueueUploadModeWifiOnly);
        getContext().startService(intent);
        
        // Log
        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    private void sendQueueUploadMode()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Params - wifi only " + mQueueUploadModeWifiOnly + " queueOn " + mQueueUploadModeOn);
    	
    	// Send the information to the upload service via intent
		Intent intent = new Intent(getContext(), UploadService.class);
        intent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_UPLOAD_MODE);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_ON, mQueueUploadModeOn);
        intent.putExtra(Global.PARAM_QUEUE_UPLOAD_MODE_WIFI_ONLY, mQueueUploadModeWifiOnly);
        getContext().startService(intent);
        
        // Log
        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    private void updateQueueSettings(boolean queueUploadModeOn, boolean queueUploadModeWifiOnly)
    {
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Current On is " + mQueueUploadModeOn + " and wi-fi is " + mQueueUploadModeWifiOnly);
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": New On is " + queueUploadModeOn + " and wi-fi is " + queueUploadModeWifiOnly);
    	
		boolean oldQueueUploadModeWifiOnly = mQueueUploadModeWifiOnly;
		mQueueUploadModeWifiOnly = queueUploadModeWifiOnly;
		//boolean oldQueueUploadModeOn = queueUploadModeOn;
		mQueueUploadModeOn = queueUploadModeOn;
    	
		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": old wifi on" + oldQueueUploadModeWifiOnly);
		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": new wifi on" + mQueueUploadModeWifiOnly);
		
		// Notify service of upload mode changes
    	sendQueueUploadMode();
    	
    	// Notify UI of upload mode changes
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Sending " + "javascript:queue_settings('" + getUploadModeString(queueUploadModeWifiOnly) + "', " + (!queueUploadModeOn) + ");");
    	mWebView.loadUrl("javascript:queue_settings('" + getUploadModeString(queueUploadModeWifiOnly) + "', " + (!queueUploadModeOn) + ");");
		
		// User pressed the wifi button to enable wifi only
		if (!oldQueueUploadModeWifiOnly && mQueueUploadModeWifiOnly)
		{
			// If queue is on we may have to switch it off
			if (mQueueUploadModeOn)
			{
				// Check the Wifi state
				boolean wifiConnected = NetworkUtils.isWifiConnected(getContext());
				if (!wifiConnected)
				{
					// Stop queue
					if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Stopping queue due to Wifi setting getting enabled!");
					stopQueue();
				}
			}
		}
		// User pressed the wifi button to disable wifi only
		else if (oldQueueUploadModeWifiOnly && !mQueueUploadModeWifiOnly)
		{
			// If queue is off we may have to switch it on
			if  (mQueueUploadModeOn)
			{
				// Check if we have any other connection connection
				boolean anyConnected = NetworkUtils.isAnyConnected(getContext());
				
				if (anyConnected)
				{
					// Start queue
					if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Starting queue due to Wifi setting getting disabled!");
					startQueue();
				}
			}
		}
    }
    
    // The action performed for snapr://upload?setting=
    private Action queueSettingAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueSettingAction: Received URL " + url);
    		
    		// Get the setting from the URL
    		Uri uri = Uri.parse(url);
    		String settingString = UrlUtils.getQueryParameter(uri, Global.PARAM_SETTING);
    		boolean queueUploadModeWifiOnly = getWifiOnlyFromUploadMode(settingString);
    		boolean queueUploadModeOn = getOnFromUploadMode(settingString);
	    	
    		// Update the queue
    		updateQueueSettings(queueUploadModeOn, queueUploadModeWifiOnly);
    		
	    	// Log
	    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    // The action performed for snapr://upload?start
    private Action queueStartAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{	
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueStartAction: Received URL " + url);
    		
    		// Set the queue on/of state
    		//mQueueUploadModeOn = true;
    		updateQueueSettings(true, mQueueUploadModeWifiOnly);
    		
    		// Send the information to the upload service via intent
			startQueue();
	        
	        // Log
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    // The action performed for snapr://upload?stop
    private Action queueStopAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueStopAction: Received URL " + url);
    		
    		// Set the queue on/of state
    		//mQueueUploadModeOn = false;
    		updateQueueSettings(false, mQueueUploadModeWifiOnly);
    		
    		// Send the information to the upload service via intent
			stopQueue();
	        
	        // Log
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    // The action performed for snapr://upload?clear
    private Action queueClearAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{	
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueClearAction: Received URL " + url);
    		
    		// Clear the queue
    		clearQueue();
	        
	        // Log
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    private void clearQueue()
    {
    	// Send the information to the upload service via intent
		Intent intent = new Intent(getContext(), UploadService.class);
        intent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_CLEAR);
        getContext().startService(intent);
    }

    // The action performed for snapr://upload?cancel=id
    private Action queueCancelAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{	
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueCancelAction: Received URL " + url);
    		
    		// Get the localId from the url
    		Uri uri = Uri.parse(url);
    		String localId = UrlUtils.getQueryParameter(uri, Global.PARAM_CANCEL);
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> queueCancelAction: Got cancel for localId " + localId);
    		
    		// Cancel the upload
    		cancelUpload(localId);
	        
	        // Log
	        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    private void cancelUpload(String localId)
    {
    	// Cancel upload
		Intent launchIntent = new Intent(getContext(), UploadService.class);
		launchIntent.putExtra(Global.PARAM_ACTION, Global.ACTION_QUEUE_REMOVE);
		launchIntent.putExtra(Global.PARAM_LOCAL_ID, localId);
        getContext().startService(launchIntent);
    }
    
    // The action performed when browsing an external site
    private Action externalBrowseAction = new Action() {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> externalBrowseAction: Received URL " + url);
    		
    		Uri uri = Uri.parse(url);
    		if (uri.getScheme().equals(Global.SCHEME_SNAPR))
    		{
    			// Set the url to the embedded parameter
    			url = UrlUtils.getQueryParameter(uri, Global.PARAM_URL);
			}
    		
    		// Check if this is a Facebook link that should be handled natively
    		// Handle natively only when we have a FACEBOOK_APP_ID
    		String facebookAppId = pr.sna.snaprkit.utils.Configuration.getInstance().getFacebookAppId();
    		if (facebookAppId != null && facebookAppId.length() > 0)
    		{
    			if (url.contains(Global.URL_SNAPR_DOMAIN) && url.contains(Global.URL_FACEBOOK_LOGIN_BASE))
	    		{
	    			// Move this to Facebook native flow
	    			facebookLoginAction.run(url);
	    			return;
	    		}
    			else if (url.contains(Global.URL_SNAPR_DOMAIN) && url.contains(Global.URL_FACEBOOK_OAUTH_BASE))
	    		{
	    			// Move this to Facebook native flow
	    			facebookPublishAction.run(url);
	    			return;
	    		}
    		}
    		
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Starting external webview with URL " + url);
    		
    		// Start the external browse activity
    		Intent intent  = new Intent(getContext(), WebViewExternalActivity.class);
    		intent.putExtra("url", url);
			startActivityForResult(intent, WebViewExternalActivity.DISPLAY_EXTERNAL_URL);
			
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };

    // The default action performed when there is no match in the action table
    private Action defaultAction = new Action() {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> defaultAction: Received URL " + url);
    		
    		// Load the startup URL
			mWebView.loadUrl(url);
			
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    // The actionsheet action
    private Action actionSheetAction = new Action() {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> actionSheetAction: Received URL " + url);
    		
    		
    		// Get the setting from the URL
    		Uri uri = Uri.parse(url);
    		mContextMenuTitle = UrlUtils.getQueryParameter(uri, Global.PARAM_TITLE);
    		mContextMenuDestructiveItemLabel = UrlUtils.getQueryParameter(uri, Global.PARAM_DESTRUCTIVE_BUTTON_LABEL);
    		mContextMenuCancelItemLabel = UrlUtils.getQueryParameter(uri, Global.PARAM_CANCEL_BUTTON_LABEL);
    		mContextMenuOtherItem1Label = UrlUtils.getQueryParameter(uri, Global.PARAM_OTHER_BUTTON_1_LABEL);
    		mContextMenuOtherItem2Label = UrlUtils.getQueryParameter(uri, Global.PARAM_OTHER_BUTTON_2_LABEL);
    		mContextMenuOtherItem3Label = UrlUtils.getQueryParameter(uri, Global.PARAM_OTHER_BUTTON_3_LABEL);
    		String contextMenuActionIdString = UrlUtils.getQueryParameter(uri, Global.PARAM_ACTION_ID);
    		mContextMenuActionId = Integer.parseInt(contextMenuActionIdString);
    		
    		// Open the context menu
    		getActivity().openContextMenu(mContextMenuButton);
			
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    // The alert action
    private Action alertAction = new Action() {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> alertAction: Received URL " + url);
    		
    		
    		// Get the setting from the URL
    		Uri uri = Uri.parse(url);
    		String title = UrlUtils.getQueryParameter(uri, Global.PARAM_TITLE);
    		if (title == null) title="";
    		String buttonLabel = UrlUtils.getQueryParameter(uri, Global.PARAM_OTHER_BUTTON_1_LABEL);
    		if (buttonLabel == null) buttonLabel = "";
    		String message = UrlUtils.getQueryParameter(uri, Global.PARAM_MESSAGE);
    		if (message == null) message = "";
    		
    		// Display the alert
    		new AlertDialog.Builder(getActivity())  
            .setTitle(title)  
            .setMessage(message)
            .setPositiveButton(buttonLabel,  
                    new AlertDialog.OnClickListener()  
                    {  
                        public void onClick(DialogInterface dialog, int which)  
                        {  
                        }  
                    })  
            .setCancelable(false) 
            .create()  
            .show();
    	}
    };
    
    // The download image action
    private Action downloadImageAction = new Action() {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> downloadImageAction: Received URL " + url);
    		
    		// Get the setting from the URL
    		// Note that the query parameter does not need to be decoded and that we need to correct some problems
    		Uri uri = Uri.parse(url);
    		final String imageData = UrlUtils.getEncodedQueryParameter(uri, Global.PARAM_IMAGE_DATA).replace(" ","+");
    		
    		// Display the alert
    		new AlertDialog.Builder(getActivity())
    		.setMessage(R.string.snaprkit_downloadimage_message)
    		.setPositiveButton(R.string.snaprkit_save,  
                    new AlertDialog.OnClickListener()  
                    {  
                        public void onClick(DialogInterface dialog, int which)  
                        {
                        	byte[] data = Base64.decode(imageData, Base64.DEFAULT);
                        	SavePhotoTask savePhotoTask = new SavePhotoTask();
                        	savePhotoTask.execute(data);
                        }  
                    })
    		.setNegativeButton(R.string.snaprkit_cancel,
            		new AlertDialog.OnClickListener()  
		            {  
		                public void onClick(DialogInterface dialog, int which)  
		                {
		                	// Do nothing
		                }  
		            })
    		.setCancelable(false) 
    		.create()  
    		.show();
    	}
    };

    class SavePhotoTask extends AsyncTask<byte[], Void, String> {
        @Override
        protected String doInBackground(byte[]... jpeg)
        {
        	// Prepare file
        	File photo = new File(pr.sna.snaprkit.utils.FileUtils.getDCIMCameraDirectory(), CameraUtils.getPictureName());
        	
        	// Make sure that the photo file path exists!
        	try
        	{
        		if(photo.exists() == false)
        		{
        			photo.getParentFile().mkdirs();
        			photo.createNewFile();
        		}
        	}
        	catch (IOException e)
        	{
        		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() +  " Could not create file " + photo.getAbsolutePath());
        		if (Global.LOG_MODE) Global.log(ExceptionUtils.getExceptionStackString(e));
        		return null;
        	}

        	try
        	{
        		FileOutputStream fos=new FileOutputStream(photo.getPath());
	
        		fos.write(jpeg[0]);
        		fos.close();
        	}
        	catch (java.io.IOException e)
        	{
        		if (Global.LOG_MODE) Global.log("Could not save image to disk");
        		if (Global.LOG_MODE) Global.log(ExceptionUtils.getExceptionStackString(e));
        		return null;
        	}
	
        	return photo.getPath();
        }

		@Override
		protected void onPostExecute(String filePath)
		{
        	if(filePath != null) MediaScannerConnection.scanFile(getActivity().getApplicationContext(), new String[] { filePath }, null, null);
		}
    }
    
    private void initActionMap()
    {
		mActionMappings.add(new UrlMapping("snaprkit-parent://.*", snaprKitParentAction));
		mActionMappings.add(new UrlMapping("snaprkit://.*", snaprKitParentAction));
		mActionMappings.add(new UrlMapping("snapr://login.*", loginAction));
		mActionMappings.add(new UrlMapping("snapr://logout.*", logoutAction));
		mActionMappings.add(new UrlMapping("snapr://get_location.*", getLocationAction));
		mActionMappings.add(new UrlMapping("snapr://upload_progress.*", uploadProgressAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?setting.*", queueSettingAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?start.*", queueStartAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?stop.*", queueStopAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?clear.*", queueClearAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?cancel.*", queueCancelAction));
		mActionMappings.add(new UrlMapping("snapr://upload\\?.*", uploadAction));
		mActionMappings.add(new UrlMapping("snapr://redirect.*", redirectAction));
		mActionMappings.add(new UrlMapping("snapr://camera.*", cameraAction));
    	mActionMappings.add(new UrlMapping("snapr://photo-library.*", photoGalleryAction));
    	mActionMappings.add(new UrlMapping("snapr://action.*", actionSheetAction));
    	mActionMappings.add(new UrlMapping("snapr://alert.*", alertAction));
    	mActionMappings.add(new UrlMapping("snapr://download-image.*", downloadImageAction));
    	
    	mActionMappings.add(new UrlMapping("snapr://link.*", externalBrowseAction));
    	if (Global.USE_AVIARY_SDK) mActionMappings.add(new UrlMapping("snapr://aviary.*", editPhotoAction));
		mActionMappings.add(new UrlMapping("snapr://.*", defaultAction));
		mActionMappings.add(new UrlMapping("https://sna.pr/api/linked_services/facebook/signin/.*redirect=.*", facebookLoginAction));
		mActionMappings.add(new UrlMapping("https://sna.pr/api/linked_services/facebook/oauth/.*redirect=.*", facebookPublishAction));
		mActionMappings.add(new UrlMapping("file://.*", defaultAction));
		mActionMappings.add(new UrlMapping("http://.*", externalBrowseAction));
		mActionMappings.add(new UrlMapping("https://.*", externalBrowseAction));
    }
    
    // Set webview settings and display startup page
	// mWebView global should have been prepopulated
    @SuppressLint({ "SetJavaScriptEnabled", "NewApi", "SdCardPath" })
	private void initWebView(View view, Bundle savedInstanceState)
    {
    	// Declare
    	String url = null;
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
        
    	// Get the webview control
        mWebView = (WebView) view.findViewById(R.id.webview);
        
        // Override clicks
        mWebView.setWebViewClient(new WebViewClientEx(getActivity()) {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "shouldOverrideUrlLoading() " + url);
				
				// Run appropriate action
				//String urlKey = UrlUtils.getUrlKey(url);
				//if (Global.LOG_MODE) Global.log("Overriding url: " + url + " using url key: " + urlKey);
				//Action action = getActionForUrl(urlKey);
				Action action = getActionForUrl(url);
				action.run(url);
				
				// Set the current Url
				if (url != null)
				{
					String normalUrl = UrlUtils.normalUrl(url);
					if (UrlUtils.isLocalUrl(normalUrl))
					{						
						mCurrentUrl = url;
					}
				}
				
				// Indicate that we handled the request
				return true;
			}

			@Override
			public void onPageFinished(WebView view, String url)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "onPageFinished() " + url);
				
				// Get normal URL
				String normalUrl = UrlUtils.normalUrl(url);
				
				// Set the current Url
				if (normalUrl != null && UrlUtils.isLocalUrl(normalUrl))
				{
					mCurrentUrl = url;
				}
				
				// Perform queue setting init after first page load
				// We need the page loaded so the JS is present
				if (normalUrl != null && mMenuInitDone == false)
				{
					// Change flag
					mMenuInitDone = true;
				
					// Set queue settings using two second delay
					mHandler.postDelayed(mUpdateQueueSettings, 2000);
				}
				
				// Clear the history every time we hit index.html				
				if(url != null && (
					url.startsWith(UrlUtils.getFullLocalUrl(UrlUtils.ajaxUrl(Global.URL_HOME))) ||
					url.startsWith(UrlUtils.getFullLocalUrl(UrlUtils.ajaxUrl(Global.URL_MENU)) + "?") ||
					url.equals(UrlUtils.getFullLocalUrl(Global.URL_MENU) + "#") ||
					url.equals(UrlUtils.getFullLocalUrl(UrlUtils.ajaxUrl(Global.URL_MENU)))))
				{
					// Clear the history
					if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Clearing history because we hit home page!");
					mWebView.clearHistory();
				}
				
				// Check if we have a listener and call it
				// Exclude snaprkit-parent urls
				if (mSnaprKitListener != null && url != null && !url.startsWith("snaprkit-parent://"))
				{
					mSnaprKitListener.onPageFinished(url);
				}
				
				// Proceed as usual
				super.onPageFinished(view, url);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "onPageStarted() " + url);
				
        		// Carry on
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.TAG, "onReceiveError() got error " + description);
			}
    	});
        
        // Add support for JavaScript alert dialogs - necessary for unlink dialogs
        mWebView.setWebChromeClient(new WebChromeClient() {

			@Override  
            public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result)  
            {
				if (SnaprKitFragment.this.getActivity() != null && !SnaprKitFragment.this.getActivity().isFinishing()) // Need check to avoid random crashes when we are in the backgroound
				{
					String appName = pr.sna.snaprkit.utils.Configuration.getInstance().getAppName();
	                new AlertDialog.Builder(getActivity())  
	                    .setTitle(appName)  
	                    .setMessage(message)  
	                    .setPositiveButton(android.R.string.ok,  
	                            new AlertDialog.OnClickListener()  
	                            {  
	                                public void onClick(DialogInterface dialog, int which)  
	                                {  
	                                    result.confirm();  
	                                }  
	                            })  
	                    .setCancelable(false)  
	                    .create()  
	                    .show();  
				}
          
                return true;  
            };
            
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) 
            {
            	if (!getActivity().isFinishing()) // Need check to avoid random crashes when we are in the backgroound
            	{
            		String appName = pr.sna.snaprkit.utils.Configuration.getInstance().getAppName();
	                new AlertDialog.Builder(getActivity())
	                    .setTitle(appName)
	                    .setMessage(message)
	                    .setPositiveButton(android.R.string.ok, 
	                            new DialogInterface.OnClickListener() 
	                            {
	                                public void onClick(DialogInterface dialog, int which) 
	                                {
	                                    result.confirm();
	                                }
	                            })
	                    .setNegativeButton(android.R.string.cancel, 
	                            new DialogInterface.OnClickListener() 
	                            {
	                                public void onClick(DialogInterface dialog, int which) 
	                                {
	                                    result.cancel();
	                                }
	                            })
	                .create()
	                .show();
            	}
            
                return true;
            };
            
            @Override
            public void onReceivedTitle(WebView view, String title)
            {
                //setTitle(title);
                super.onReceivedTitle(view, title);
            }
        });
        
        // Enable JavaScript
        mWebView.getSettings().setJavaScriptEnabled(true);
        
        // Enable HTML5 local storage and make it persistent
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDatabasePath("/data/data/" + getActivity().getPackageName() + "/databases/");
        
        // Clear spurious cache data
        mWebView.clearHistory();
        mWebView.clearFormData();
        mWebView.clearCache(true);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setUseWideViewPort(true);
        
        // Accept cookies
        CookieSyncManager.createInstance(getContext()); 
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        
        // Make sure that the webview does not allocate blank space on the side for the scrollbars
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        
        // Restore the webview history
        // Must be done before using the webview, otherwise the restore fails
        if (savedInstanceState != null && savedInstanceState.isEmpty() == false)
        {
        	// Log
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found webview state");
        	
        	// Restore the state
        	mWebView.restoreState(savedInstanceState);
        	
        	// Process current URL
        	if (mCurrentUrl !=null && mCurrentUrl.startsWith(Global.SCHEME_SNAPR))
			{
				// Special exception for SNAPR URLs to prevent external action triggers
				url = null;
				
				// Log the previous URL
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found snapr:// scheme on startup and previous URL:" + UrlUtils.getPreviousUrl(mWebView));
			}
			else
			{
				url = mCurrentUrl;
			}
        	
        	// Load the URL
        	mWebView.loadUrl(url);
        	
        	// Log
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Started via activity restore flow with initial URL " + url);
        }
        else
        {
        	if (mCurrentUrl != null)
        		{
        		//mWebView.loadUrl(mCurrentUrl);
        		mWebView.loadUrl( "javascript:window.location.reload( true )" );
        		}
        }
        
        // Bugfix for http://code.google.com/p/android/issues/detail?id=7189
        // If we want the soft keyboard to show for form fields in the webview
        // we must obtain focus after load. We also need to add an event handler 
        // to obtain focus after every field touch
        
        mWebView.setFocusable(true);
        mWebView.setFocusableInTouchMode(true);
        mWebView.requestFocus(View.FOCUS_DOWN);
        /*
        mWebView.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });
        */
       
        // Log
        if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    }    
    
    public void setSharedPictureFileName(String sharedPictureFileName)
    {
    	mSharedPictureFileName = sharedPictureFileName;
    }
    
    private SharedPictureInfo getSharedPictureInfo()
    {
    	// Declare
    	SharedPictureInfo info = null;
    	double latitude = 0;
    	double longitude = 0;
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Check that we have a shared picture name
    	if (mSharedPictureFileName != null)
    	{
    	
	    	// Get the EXIF location from the filename
	    	Location exifLocation = getExifLocation(mSharedPictureFileName);
	    	
	    	if (exifLocation != null)
	    	{
	    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found location information within picture");
	    		latitude = exifLocation.getLatitude();
	    		longitude = exifLocation.getLongitude();
	    	}
	    	else
	    	{
	    		// Log
	    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found no location information within picture");
	    	}
	    	
			// Add the info
	    	info  = new SharedPictureInfo();
	    	info.setFileName(mSharedPictureFileName);
	    	info.setLatitude(latitude);
	    	info.setLongitude(longitude);
    	}
        
        // Return
        return info;
    }
    
    private Location getExifLocation(String fileName)
    {
		// Try to get the location from the file
    	ExifData exifData = CameraUtils.getExifData(fileName);
		return exifData.getLocation();
    }

	// Get the wifi setting from the upload mode / setting string
    // Used internally, so do not internatialize
    private String getUploadModeString(boolean wifiOnly)
    {
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Got wifi " + wifiOnly);
    	if (wifiOnly == true)
    	{
    		return UPLOAD_MODE_WIFI_ONLY; 
    	}
    	else
		{
    		return  UPLOAD_MODE_ON;
		}
    }

    /**
     * Gets the on queue setting from the upload mode / setting string
     * @param uploadMode The upload mode or setting string
     */
    private boolean getOnFromUploadMode(String uploadMode)
    {
    	if(UPLOAD_MODE_ON.equals(uploadMode))
    	{
    		return true;
    	}
    	else if (UPLOAD_MODE_WIFI_ONLY.equals(uploadMode))
    	{
    		return true;
    	}
    	else
    	{
    		return false;
    	}
    }
    
    /**
     * Gets the queue settings from the upload mode / setting string
     * @param uploadMode The upload mode or setting string
     */
    private boolean getWifiOnlyFromUploadMode(String uploadMode)
    {
    	if(UPLOAD_MODE_ON.equals(uploadMode))
    	{
    		return false;
    	}
    	else if (UPLOAD_MODE_WIFI_ONLY.equals(uploadMode))
    	{
    		return true;
    	}
    	else
    	{
    		return true;
    	}
    }
    
    private void initGlobals()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	Global.densityDpi = Global.getScreenDensity(SnaprKitFragment.this.getActivity());
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void initQueueSettings()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	try
    	{
	    	// Load the queue settings into variables
	    	loadQueueSettings();
	    	
	    	// Notify JavaScript
	    	// Now done onPageFinished to guarantee that the JS is loaded
	    	
	    	// Notify service of upload mode
	    	sendQueueUploadMode();
	    	
	    	// If the queue is supposed to be on, then turn it on
	    	if (mQueueUploadModeOn)
	    	{
	    		startQueue();
	    	}
	    	else
	    	{
	    		stopQueue();
	    	}
    	}
    	catch (Exception e)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed to intialize queue settings due to error " + e.toString());
    	}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void initSavedState(Bundle savedInstanceState)
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	if ((savedInstanceState != null) && (savedInstanceState.isEmpty() == false))
		{
    		// Log
        	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + ": Found saved state");
        	
        	// Set flag
        	mRestoredFromSavedState = true;
    		
			mMenuInitDone = savedInstanceState.getBoolean("mMenuInitDone");
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mMenuInitDone to " + mMenuInitDone);
			
			mLastPictureLatitude = savedInstanceState.getDouble("mLastPictureLatitude");
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mLastPictureLatitude to " + mLastPictureLatitude);
			
			mLastPictureLongitude = savedInstanceState.getDouble("mLastPictureLongitude");
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mLastPictureLongitude to " + mLastPictureLongitude);
			
			long date = savedInstanceState.getLong("mLastPictureDate");
			if (date != 0) mLastPictureDate = new Date(date);
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mLastPictureDate to " + mLastPictureDate);
			
			mCurrentUrl = savedInstanceState.getString("mCurrentUrl");
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mCurrentUrl to " + mCurrentUrl);
			
			mStickerPathPath = savedInstanceState.getString("mStickerPathPath");
			mFilterPackPath = savedInstanceState.getString("mFilterPackPath");
		}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void initCameraManager(Bundle savedInstanceState)
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());

    	if (mPictureAcquisitionManager == null) mPictureAcquisitionManager = new PictureAcquisitionManager(SnaprKitFragment.this);
    	
    	if ((savedInstanceState != null) && (savedInstanceState.isEmpty() == false))
    	{
    		// Log
        	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + ": Found saved state");
        	
        	long photoTimestamp = savedInstanceState.getLong("mPhotoTimestamp");
        	mPictureAcquisitionManager.setPhotoTimestamp(photoTimestamp);
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored mPhotoTimestamp to " + photoTimestamp);
        	
    		// Restore saved variables
        	int imageSource = savedInstanceState.getInt("mImageSource");
    		mPictureAcquisitionManager.setImageSource(imageSource);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored imageSource to " + imageSource);
        	
    		// Note that we restore both image URI and path -- must be done this way
        	// because depending on reentry point, the code inside the class may not 
        	// set the image path
    		String imagePath = savedInstanceState.getString("mImagePath");
    		Uri imageUri = UrlUtils.imagePath2Uri(imagePath);
    		mPictureAcquisitionManager.setImageUri(imageUri);
    		mPictureAcquisitionManager.setImagePath(imagePath);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored imageUri to " + imageUri);
    		
    		boolean isActive = savedInstanceState.getBoolean("mIsActive");
    		mPictureAcquisitionManager.setIsActive(isActive);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored isActive to " + isActive);
    		
    		boolean isGeolocationActive = savedInstanceState.getBoolean("mIsGeolocationActive");
    		mPictureAcquisitionManager.setIsGeolocationActive(isGeolocationActive);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored isGeolocationActive to " + isGeolocationActive);
    		
    		double latitude = savedInstanceState.getDouble("mLatitude");
    		double longitude = savedInstanceState.getDouble("mLongitude");
    		if (latitude != -1 && longitude != -1)
    		{
    			Location location = new Location ("unknown");
    			location.setLatitude(latitude);
    			location.setLongitude(longitude);
    			mPictureAcquisitionManager.setLocation(location);
    			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored location latitude to " + latitude);
    			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored location longitude to " + longitude);
    		}
    		else
    		{
    			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored location to null");
    		}
    		
    		boolean networkProviderEnabled = savedInstanceState.getBoolean("mNetworkProviderEnabled");
    		mPictureAcquisitionManager.setNetworkProviderEnabled(networkProviderEnabled);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored networkProviderEnabled to " + networkProviderEnabled);
    		
    		boolean gpsProviderEnabled = savedInstanceState.getBoolean("mGpsProviderEnabled");
    		mPictureAcquisitionManager.setGpsProviderEnabled(gpsProviderEnabled);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored gpsProviderEnabled to " + gpsProviderEnabled);
    		
    		boolean wifiProviderEnabled = savedInstanceState.getBoolean("mWifiProviderEnabled");
    		mPictureAcquisitionManager.setWifiProviderEnabled(wifiProviderEnabled);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored wifiProviderEnabled to " + wifiProviderEnabled);
    		
    		String userData = savedInstanceState.getString("mUserData");
    		mPictureAcquisitionManager.setUserData(userData);
    		if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Restored userData to " + userData);
    		
    		// Add back the camera listener, but use current version
    		mPictureAcquisitionManager.setPictureAcquisitionListener(getPictureAcquisitionListener());
    	}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void initBroadcastReceivers()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Register connectivity receiver
    	mConnectivityReceiver = new ConnectivityBroadcastReceiver();
    	getContext().registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    	
    	// Register upload receiver
    	IntentFilter filter = new IntentFilter(Global.INTENT_BROADCAST_UPLOAD);
    	filter.addCategory(Intent.CATEGORY_DEFAULT);
    	mServiceCallbackReceiver = new UploadBroadcastReceiver();
    	getContext().registerReceiver(mServiceCallbackReceiver, filter);

    	// Register camera manager geolocation receiver
    	// but only if we are currently doing geolocation
		if (mPictureAcquisitionManager != null && mPictureAcquisitionManager.isGeolocationActive())
		{
			mPictureAcquisitionManager.registerGeolocationBroadcastReceiver();
		}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void closeBroadcastReceivers()
    {
		// Unregister connectivity receiver
		if (mConnectivityReceiver != null)
		{
			getContext().unregisterReceiver(mConnectivityReceiver);
			mConnectivityReceiver = null;
		}
		
		// Unregister upload receiver
		if (mServiceCallbackReceiver != null)
		{
			getContext().unregisterReceiver(mServiceCallbackReceiver);
			mServiceCallbackReceiver = null;
		}
		
		// Unregister camera manager geolocation receiver
		if (mPictureAcquisitionManager != null)
		{
			mPictureAcquisitionManager.unregisterGeolocationBroadcastReceiver();
		}
    }
    
    private void performLogsMaintenance()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Get the cache directory
    	String logsDirectory = FileUtils.getLogsDirectory();
    	long cacheSize = FileUtils.getDirectorySize(logsDirectory);
    	
    	// Check the cache size
    	if (cacheSize > Global.LOG_MAX_SIZE)
    	{
    		FileUtils.cleanDirectory(logsDirectory);
    	}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    private void clearLogs()
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Get the logs directory
    	String logsDirectory = FileUtils.getLogsDirectory();
    	if (FileUtils.isDirectoryPresent(logsDirectory))
    	{
    		FileUtils.cleanDirectory(logsDirectory);
    	}
    	
    	// Log
    	if (Global.LOG_MODE) Global.log(" <- " + Global.getCurrentMethod());
    }
    
    // Send the log through email
    private void sendLog(String logFileName)
    {
    	Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND); 
        emailIntent.setType("text/plain");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{getString(R.string.snaprkit_log_email_address)}); 
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.snaprkit_log_email_subject)); 
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, getString(R.string.snaprkit_log_email_body_text)); 
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ logFileName));
        startActivity(Intent.createChooser(emailIntent, getString(R.string.snaprkit_log_email_send)));
    }
    
    // Ask the user to select a log and then send it thru email
    private void sendPreviousLog()
    {
    	// Get the list of files for the log directory
    	String logDirectory = FileUtils.getLogsDirectory();
    	File   logDir = new File(logDirectory);
    	String[] files = logDir.list();
    	
    	// Sort the file list in reverse order
    	Arrays.sort(files, new Comparator<String>() {

    	    @Override
    	    public int compare(String o1, String o2) {
    	        return o2.compareTo(o1);
    	    }
    	});
    	
    	// Convert to arrayList
    	ArrayList<String> filesList = new ArrayList<String>(Arrays.asList(files));
    	filesList.remove(0);
    	files = filesList.toArray(new String[filesList.size()]);
    	
    	// Invoke the log file picker
    	showLogFilePicker(files);
    }
    
    private void init(Bundle savedInstanceState)
    {
    	// Log
    	if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    	
    	// Initialize globals
    	initGlobals();
    	
    	// Prune the logs
    	if (Global.LOG_MODE)
    	{
    		performLogsMaintenance();
    	}
    	
    	// Initialize the camera manager
    	initCameraManager(savedInstanceState);
    	
    	// Load user info from preferences
    	loadUserInfo();
    	
    	// Initialize queue settings
    	initQueueSettings();
    	
    	// Initialize the saved state
    	initSavedState(savedInstanceState);
    	
    	// Set action map
    	initActionMap();
    	
    	// Set the Facebok native objects
    	initFacebookNative(savedInstanceState);
        
        // Log
        if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod());
    }
	
    /*
	private void showUploadError(String errorMessage)
	{
		AlertUtils.showAlert(getActivity(), errorMessage, getString(R.string.snaprkit_error_upload));
	}
	*/
	
    /**
     * Show a picker to allow the user to select a file
     */
	private void showLogFilePicker(final String[] files)
	{
		// Do not build dialog if the activity is finishing
		if (SnaprKitFragment.this.getActivity().isFinishing()) return;
		
		// Display alert
		AlertDialog.Builder builder = new AlertDialog.Builder(SnaprKitFragment.this.getActivity());
		builder.setTitle(getString(R.string.snaprkit_log_select));
		builder.setItems(files, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialogInterface, int itemIndex)
			{
				// Call the log send with the file
				String fileName = Global.DIR_LOGS + "/" + files[itemIndex]; 
				sendLog(fileName);
			}
		});
		builder.create().show();
	}
    
	private interface Action
	{
		public void run(String url);
	}
	
	private class UrlMapping {
		public String url;
		public Action action;

		public UrlMapping(String url, Action action) {
			this.url = url;
			this.action = action;
		}
	}
	
	/**
	 * Goes through the list of mappings from beginning to end until it finds a match. 
	 * The mapping uses regular expressions.
	 */
	private Action getActionForUrl(String url) {
		Action action = defaultAction;
		if (url != null) {
			for (UrlMapping urlMapping : mActionMappings) {
				if (url.matches(urlMapping.url)) {
					if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": URL " + url + " matches " + urlMapping.url);
					action = urlMapping.action;
					break;
				}
			}
		}
		return action;
	}
	
	private Context getContext()
	{
		return (getActivity()!=null?getActivity():mView.getContext());
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		// Extract the items id
		int itemId = item.getItemId();
		
		// Check it for validity
		if (itemId <-1 || itemId > 3) return false;
		
		// Call the JavaScript to report click
		mWebView.loadUrl("javascript:tapped_action(" + mContextMenuActionId + ", " + itemId + ");");
		
		// Log
		if (Global.LOG_MODE) Global.log( " -> " + Global.getCurrentMethod() + ": Called JavaScript with action id " + mContextMenuActionId + " and item id " + itemId);
		
		// Return
		return true;  
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(mContextMenuTitle);
		if (mContextMenuDestructiveItemLabel != null) menu.add(0, -1, 0, mContextMenuDestructiveItemLabel); // always first
        if (mContextMenuOtherItem1Label != null) menu.add(0, 1, 0, mContextMenuOtherItem1Label);
        if (mContextMenuOtherItem2Label != null) menu.add(0, 2, 0, mContextMenuOtherItem2Label);
        if (mContextMenuOtherItem3Label != null) menu.add(0, 3, 0, mContextMenuOtherItem3Label);
        if (mContextMenuCancelItemLabel != null) menu.add(0, 0, 0, mContextMenuCancelItemLabel); // always last
	}

	// ------------------------------------------------------------------------
	// Native Facebook integration
	// ------------------------------------------------------------------------
	
	private FacebookSessionStatusListener mStatusListener = new FacebookSessionStatusListener();
	
	public void initFacebookNative(Bundle savedInstanceState)
	{
		// Set Facebook session
    	if (Global.LOG_MODE) Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
        Session session = Session.getActiveSession();
        if (session == null)
        {
            if (savedInstanceState != null)
            {
            	session = Session.restoreSession(getActivity(), null, mStatusListener, savedInstanceState);
            }
            if (session == null)
            {
            	String facebookAppId = pr.sna.snaprkit.utils.Configuration.getInstance().getFacebookAppId();
            	session = new Session.Builder(getActivity()).setApplicationId(facebookAppId).build();
            }
            Session.setActiveSession(session);
        }
        
        // Restore Facebook status
        mStatusListener.restoreStatus(savedInstanceState);
	}
	
	public void getFacebookReadAccess(FacebookSessionStatusListener listener, boolean retrieveUserBirthday)
	{
		List<String> permissions = getRequiredReadPermissions(retrieveUserBirthday);
		getFacebookReadAccess(listener, permissions);
	}
	
	public List<String> getRequiredReadPermissions(boolean retrieveUserBirthday)
	{
		if (retrieveUserBirthday)
		{
			return Arrays.asList("email", "user_birthday");
		}
		else
		{
			return Arrays.asList("email");
		}
	}
	
	public void getFacebookReadAccess(FacebookSessionStatusListener listener, List<String> permissions)
	{
		Session session = Session.getActiveSession();
		
		if (session == null || session.isClosed())
		{
			String facebookAppId = pr.sna.snaprkit.utils.Configuration.getInstance().getFacebookAppId();
			session = new Session.Builder(getActivity()).setApplicationId(facebookAppId).build();
        	Session.setActiveSession(session);
		}
		
        if (!session.isOpened())
        {
        	session.openForRead(getSessionOpenRequest(listener, permissions));
        }
        else
        {
        	listener.onFacebookAccess(session.getAccessToken(), session.getExpirationDate(), session.getPermissions());
        }
	}
	
	public List<String> getRequiredPublishPermissions()
	{
		return Arrays.asList("publish_stream");
	}
	
	public void getFacebookPublishAccess(FacebookSessionStatusListener listener, List<String> publishPermissions)
	{		
		// Check for publish permissions
		Session session = Session.getActiveSession();
        
		if (session == null || session.isClosed())
        {
			String facebookAppId = pr.sna.snaprkit.utils.Configuration.getInstance().getFacebookAppId();
        	session = new Session.Builder(getActivity()).setApplicationId(facebookAppId).build();
        	Session.setActiveSession(session);
        }
		
		if (!session.isOpened())
        {
        	session.openForPublish(getSessionOpenRequest(listener, publishPermissions));
        }
        else
        {
        	List<String> permissions = session.getPermissions();
        	if (!isSubsetOf(publishPermissions, permissions))
	        {
	            Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(this, publishPermissions);
	            session.requestNewPublishPermissions(newPermissionsRequest);
	            return;
	        }
	        else
	        {
	        	// Return Facebook access token
	        	listener.onFacebookAccess(session.getAccessToken(), session.getExpirationDate(), session.getPermissions());
	        	return;
	        }
        }
	}
	
	private OpenRequest getSessionOpenRequest(FacebookSessionStatusListener listener, List<String> permissions)
	{
		OpenRequest openRequest = new Session.OpenRequest(this);
		openRequest.setCallback(listener);
		openRequest.setPermissions(permissions);
		return openRequest;
	}
	
	private boolean isSubsetOf(Collection<String> subset, Collection<String> superset)
	{
	    for (String string : subset)
	    {
	        if (!superset.contains(string))
	        {
	            return false;
	        }
	    }
	    return true;
	}
	
	private class FacebookSessionStatusListener implements Session.StatusCallback, OnFacebookAccessListener
	{
		// Members
		private String mOriginalUrl = null;
		private boolean mRequestedPublishPermissions = false;
		private boolean mImmediatelyRequestPublishPermissions = false;
		
		public FacebookSessionStatusListener()
		{
			mOriginalUrl = null;
			mRequestedPublishPermissions = false;
			mImmediatelyRequestPublishPermissions = false;
		}
		
		public void saveStatus(Bundle outState)
		{
			outState.putString("mOriginalUrl", mOriginalUrl);
			outState.putBoolean("mRequestedPublishPermissions", mRequestedPublishPermissions);
			outState.putBoolean("mImmediatelyRequestPublishPermissions", mImmediatelyRequestPublishPermissions);
		}
		
		public void restoreStatus(Bundle savedInstanceState)
		{
			if (savedInstanceState != null && !savedInstanceState.isEmpty())
			{
				mOriginalUrl = savedInstanceState.getString("mOriginalUrl");
				mRequestedPublishPermissions = savedInstanceState.getBoolean("mRequestedPublishPermissions");
				mImmediatelyRequestPublishPermissions = savedInstanceState.getBoolean("mImmediatelyRequestPublishPermissions");
			}
		}
		
		// Overridden method
		@Override
        public void call(Session session, SessionState state, Exception exception)
        {
			if (Global.LOG_MODE) Global.log(" -> FacebookSessionStatusListener: Received state: " + state.toString());
			if (Global.LOG_MODE && exception != null) Global.log(" -> FacebookSessionStatusListener: got exception: \n" + ExceptionUtils.getExceptionStackString(exception));
			
        	if (state == SessionState.OPENED)
        	{
        		// Check if we must immediately request publish permissions
        		if (mImmediatelyRequestPublishPermissions)
        		{
        			if (Global.LOG_MODE) Global.log(Global.TAG, " -> FacebookSessionStatusListener.call(): Completed obtaining read permissions after publish failure, requesting publish permissions");
        			setRequestedPublishPermissions(true);
        			setImmediatelyRequestPublishPermissions(false);
        			getFacebookPublishAccess(FacebookSessionStatusListener.this, getRequiredPublishPermissions());
        		}
        		else
        		{	
	        		// Return read token here
	        		onFacebookAccess(session.getAccessToken(), session.getExpirationDate(), session.getPermissions());
	
	        		// If debugging, get the user info
	        		if (Global.LOG_MODE)
	                {
		                Request.executeMeRequestAsync(session, new GraphUserCallback()
		                {
		                    @Override
		                    public void onCompleted(GraphUser user, Response response)
		                    {
	                        	if (user != null)
	                        	{
	                        		Global.log("Got Facebook user id: " + user.getId());
	                        		Global.log("Got Facebook email:" + user.asMap().get("email"));
	                        		Global.log("Got Facebook bday: " + user.getBirthday());
	                        	}
		                    }
		                });
	                }
        		}
        	}
        	else if(state == SessionState.OPENED_TOKEN_UPDATED)
            {
                // Return publishing token here
        		onFacebookAccess(session.getAccessToken(), session.getExpirationDate(), session.getPermissions());
            }
        	else if (state == SessionState.CLOSED_LOGIN_FAILED)
        	{
        		if (exception instanceof FacebookOperationCanceledException && 
        				exception.getMessage().contains("The app must ask for a basic read permission at install time."))
        		{
        			if (Global.LOG_MODE) Global.log(Global.TAG, " -> FacebookSessionStatusListener.call(): Restarting the publish process because we do not have read permissions");
        			mStatusListener.setRequestedPublishPermissions(false);
        			mStatusListener.setImmediatelyRequestPublishPermissions(true);
        			getFacebookReadAccess(mStatusListener, getRequiredReadPermissions(false));
        		}
        		onFacebookError(exception);
        	}
        }
		
		@Override
		public void onFacebookAccess(String accessToken, Date expirationDate, List<String> permissions)
		{
			if (!mRequestedPublishPermissions)
			{
				FacebookLoginInfo loginInfo = new FacebookLoginInfo();
				Uri uri = Uri.parse(mOriginalUrl);
	    		loginInfo.mRedirectUrl = UrlUtils.getQueryParameter(uri, Global.PARAM_REDIRECT);
	    		loginInfo.mClientId = UrlUtils.getQueryParameter(uri, Global.PARAM_CLIENT_ID);
	    		loginInfo.mCreate = UrlUtils.getQueryParameter(uri, Global.PARAM_CREATE);
	    		loginInfo.mMinAge = UrlUtils.getQueryParameter(uri, Global.PARAM_MIN_AGE);
	    		loginInfo.mToken = accessToken;
	    		loginInfo.mTokenExpirationDate = expirationDate;
	    		loginInfo.mTokenPermissions = permissions;
	    		
	    		FacebookLoginAsyncTask facebookLoginTask = new FacebookLoginAsyncTask(SnaprKitFragment.this);
	    		facebookLoginTask.execute(loginInfo);
			}
			else
			{
				FacebookPublishInfo publishInfo = new FacebookPublishInfo();
				Uri uri = Uri.parse(mOriginalUrl);
	    		publishInfo.mRedirectUrl = UrlUtils.getQueryParameter(uri, Global.PARAM_REDIRECT);
	    		publishInfo.mSnaprToken = UrlUtils.getQueryParameter(uri, Global.PARAM_ACCESS_TOKEN);
	    		publishInfo.mToken = accessToken;
	    		publishInfo.mTokenExpirationDate = expirationDate;
	    		publishInfo.mTokenPermissions = permissions;
	    		
	    		FacebookPublishAsyncTask facebookPublishTask = new FacebookPublishAsyncTask(SnaprKitFragment.this);
	    		facebookPublishTask.execute(publishInfo);
			}
		}
		
		@Override
		public void onFacebookError(Exception e)
		{
			if (Global.LOG_MODE) Global.log("onFacebookError: Got exception " + e);
			Uri uri = Uri.parse(mOriginalUrl);
			String redirectUrl = UrlUtils.getQueryParameter(uri, Global.PARAM_REDIRECT);
			if (!mRequestedPublishPermissions)
			{
				SnaprKitFragment.this.onSnaprFacebookLoginError(e, redirectUrl);
			}
			else
			{
				SnaprKitFragment.this.onSnaprFacebookPublishError(e, redirectUrl);
			}
		}
		
		public void setOriginalUrl(String originalUrl)
		{
			mOriginalUrl = originalUrl;
		}
		
		public void setRequestedPublishPermissions(boolean requestedPublishPermissions)
		{
			mRequestedPublishPermissions = requestedPublishPermissions;
		}
		
		public void setImmediatelyRequestPublishPermissions(boolean immediatelyRequestPublishPermissions)
		{
			mImmediatelyRequestPublishPermissions = immediatelyRequestPublishPermissions;
		}
	}
	
	interface OnFacebookAccessListener
	{
		public void onFacebookAccess(String accessToken, Date expirationDate, List<String> permissions);
		public void onFacebookError(Exception e);
	}
	
    // The action performed when logging in via Facebook
    private Action facebookLoginAction = new Action()
    {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> facebookLoginAction: Received URL " + url);
    		
    		// Parse redirect URL and add it to Facebook status listener
    		Uri uri = Uri.parse(url);
    		String minAge = UrlUtils.getQueryParameter(uri, Global.PARAM_MIN_AGE);
    		List<String> permissions = getRequiredReadPermissions((minAge != null && minAge.length() > 0));
    		
    		// Add items
    		//mStatusListener = new FacebookSessionStatusListener();
    		mStatusListener.setOriginalUrl(url);
    		mStatusListener.setRequestedPublishPermissions(false);
    		
    		// Request read access
    		getFacebookReadAccess(mStatusListener, permissions);
			
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    // The action performed when publishing in via Facebook
    private Action facebookPublishAction = new Action() {
    	@Override
    	public void run(String url)
    	{
    		// Log
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> facebookPublishAction: Received URL " + url);
    		
    		// Parse redirect URL and add it to Facebook status listener
    		List<String> permissions = getRequiredPublishPermissions();
    		//mStatusListener = new FacebookSessionStatusListener();
    		mStatusListener.setOriginalUrl(url);
    		mStatusListener.setRequestedPublishPermissions(true);
    		
    		// Request publish access
    		getFacebookPublishAccess(mStatusListener, permissions);
			
			// Log
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	}
    };
    
    private String unpackRedirectUrl(String redirectUrl)
    {
    	boolean isSnaprRedirect = redirectUrl.startsWith(Global.URL_SNAPR_REDIRECT);
		if (isSnaprRedirect)
		{
			Uri redirectUri = Uri.parse(redirectUrl);
			redirectUrl = redirectUri.getQueryParameter(Global.PARAM_REDIRECT_URL);
		}
		
		return redirectUrl;
    }
    
	@Override
	public void onSnaprFacebookLogin(UserInfo userInfo, String redirectUrl)
	{
		// Log
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> onSnaprLogin: Received redirect URL " + redirectUrl);
		
		// Unpack snapr://redirect URLs
		redirectUrl = unpackRedirectUrl(redirectUrl);
		
		// Update redirect url
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", userInfo.mAccessToken);
		params.put("display_username", userInfo.mDisplayUserName);
		params.put("snapr_user", userInfo.mSnaprUserName);
		redirectUrl = UrlUtils.appendParamsToUrl(redirectUrl, params);
		
		// Redirect
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> Redirecting to " + redirectUrl);
		mWebView.loadUrl(redirectUrl);
		
		// Log
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	}
	
	@Override
	public void onSnaprFacebookLoginError(Throwable e, String redirectUrl)
	{
		// Log
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> onSnaprLoginError: Received redirect URL " + redirectUrl + " and error");		
		if (Global.LOG_MODE) Global.log(ExceptionUtils.getExceptionStackString(e));
		
		// Unpack snapr://redirect URLs
		redirectUrl = unpackRedirectUrl(redirectUrl);
		
		// Create redirect url
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("error", e.getMessage());
		redirectUrl = UrlUtils.appendParamsToUrl(redirectUrl, params);
		
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> Redirecting to " + redirectUrl);
		mWebView.loadUrl(redirectUrl);
		
		// Log
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	}
	
	@Override
	public void onSnaprFacebookPublish(String redirectUrl)
	{
		// Log
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> onSnaprFacebookPublish: Received redirect URL " + redirectUrl);
		
		// Unpack snapr://redirect URLs
		redirectUrl = unpackRedirectUrl(redirectUrl);
		
		// Redirect
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> Redirecting to " + redirectUrl);
		mWebView.loadUrl(redirectUrl);
		
		// Log
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	}

	@Override
	public void onSnaprFacebookPublishError(Throwable e, String redirectUrl)
	{
		// Log
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> onSnaprFacebookPublishError: Received redirect URL " + redirectUrl + " and error");		
		if (Global.LOG_MODE) Global.log(ExceptionUtils.getExceptionStackString(e));
		
		// Unpack snapr://redirect URLs
		redirectUrl = unpackRedirectUrl(redirectUrl);
		
		// Create redirect url
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("error", e.getMessage());
		redirectUrl = UrlUtils.appendParamsToUrl(redirectUrl, params);
		
		// Redirect
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> Redirecting to " + redirectUrl);
		mWebView.loadUrl(redirectUrl);
		
		// Log
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	}
	
	// ------------------------------------------------------------------------
	// Library interface
	// ------------------------------------------------------------------------
	
	// Callback interface
	public interface SnaprKitListener
    {
    	public void onPageFinished(String url);
    	public void onSnaprKitParent(String url);
    }
	
	// Callback setting function
	public void setSnaprKitListener(SnaprKitListener listener)
	{
		mSnaprKitListener = listener;
	}
	
	// Moves to the previous page
	public boolean goBack()
	{
		return onBackPressed();
	}
	
	// Start via the normal flow with default page
	public void startNormalFlow()
	{
		startNormalFlow(null);
	}
	
	// Start via the normal flow with custom page
	public void startNormalFlow(String pageUrl)
	{		
		// Load the user info
		loadUserInfo();
		
		// Set default URL
		if (pageUrl == null)
		{
			pageUrl = getStartupUrl();
		}
		
		// Load the URL
		loadUserProvidedUrl(pageUrl);
	}
	
	private void loadUserProvidedUrl(String pageUrl)
	{
       	// Ensure we have a full URL
    	String fullUrl = UrlUtils.relativeUrlToFullUrl(pageUrl);
		
    	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Loading url " + pageUrl);
    	
    	// Load it
    	if (fullUrl!=null)
    	{
    		mWebView.loadUrl(fullUrl);
    	}
	}
	
	// Start via the share flow
	public void startShareFlow(String sharedPictureFileName)
	{
		// Declare
		String url = null;
		
		// Load the user info
		loadUserInfo();
		
		// Set the shared picture filename
		mSharedPictureFileName = sharedPictureFileName;
		
		// Check if we have an image passed in as data
        SharedPictureInfo info = getSharedPictureInfo();
        if (info != null)
        {
        	// Log
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Started via share picture flow");
        	
        	// Get the info data
        	String fileName = info.getFileName();
        	double latitude = info.getLatitude();
        	double longitude = info.getLongitude();
        	 
			// Check if we succeeded
			if(latitude == 0 && longitude == 0)
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found no location information within picture");
				
				// Get the location from location providers
				// This will load later so we do not need to load url below
				getLocationAction.run(null);
			}
			else
			{
				// Log
				if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Found location information in picture (" + latitude + "," + longitude + ")");
				
	        	// Get the URL
		        url = getSharePictureUrl(fileName, latitude, longitude, null, null, null, null, null, null);	
			}
        }
        
        // Check url
        if (url != null)
        {
        	loadUserProvidedUrl(url);
        }
	}
	
	// Move to the specified page URL
	// Url is provided using base name from HTML build
	public void goToPage(String pageUrl)
	{
		loadUserProvidedUrl(pageUrl);
	}	
	
	/**
	 * Set the Snapr user info
	 * @param displayUserName The username to display on screen
	 * @param userName The Snapr username
	 * @param accessToken The Snapr access token
	 */
	public void setUserInfo(String displayUserName, String snaprUserName, String accessToken)
	{
		// Set user info
		mDisplayUserName = displayUserName;
		mSnaprUserName = snaprUserName;
		mAccessToken = accessToken;
		
		// Set the shared preferences
		UserInfoUtils.saveUserInfo(getContext(), mDisplayUserName, mSnaprUserName, mAccessToken);
	}
	
	/**
	 * Clears the Snapr user info
	 */
	public void clearUserInfo()
	{
		// Clear class members
		mDisplayUserName = null;
		mSnaprUserName = null;
		mAccessToken = null;
		
		// Clear shared preferences
		UserInfoUtils.clearUserInfo(getContext());
	}	
	
	private void loadUserInfo()
	{
		if (!haveUserInfo())
		{
	    	// Load user info from preferences
	    	String userInfo[] = new String[3];
	    	UserInfoUtils.loadUserInfo(SnaprKitFragment.this.getActivity(), userInfo);
	    	mDisplayUserName = userInfo[0];
	    	mSnaprUserName = userInfo[1];
	    	mAccessToken = userInfo[2];
		}
	}
	
    public void setFilterPackPath(String path)
    {
    	mFilterPackPath = path;
    }

    public void setStickerPackPath(String path)
    {
    	mStickerPathPath = path;
    }
    
	/**
	 * Gets the full collection of configurations for effects
	 * @param config The configuration to set.
	 */
	public Map<String, SnaprSetting> getFxSettings()
	{
		return mSettings;
	}
	
	/**
	 * Sets the full collection of configurations for effects, clearing out any existing entries.
	 * @param config The configuration to set.
	 */
	public void setFxSettings(Map<String, SnaprSetting> settings)
	{
		mSettings = settings;
	}
}