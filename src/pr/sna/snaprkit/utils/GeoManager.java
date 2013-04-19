package pr.sna.snaprkit.utils;

import java.util.Date;

import pr.sna.snaprkit.Global;
import pr.sna.snaprkit.R;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

@SuppressLint("InlinedApi")
public class GeoManager implements LocationListener {
  
	private Context mContext;
	private GeoListener mGeoListener;
	private LocationManager mLocationManager;
	private Location mCurrentLocation;
	private Handler mHandler = new Handler();
	private boolean mIsActive;
    private int mCaller;
    //private static Date sLastGeolocationFailureDate;
    private TransitionDialog mTransitionDialog = null;
    private static final int LOCATION_DISTANCE_TOLERANCE = 100;             // plus minus tolerance for distance 
    private static final int LOCATION_TIME_TOLERANCE = 1 * 30 * 1000;		// plus minus tolerance time (really just minus)
    //private static final int LOCATION_TIMEOUT = 1 * 20 * 1000;				// location timeout
    //private static final int LOCATION_FAILURE_TIMEOUT = 5 * 60 * 1000;		// time to avoid querying location after failure
    
    private Runnable mTimeOutRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			mIsActive = false;
			if (Global.LOG_MODE) Global.log(Global.TAG, "Setting last location failure date to " + new Date());
			setLastGeolocationFailureDate(new Date());
			if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
	    	mLocationManager.removeUpdates(GeoManager.this);
	    	mGeoListener.onTimeOut(mCurrentLocation, mCaller);
	    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
		}
	};
    
	private void setLastGeolocationFailureDate(Date lastGeolocationFailureDate)
	{
		// Save the date to shared preferences
		if (lastGeolocationFailureDate != null)
		{
			int mode = Context.MODE_PRIVATE;
			if (android.os.Build.VERSION.SDK_INT >= 11) mode = mode | Context.MODE_MULTI_PROCESS;
			SharedPreferences prefs = mContext.getSharedPreferences(Global.SNAPR_PREFERENCES, mode);
			SharedPreferences.Editor prefEditor = prefs.edit();
			prefEditor.putLong(Global.SNAPR_PREFERENCES_LAST_GEOLOCATION_FAILURE_DATE, lastGeolocationFailureDate.getTime());
			prefEditor.commit();
		}
	}
	
	private Date getLastGeolocationFailureDate()
	{
		long lastGeolocationFailureDateLong = 0;
		
		int mode = Context.MODE_PRIVATE;
		if (android.os.Build.VERSION.SDK_INT >= 11) mode = mode | Context.MODE_MULTI_PROCESS;
		
		SharedPreferences prefs = mContext.getSharedPreferences(Global.SNAPR_PREFERENCES, mode);
		lastGeolocationFailureDateLong = prefs.getLong(Global.SNAPR_PREFERENCES_LAST_GEOLOCATION_FAILURE_DATE, 0);
		
		if (lastGeolocationFailureDateLong == 0)
			return null;
		else
			return new Date(lastGeolocationFailureDateLong);
	}
	
    /**
     * Create a new LocationFinder object
     * @param context Activity context of the calling activity
     */
    public GeoManager(Context context)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	// Initialize members
    	mContext = context;
    	mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
    	
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }

	public void getLocation(GeoListener listener, int caller)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	// Do not perform geolocation if we failed recently
    	long timeout = pr.sna.snaprkit.utils.Configuration.getInstance().getLocationFailureTimeoutInterval()*1000;
    	Date now = new Date();
    	Date lastGeolocationFailureDate = getLastGeolocationFailureDate();
    	if ((lastGeolocationFailureDate != null) && (now.getTime() - lastGeolocationFailureDate.getTime()) <= timeout)
    	{
    		if (Global.LOG_MODE) Global.log(Global.TAG, "Skipping the location check because we are under failure threshold: \nLastFailureDate: " + lastGeolocationFailureDate +"\nNow: " + now + "\nTimeout: " + timeout);  
    		
    		listener.onTimeOut(mCurrentLocation, caller);
    		return;
    	}
    	else
    	{
    		if (Global.LOG_MODE) Global.log(Global.TAG, "Location check values: \nLastFailureDate: " + lastGeolocationFailureDate +"\nNow: " + now +"\nTimeout: " + timeout);
    	}
    	
    	mIsActive = true;
    	mCaller = caller;
    	
    	mGeoListener = listener;
        
    	if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        {	       
	        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	        updateLocation(location);
        }
      
        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {   
	        Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	        updateLocation(location);
        }
    	
    	mLocationManager.requestLocationUpdates(
	            LocationManager.GPS_PROVIDER, 
	            0, 
	            0, 
	            this);
		
    	mLocationManager.requestLocationUpdates(
    			LocationManager.NETWORK_PROVIDER, 
    			0, 
    			0,  
    			this);
    	
    	timeout = pr.sna.snaprkit.utils.Configuration.getInstance().getLocationTimeoutInterval()*1000;
    	
    	if (Global.LOG_MODE) Global.log("Location Timeout: " + timeout);
    	mHandler.postDelayed(mTimeOutRunnable, timeout);
    	
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    public interface GeoListener
    {
    	public void onLocation(Location location, int caller);
    	public void onTerminate(Location location, int caller);
    	public void onTimeOut(Location location, int caller);
    }
    
    /**
     * @return Boolean value indicating whether geolocation is in progress
     */
    public boolean isActive()
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	return mIsActive;
    }
    
    /**
     * Determines whether a provided location meets the distance acceptance criteria 
     * @return Boolean indicating whether the location meets the distance criteria
     */
    public static boolean isLocationAcceptable(Location location)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    	// Determine if the location accuracy is acceptable
    	return ((location != null) && (location.getAccuracy() < LOCATION_DISTANCE_TOLERANCE)); 
    }
    
    public void resetTimeOut()
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	mHandler.removeCallbacks(mTimeOutRunnable);
    	getLocation(mGeoListener, mCaller);
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    public void terminate()
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	if (mIsActive != false)
    	{
	    	mIsActive = false;
	    	mHandler.removeCallbacks(mTimeOutRunnable);
	    	mLocationManager.removeUpdates(this);
	    	mGeoListener.onTerminate(mCurrentLocation, mCaller);
    	}
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }

	/** Determines whether one Location reading is better than the current Location fix
	  * @param location  The new Location that you want to evaluate
	  * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	  */
	private boolean isBetterLocation(Location location, Location currentBestLocation)
	{
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
		
		if (location == null) return false;
		
		// Discard if old
		if (System.currentTimeMillis() > location.getTime() + LOCATION_TIME_TOLERANCE)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
			return false;
		}
		
		// Compare 
		if (currentBestLocation == null)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod() + " returning true");
		    return true;
		}
		else
		{
			boolean isBetter = (location.getAccuracy() <= mCurrentLocation.getAccuracy()); 
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod() + " returning " + isBetter);
			return isBetter;
		}
	}
    
    /**
     * Updates the current location member based on criteria
     * @param loc Latest current location
     */
    private void updateLocation(Location location)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	// Check if we have a better location
    	if((location != null) && (isBetterLocation(location, mCurrentLocation)))
    	{
    		// Update location
    		mCurrentLocation = location;
    		
    		// Notify listener
    		mGeoListener.onLocation(location, mCaller);
    		
    		// Terminate if location is within desired bounds
    		if (GeoManager.isLocationAcceptable(location))
    		{
    			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod() + ": Terminating because we have a high quality location...");
    			terminate();
    		}
    	}
    	
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    @Override
    public void onLocationChanged(Location location)
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
        updateLocation(location);
        if (location == null)
        {
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Received null location!");
        }
        else
        {
        	if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Received location " + location.getLatitude() + "," + location.getLongitude() + " with accuracy " + location.getAccuracy());
        }
        if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }

    @Override
    public void onProviderDisabled(String provider)
    {
    }

    @Override
    public void onProviderEnabled(String provider)
    {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
    }
    
    public void showLocationPendingDialog()
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	if (mTransitionDialog != null)
    	{
    		if (Global.LOG_MODE) Global.log(Global.TAG, " -> Error: Already have a mPleaseWaitDialog" + Global.getCurrentMethod());
    		return;
    	}
    	
    	String message = mContext.getString(R.string.snaprkit_location_determining);
    	String title = mContext.getString(R.string.snaprkit_please_wait);
    	mTransitionDialog = new TransitionDialog(mContext);
    	mTransitionDialog.showTransitionDialog(message, title);
	    
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
    
    public void cancelLocationPendingDialog()
    {
    	if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
    	
    	if (mTransitionDialog != null)
    	{
    		mTransitionDialog.cancelTransitionDialog();
    		mTransitionDialog = null;
    	}
    	
    	if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
    }
}
