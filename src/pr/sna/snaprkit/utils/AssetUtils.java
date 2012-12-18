package pr.sna.snaprkit.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import pr.sna.snaprkit.Global;
import pr.sna.snaprkit.utils.AssetsCopier.AssetCopierListener;

import android.app.Activity;
import android.content.res.AssetManager;

@SuppressWarnings("unused")
public class AssetUtils
{	
	public void prepareSnaprAssets(Activity activity, AssetCopierListener listener, boolean showDialogs)
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetUtils.prepareSnaprAssets: " + pr.sna.snaprkit.utils.FileUtils.isDirectoryPresent(getSnaprAssetsDirectory(activity)));
		if (android.os.Build.VERSION.SDK_INT >= Global.SDK_HONEYCOMB && android.os.Build.VERSION.SDK_INT < Global.SDK_JELLYBEAN && !AssetUtils.areSnaprAssetsPresent(activity))
    	{
    		// Copy assets and load			
    		AssetsCopier assetsCopier = new AssetsCopier(activity, listener, showDialogs); 
    		assetsCopier.execute();
    	}
	}
	
	public static void copyAssetsToCache(Activity activity)
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.copyAssetsToCache: called...");
		copyFileOrDir(activity, "snaprkit_html");
	}
	
	public static boolean areSnaprAssetsPresent(Activity activity)
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.areSnaprAssetsPresent: returning " + pr.sna.snaprkit.utils.FileUtils.isDirectoryPresent(getSnaprAssetsDirectory(activity)));
		return pr.sna.snaprkit.utils.FileUtils.isDirectoryPresent(getSnaprAssetsDirectory(activity));
	}
	
	public static String getSnaprDataDirectory(Activity activity)
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.getSnaprDataDirectory: returning " + "/data/data/" + activity.getPackageName() + "/");
		return "/data/data/" + activity.getPackageName() + "/"; // must end in slash
	}
	
	public static String getSnaprAssetsDirectory(Activity activity)
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.getSnaprDataDirectory: returning " + getSnaprDataDirectory(activity) + "snaprkit_html/");
		return getSnaprDataDirectory(activity) + "snaprkit_html/"; // must end in slash
	}
	
	private static void copyFileOrDir(Activity activity, String path)
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.copyFileOrDir: started with path " + path + "...");
		AssetManager assetManager = activity.getAssets();
		String assets[] = null;
		try
		{
			assets = assetManager.list(path);
			if (assets.length == 0)
			{
				if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.copyFileOrDir: no file results found for path, must be file, copying file " + path + "...");
				copyFile(activity, path);
			}
			else
			{
				if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.copyFileOrDir: assets found...");
				String fullPath = getSnaprDataDirectory(activity) + path;
				if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.copyFileOrDir: full path is " + fullPath + "...");
				File dir = new File(fullPath);
				if (!dir.exists())
				{
					if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.copyFileOrDir: full path did not exist creating " + fullPath + "...");
					dir.mkdir();
				}
				for (int i = 0; i < assets.length; ++i)
				{
					if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.copyFileOrDir: recursing using path " + path + "/" + assets[i] + "...");
					copyFileOrDir(activity, path + "/" + assets[i]);
				}
            }
        }
		catch (IOException e)
		{
			if (Global.LOG_MODE) Global.log("I/O Exception" + e.getMessage());
		}
	}

	private static void copyFile(Activity activity, String filename)
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.copyFile: copying file with filename " + filename + "...");
		
		AssetManager assetManager = activity.getAssets();

		InputStream in = null;
		OutputStream out = null;
		try
		{
			in = assetManager.open(filename);
			String newFileName = "/data/data/" + activity.getPackageName() + "/" + filename;
			if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsUtils.copyFile: using newFileName " + newFileName + "...");
			out = new FileOutputStream(newFileName);

			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1)
			{
				out.write(buffer, 0, read);
			}
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log("AssetsUtils.copyFile: failed with exception..." + e);
		}
	}
}