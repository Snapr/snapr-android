package pr.sna.snaprkit.utils;

import pr.sna.snaprkit.Global;
import android.content.Context;
import android.content.SharedPreferences;

public class UserInfoUtils
{    
    // Load shared preferences into class members
    public static void loadUserInfo(Context context, String[] userInfo)
    {
    	// Get the shared preferences object
        SharedPreferences prefs = context.getSharedPreferences(Global.SNAPR_PREFERENCES, Context.MODE_PRIVATE);
        
        // Load preferences
        String displayUserName = prefs.getString(Global.SNAPR_PREFERENCES_DISPLAY_USERNAME, "");
        String snaprUserName = prefs.getString(Global.SNAPR_PREFERENCES_SNAPR_USERNAME, "");
        String accessToken = prefs.getString(Global.SNAPR_PREFERENCES_ACCESS_TOKEN, "");
        userInfo[0] = displayUserName;
        userInfo[1] = snaprUserName;
        userInfo[2] = accessToken;
    }
    
    public static void saveUserInfo(Context context, String displayUserName, String snaprUserName, String accessToken)
    {
    	// Save shared preferences
		SharedPreferences prefs = context.getSharedPreferences(Global.SNAPR_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString(Global.SNAPR_PREFERENCES_DISPLAY_USERNAME, displayUserName);
		prefEditor.putString(Global.SNAPR_PREFERENCES_SNAPR_USERNAME, snaprUserName);
		prefEditor.putString(Global.SNAPR_PREFERENCES_ACCESS_TOKEN, accessToken);
		prefEditor.commit();
    }
    
    public static void clearUserInfo(Context context)
    {
    	// Clear the user info
    	SharedPreferences prefs = context.getSharedPreferences(Global.SNAPR_PREFERENCES, Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = prefs.edit();
		prefEditor.putString(Global.SNAPR_PREFERENCES_DISPLAY_USERNAME, "");
		prefEditor.putString(Global.SNAPR_PREFERENCES_SNAPR_USERNAME, "");
		prefEditor.putString(Global.SNAPR_PREFERENCES_ACCESS_TOKEN, "");
		prefEditor.commit();
    }
    
    public static boolean haveCredentials(String userName, String accessToken)
    {
    	return (userName!= null && userName.length() > 0 && accessToken != null && accessToken.length() > 0);
    }
    
    public static boolean haveUserInfo(String displayUserName, String snaprUserName, String accessToken)
    {
    	return (snaprUserName!= null && snaprUserName.length() > 0 && accessToken != null && accessToken.length() > 0);
    }
    
}
