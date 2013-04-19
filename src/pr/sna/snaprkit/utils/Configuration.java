package pr.sna.snaprkit.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import pr.sna.snaprkit.Global;

import android.content.Context;
import android.content.res.AssetManager;

public class Configuration
{
	// Constants
	private static final String CONFIGURATION_FILENAME = "snaprkit.properties";
	private static final String PROPERTY_APP_NAME = "appName";
	private static final String PROPERTY_LOGGING_ENABLED = "loggingEnabled";
	private static final String PROPERTY_ENVIRONMENT = "environment";
	private static final String PROPERTY_FACEBOOK_APP_ID_LIVE = "facebookAppIdLive";
	private static final String PROPERTY_FACEBOOK_APP_ID_DEV = "facebookAppIdDev";
	private static final String PROPERTY_AUTO_CLEAR_FAILED_UPLOADS = "autoClearFailedUploads";
	private static final String PROPERTY_LOCATION_TIMEOUT_INTERVAL = "locationTimeoutInterval";
	private static final String PROPERTY_LOCATION_FAILURE_TIMEOUT_INTERVAL = "locationFailureTimeoutInterval";
	private static final String PROPERTY_VALUE_TRUE = "true";
	private static final String PROPERTY_VALUE_FALSE = "false";
	
	// Statics
	private static Configuration sInstance;
	
	// Members
	private static Properties mProperties;
	
	// Private constructor
	private Configuration()
	{
	}
	
	// Private alternate constructor
	private Configuration(Context context)
	{
		AssetManager am = context.getResources().getAssets();
		
		// Set fallback properties in case some props are missing in the stream
		mProperties = getDefaultProperties(); 

		InputStream isConfig;
		try
		{
			isConfig = am.open(CONFIGURATION_FILENAME,Context.MODE_PRIVATE);
			mProperties.load(isConfig);
		}
		catch (IOException e)
		{
			throw new java.lang.IllegalArgumentException("SnaprKit configuration could not be loaded!");
		}
		
		// Notify user of all configuration errors
		String errors = getValidationErrors();
		if (errors != null && errors.length() > 0)
		{
			throw new java.lang.IllegalArgumentException("SnaprKit configuration is invalid!\n"+errors);
		}
	}
	
	private static Properties getDefaultProperties()
	{
		// Declare
		Properties props = new Properties();
		
		// Initialize each property to its default value
		props.setProperty(PROPERTY_APP_NAME, "");
		props.setProperty(PROPERTY_LOGGING_ENABLED, "false");
		props.setProperty(PROPERTY_ENVIRONMENT, "dev");
		props.setProperty(PROPERTY_FACEBOOK_APP_ID_LIVE, "");
		props.setProperty(PROPERTY_FACEBOOK_APP_ID_DEV, "");
		props.setProperty(PROPERTY_AUTO_CLEAR_FAILED_UPLOADS, "false");
		props.setProperty(PROPERTY_LOCATION_TIMEOUT_INTERVAL, "20"); // 20 seconds
		props.setProperty(PROPERTY_LOCATION_FAILURE_TIMEOUT_INTERVAL, "300"); // 5 minutes
		
		// Return
		return props;
	}
	
	// Initialization
	public static void init(Context context)
	{
		if (sInstance == null)
		{
			sInstance = new Configuration(context);
		}
	}
	
	// 
	public static synchronized Configuration getInstance()
	{
		return sInstance;
	}
	
	private String getProperty(String name)
	{
		if (mProperties != null)
		{
			return mProperties.getProperty(name);
		}
		else
		{
			return null;
		}
	}
	
	private String getValidationErrors()
	{	
		// Declare
		String errors = "";
		
		// Perform validity checks
		if (mProperties != null)
		{
			// Validate fields one by one and output error messages
			if (!hasValidBooleanValue(PROPERTY_AUTO_CLEAR_FAILED_UPLOADS))
			{
				errors += "Invalid boolean value for field autoClearFailedUploads!\n";
			}
			
			if (!hasValidBooleanValue(PROPERTY_LOGGING_ENABLED))
			{
				errors +="Invalid boolean value for field loggingEnabled!\n";
			}
			
			// Validate fields one by one and output error messages
			if (!hasValidIntValue(PROPERTY_LOCATION_TIMEOUT_INTERVAL))
			{
				errors += "Non integer value for field locationTimeoutInterval!\n";
			}
			
			// Validate fields one by one and output error messages
			if (!hasValidIntValue(PROPERTY_LOCATION_FAILURE_TIMEOUT_INTERVAL))
			{
				errors += "Non integer value for field locationFailureTimeoutInterval!\n";
			}
			
			String validEnvironmentValues[] = {"dev","dev-android","live","live-android"};
			if (!hasValidStringValue(PROPERTY_ENVIRONMENT, validEnvironmentValues))
			{
				errors += "Invalid string value for field environment!\n";
			}
			
			return errors;
		}
		
		// Invalid configuration
		return "Properties failed to load!";
	}
	
	public String getAppName()
	{
		return getProperty(PROPERTY_APP_NAME);
	}
	
	public String getEnvironment()
	{
		return getProperty(PROPERTY_ENVIRONMENT);
	}
	
	public boolean getLoggingEnabled()
	{
		return getBooleanProperty(PROPERTY_LOGGING_ENABLED);
	}
	
	public String getFacebookAppId()
	{
		return isLiveEnvironment()?getFacebookAppIdLive():getFacebookAppIdDev();
	}
	
	public String getFacebookAppIdLive()
	{
		return getProperty(PROPERTY_FACEBOOK_APP_ID_LIVE);
	}
	
	public String getFacebookAppIdDev()
	{
		return getProperty(PROPERTY_FACEBOOK_APP_ID_DEV);
	}
	
	public boolean getAutoClearFailedUploads()
	{
		return getBooleanProperty(PROPERTY_AUTO_CLEAR_FAILED_UPLOADS);
	}
	
	public int getLocationTimeoutInterval()
	{
		return getIntProperty(PROPERTY_LOCATION_TIMEOUT_INTERVAL);
	}
	
	public int getLocationFailureTimeoutInterval()
	{
		return getIntProperty(PROPERTY_LOCATION_FAILURE_TIMEOUT_INTERVAL);
	}

	public boolean getBooleanProperty(String propertyName)
	{
		String result = getProperty(propertyName);
		
		if (PROPERTY_VALUE_TRUE.equals(result))
		{
			return true;
		}
		else if (PROPERTY_VALUE_FALSE.equals(result))
		{
			return false;
		}
		else
		{
			return false;
		}
	}
	
	public int getIntProperty(String propertyName)
	{
		String resultString = getProperty(propertyName);
		int resultInt = 0;
		
		try
		{
			resultInt = Integer.parseInt(resultString);
		}
		catch (NumberFormatException e)
		{
			Global.log(ExceptionUtils.getExceptionStackString(e));
		}
		
		return resultInt;
	}
	
	private boolean hasValidBooleanValue(String propertyName)
	{
		String result = getProperty(propertyName);
		
		if (PROPERTY_VALUE_TRUE.equals(result))
		{
			return true;
		}
		if (PROPERTY_VALUE_FALSE.equals(result))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	private boolean hasValidIntValue(String propertyName)
	{
		String resultString = getProperty(propertyName);
		
		try
		{
			Integer.parseInt(resultString);
		}
		catch (NumberFormatException e)
		{
			return false;
		}
		
		return true;
	}
	
	private boolean hasValidStringValue(String propertyName, String validPropertyValues[])
	{
		if (validPropertyValues == null) return false;
		
		String propertyValue = getProperty(propertyName);
		
		for(String s: validPropertyValues)
		{
			if (s != null && s.equals(propertyValue))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isLiveEnvironment()
	{
		return getEnvironment().startsWith("live");
	}
}