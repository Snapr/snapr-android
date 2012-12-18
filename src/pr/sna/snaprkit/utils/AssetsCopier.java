package pr.sna.snaprkit.utils;

import pr.sna.snaprkit.Global;
import pr.sna.snaprkit.R;
import android.app.Activity;
import android.os.AsyncTask;

@SuppressWarnings("unused")
public class AssetsCopier extends AsyncTask<Void, Void, Void>
{
	// Members
	private Activity mParent = null;
	private TransitionDialog mTransitionDialog;
	private boolean mShowDialogs = false;
	private AssetCopierListener mListener = null;
	
	public AssetsCopier(Activity parentActivity, AssetCopierListener listener)
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsCopier: Constructor called...");
		mParent = parentActivity;
		mListener = listener;
		mShowDialogs = true;
		mTransitionDialog = new TransitionDialog(mParent);
	}
	
	public AssetsCopier(Activity parentActivity, AssetCopierListener listener, boolean showDialogs)
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsCopier: Constructor2 called...");
		mParent = parentActivity;
		mListener = listener;
		mShowDialogs = showDialogs;
		mTransitionDialog = new TransitionDialog(mParent);
	}
	
	@Override
	protected void onPostExecute(Void result)
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsCopier: onPostExecute called...");
		
		super.onPostExecute(result);
		
		// Cancel the progress dialog
		if (mShowDialogs) mTransitionDialog.cancelTransitionDialog();
		
		// Indicate completion
		if (mListener != null) mListener.onComplete();
	}

	@Override
	protected void onPreExecute()
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsCopier: onPreExecute called...");
		
		super.onPreExecute();
		if (mShowDialogs) mTransitionDialog.showTransitionDialog(mParent.getString(R.string.snaprkit_assets_loading), null);
	}

	@Override
	protected Void doInBackground(Void... params)
	{
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsCopier: doInBackground called...");
		
		AssetUtils.copyAssetsToCache(mParent);
		
		if (Global.LOG_MODE && Global.LOG_ASSET_COPY) Global.log("AssetsCopier: doInBackground exited...");
		
		return null;
	}
	
	public interface AssetCopierListener
	{
		public void onComplete();
	}
}