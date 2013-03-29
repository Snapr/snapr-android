# SnaprKit 3.0 Android Installation Instructions

## 1. Introduction
This document will guide you through the creation of a sample application using the SnaprKit Android library.

The library is based on a fragment architecture to allow integration into any Activity.

SnaprKit has a minimum API Level of 8 and a target API of 14.

### 1.1 Prerequisites
I assume you already have the Android environment installed on your system and Eclipse with the required ADT plugin.

See [http://developer.android.com/sdk/installing.html](http://developer.android.com/sdk/installing.html) and [http://developer.android.com/sdk/eclipse-adt.html](http://developer.android.com/sdk/eclipse-adt.html) if you need instructions on how to setup the Android environment.

## 2. Workspace setup
First, we’ll need to import the Eclipse library project into our workspace.

### 2.1 Import Project
Open Eclipse and select `Import` from the file menu.

### 2.2 Set Up Import
The import dialog will appear. From the list of import options, select `Existing Projects into Workspace,` and then click `Next.`

### 2.3 Select the File
In the new dialog, click on the `Select archive file` radio button and then click the `Browse` button on the right. From here, select the `SnaprKit3.0.zip` file included with this document. Click on the `Finish` button at the bottom of the dialog.

A new Android library project called `SnaprKit` will be created in your current workspace. This is the required library project which you must include in your application if you want to use Snapr.

## 3. Include Snapr in a new Application
Here’s a step by step guide on how to include SnaprKit in a new Android application.

### 3.1 Create a new Android project
Just create a new Android project as usual from Eclipse.

#### 3.2 Project references
Once the new project has been created, open the project properties and navigate to the `Android` section.

Click the `Add...` button of the `Library` subsection and select `SnaprKit` from the dialog.

Next, navigate to the `Java Build Path` section of the project properties dialog and click on `Add JARs...` button of the `Libraries` subsection. From here, select all the .jar files included in the `libs` folder of the SnaprKit project:

    commmons-logging-1.1.1.jar
    commons-io-2.0.1.jar
    apache-mime4j-0.6.1.jar
    htttpmime-4.1.1.jar
    android-support-v4.jar
    SanselanAndroid.jar

### 3.3 Assets
Please copy the following assets from the sample project into the corresponding folders in your project:

* __HTML files__ - Copy the HTML files from the sample project’s `/assets/snaprkit_html/` directory and move them to the same directory in your application. These are required for proper library functioning.
* __Filter files__ - Copy the filter files from the sample project’s `/assets/filter_packs/` directory and move them to the same directory in your application.
* __Sticker files__ - Copy the sticker files from the sample project’s `/assets/sticker_packs/` directory and move them to the same directory in your application.

### 3.4 Resources
Depending on the project, we may provide additional image resouces to customize the look and feel of the graphics effects module. You should copy the indicated files from the `res/drawable-*hdpi` folder(s) of the sample project to the corresponding folders in your project.

### 3.5 AndroidManifest.xml
Add some entries to the manifest file of your application.

### 3.5.1 Minimum SDK
Add the following lines within the <manifest> tag

    <uses-sdk android:targetSdkVersion="14" android:minSdkVersion="8" />

### 3.5.2 Permissions
To function correctly, SnaprKit requires a variety of permissions. To grant the necessary permissions, add these entries inside the `AndroidManifest.xml` `<manifest>` tag in your app:

    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />

### 3.5.3. Hardware Features
The following hardware features entries let the Android Market know what hardware your app is using:

    <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.location"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.screen.portrait"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.touchscreen"
        android:required="false" />
    <uses-feature
        android:name="android.hardware.wifi"
        android:required="false" />

### 3.5.4 Activity declaration
Then, inside the <application> tag, add a reference to the following activities:

    <activity android:name="pr.sna.snaprkit.WebViewExternalActivity"  
        android:screenOrientation="portrait"   
        android:label="Browsing External Site"  
        android:configChanges="keyboardHidden|orientation">  
    </activity>

    <activity
        android:name="pr.sna.snaprkitfx.SnaprImageEditFragmentActivity"
        android:configChanges="keyboardHidden|orientation"
        android:screenOrientation="portrait"
        android:theme="@style/Application.Theme.NoTitleBar">
    </activity>
    
    <activity android:name="com.facebook.LoginActivity"
        android:theme="@android:style/Theme.Translucent.NoTitleBar"
        android:label="@string/app_name">
    </activity>

### 3.5.5 Services declaration
Also inside the <application> tag, add a reference to the following services:

    <service
        android:name="pr.sna.snaprkit.UploadService">   
    </service>  
        
    <service  
        android:name="pr.sna.snaprkit.GeolocationService"  
        android:process="android.process.snaprkit.geolocation">   
    </service>

## 4. Layout
To add the SnaprKit fragment to an activity, add the following to the layout file:

    <fragment
        android:id="@+id/snaprKitFragment"
        android:name="pr.sna.snaprkit.SnaprKitFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

## 5. Activity
### 5.1 Change manifest
In the manifest, locate the activity entry for the activity that contains the SnaprKit fragment. Add the attributes `android:screenOrientation`, `android:configChanges` and `android:theme`, as shown below:

    <activity
        android:name=".MainActivity"
        android:screenOrientation="portrait"
        android:configChanges="keyboardHidden|orientation"
        android:theme="@android:style/Theme.NoTitleBar">
    </activity>

### 5.2 Setup Base Class
Open the source file for the activity that contains the SnaprKit fragment and change the class declaration to make the class extend `FragmentActivity` rather than `Activity`.

## 5.3 Initialize
### 5.3.1 Static Fragment
If you are using the fragment in an activity without swapping it, then add the following code in the `onCreate` method of your activity:

    // Add this to the class members
    private SnaprKitFragment mSnaprKitFragment = null;

    // Add this to the onCreate method
    // Find the SnaprKit fragment
    mSnaprKitFragment = (SnaprKitFragment)getSupportFragmentManager().findFragmentById(R.id.snaprKitFragment);
	
    // Start the normal activity flow
    mSnaprKitFragment.startNormalFlow();`

### 5.3.2 Dynamic Fragment
If you are swapping the fragment in and out of the activity, then do the following:

* Create a new fragment class called `MySnaprKitFragment` that extends the `SnaprKitFragment` class included in SnaprKit.
* In the `onCreate` event, perform the one-time tasks that do not involve the UI (such as external log in, if this is a requirement for your project).
* In the `onCreateActivity` event, perform the tasks that need to be repeated every time the fragment is brought back, or the tasks that deal with the UI.

### 5.3.3 Add Back button support
To support webview navigation using the back button, override the `onBackPressed` method in the activity that contains the SnaprKit fragment:

    @Override
    public void onBackPressed()
    {
        // Call the SnaprKit back pressed and check if it's handled
        if (!mSnaprKitFragment.goBack())
        {
            // Let the OS handle the back press
            super.onBackPressed();
        }
    }

## 6. Backend Server Configuration
You can configure the app to communicate with either the Snapr development environment or the Snapr production environment.

To change between these, edit the `src/pr/sna/snaprkit/Global.java` file in the SnaprKit library and set the `ENVIRONMENT` string to either `dev-android` (development) or `live-android` (production).

## 7. Native Facebook Configuration

SnaprKit 3.0 now allows you to use native Facebook login and sharing using Facebook SDK 3.0 and higher.

### 7.1 Facebook SDK Configuration

If your app already includes the Facebook SDK library, then you should adjust the SnaprKit dependencies so that SnaprKit uses the same Facebook library as your main project.

Here is how to reconfigure SnaprKit to use the appropriate library. These instructions assume that you are using Eclipse as your IDE.

1. In the Package Explorer view, right click on the `SnaprKit` project and select `Properties` from the context menu.
2. In the `Properties` dialog, select the `Android` menu option on the left. The Android options will show on the right.
3. Locate the `Library` section on the right, and find the `Facebook SDK` entry in the list of libraries. Click on the `FacebookSDK` entry and click the `Remove` on the right.
4. Click the `Add` button, select your Facebook SDK project from the list of projects and then click `OK`.
5. Clean / refresh the project for the changes to take effect.

### 7.2 Facebook Native Login and Sharing

To enable these features you must take the following steps:

1. Go to [developers.facebook.com](developers.facebook.com) and create two Facebook applications: one for production and one for development.
2. Make a note of the Facebook application IDs from step 1 above. Edit the `src/pr/sna/snaprkit/Global.java` file in the SnaprKit library and enter the IDs under `FACEBOOK_APP_ID_PROD` and `FACEBOOK_APP_ID_DEV`.
3. Once the APP_ID values are in place, SnaprKit will use native Facebook functionality. If you don’t provide Facebook application IDs, SnaprKit will fall back to the Facebook web login and share.

## 8. API Reference
The following table shows the list of functions available:

### 8.1 SnaprKitFragment

The following functions are available through SnaprKitFragment:

Function | Description
-------- | --------
void startNormalFlow() | Starts the normal flow using the default menu page
void startNormalFlow(String pageUrl) | Starts the normal flow using the specified page URL. The URL must be specified either as a full URL or as a relative URL starting with `index.html#`.
void startShareFlow(String filename) | Starts the share flow using the specified shared filename. We check the file for EXIF latitude and longitude and if none is present we get the current location. The first page displayed on this flow is always the share page
void goToPage(String pageUrl) | Moves the webview to the specified page. The URL must be specified either as a full URL or as a relative URL starting with `index.html#`.
boolean goBack() | Moves the webview back. Returns `true` if the webview successfully moved back, `false` otherwise. This is useful to support built-in back button on Android. In the `onBackPressed()` event of the activity attempt to move back using this function. If this returns `false`, call `super.onBackPressed()`.
void setUserInfo(String displayUserName, String snaprUserName, String accessToken) | Allows the library user to set the display username, the Snapr username and the access token
void clearUserInfo() | Clears the current user info
void setCredentials(String username, String accessToken) | Deprecated. Please use `setUserInfo(String username, String accessToken)` instead.
void clearCredentials() | Deprecated. Please use `clearUserInfo()` instead.
void setSnaprKitListener(SnaprKitListener listener) | Provides a listener to listen to SnaprKit events. See section on SnaprKitListener below for more info.
void prepareSnaprAssets(AssetCopierListener listener, boolean showDialogs) | Deprecated, no longer necessary.

The SnaprKitListener contains the following event functions:

Function | Description
-------- | --------
void onPageFinished(String url) | Triggers when a page has finished loading. You can find out which page finished via the url parameter
void onSnaprKitParent(String url) | Triggers when the build sends a SnaprKitParent message, and provides the full url through the url parameter