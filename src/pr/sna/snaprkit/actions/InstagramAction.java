package pr.sna.snaprkit.actions;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.SnaprKitFragment.Action;
import pr.sna.snaprkit.utils.ImageUtils;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.widget.Toast;

public class InstagramAction implements Action {

	private Context mContext;
	
	private static boolean sAlreadyShared = false;
	private static String sLocalFilePath; // The path of the latest photo (set in UploadService)
	
	public static void setLatestPicture(String pLocalFilePath) {
		sAlreadyShared = false;
		sLocalFilePath = pLocalFilePath.intern();
	}
	
	public InstagramAction(Context context) {
		mContext = context;
	}

	/**
	 * Runs when the snapr://instagram intent is broadcasted. This receiver will
	 * spin off a new thread to upload the photo to instagram
	 */
	@Override
	public void run(String url) {
		if(!hasInstagramInstaller(mContext)) {
			//Instagram is not installed, throw up a toast notifying the user
			Toast t = Toast.makeText(mContext, R.string.snaprkit_error_instagram_not_installed, Toast.LENGTH_LONG);
			t.show();
		} else if(!sAlreadyShared){
			//Get an instagram compatible version of the photo taken
			String pathToUpload = ImageUtils.getInstagramCompatiblePhoto(sLocalFilePath);
			if(pathToUpload == null) return;
			
			//Upload the photo by constructing an intent
			Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
			//shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			shareIntent.setType("image/*");                 
			shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(pathToUpload));
			shareIntent.setPackage("com.instagram.android");
			mContext.startActivity(shareIntent);
		}
	}

	/**
	 * @param context
	 * @return
	 * 		True if instagram is installed on the device, false otherwise
	 */
	@SuppressWarnings("unused")
	private boolean hasInstagramInstaller(Context context) {
		boolean result = false;

		try {
			ApplicationInfo info = context.getPackageManager().getApplicationInfo(
					"com.instagram.android", 0);
			result = true;
		} catch (NameNotFoundException e) {
			result = false;
		}
		return result;
	}

}
