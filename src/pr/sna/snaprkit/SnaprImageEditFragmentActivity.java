package pr.sna.snaprkit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import pr.sna.snaprkit.SnaprImageEditFragment.FragmentListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class SnaprImageEditFragmentActivity extends FragmentActivity implements FragmentListener {

	public static final int EDIT_IMAGE = 20;

	public static final int EDIT_IMAGE_REQUEST_CODE = 20; 
	
	/**
	 * The path for the source image.
	 */
	public static final String EXTRA_FILEPATH = "EXTRA_FILEPATH";
	/**
	 * The filename to use for the file. All images are saved in the Environment.DIRECTORY_PICTURES folder.
	 */
	public static final String EXTRA_OUTPUT = "EXTRA_OUTPUT";
	/**
	 * A boolean to determine whether a photo was just taken to supply the source image.
	 */
	public static final String EXTRA_TOOK_PHOTO = "EXTRA_TOOK_PHOTO";
	public static final String EXTRA_TOOK_PHOTO_TIMESTAMP = "EXTRA_TOOK_PHOTO_TIMESTAMP";
	public static final String EXTRA_ANALYTICS = "EXTRA_ANALYTICS";
	/**
	 * An extra containing data to determine what effect are (un)locked.
	 */
	public static final String EXTRA_EFFECT_CONFIG = "EXTRA_EFFECT_CONFIG";
	
	public static final String ANALYTIC_PAGE_LOADED = "snaprkit-parent://coremetrics/?tag_type=Page View&category_id=ANDROID_VSPINK_APP_PICS_FILTERS_P&page_id=ANDROID_VSPINK_APP_PICS_FILTERS_SELECT_P";
	public static final String ANALYTIC_CANCEL_EVENT = "snaprkit-parent://coremetrics/?tag_type=Manual Link Click&cm_re=spring2012-_-sub-_-cancel_image_upload& link_name=CANCEL IMAGE UPLOAD&page_id=ANDROID_VSPINK_APP_PICS_FILTERS_SELECT_P"; 
	public static final String ANALYTIC_SHARE_EVENT = "snaprkit-parent://coremetrics/?tag_type=Conversion Event& link_name=CANCEL IMAGE UPLOAD& category_id=VSPINK_APP_PICS_FILTERS_SELECTED_P&element_id=ANDROID_APP_FILTERS_SELECTED_(FILTER_NAME)&action_type=2";
	
	private ArrayList<String> mAnalytics = new ArrayList<String>();
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * on create
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.snaprkit_edit_layout);
        
		SnaprImageEditFragment fragment = (SnaprImageEditFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
		fragment.setFragmentListener(this);

    }
    
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * start activity
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	/**
	 * Use this to start the Activity if you want to overwrite the original file (perhaps when taking a photo)
	 * @see #startActivity(Activity, String, boolean, String, List)
	 */
	public static void startActivity(Activity activity, String filePath, boolean justTookPhotoExtra, long justTookPhotoTimestamp) {
		startActivity(activity, filePath, justTookPhotoExtra, justTookPhotoTimestamp, (List<EffectConfig>)null);
	}
	
	/**
	 * Same as {@link #startActivity(Activity, String, boolean)} but pass in an optional configuration for the effects.
	 * @see #startActivity(Activity, String, boolean, String, List)
	 */
	public static void startActivity(Activity activity, String filePath, boolean justTookPhotoExtra, long justTookPhotoTimestamp, List<EffectConfig> config) {
		startActivity(activity, filePath, justTookPhotoExtra, justTookPhotoTimestamp, null, config);
	}
	
	/**
	 * Use this to start the Activity if you want to create a new file (perhaps when picking from a gallery)
	 * @see #startActivity(Activity, String, boolean, String, List)
	 */
	public static void startActivity(Activity activity, String filePath, boolean justTookPhotoExtra, long justTookPhotoTimestamp, String outputFilename) {
		startActivity(activity, filePath, justTookPhotoExtra, justTookPhotoTimestamp, outputFilename, null);
	}
	
	/**
	 * Same as {@link #startActivity(Activity, String, boolean, String, EffectConfig)} but pass in an optional configuration for the effects
	 * @see #buildDefaultConfig(Context)
	 */
	public static void startActivity(Activity activity, String filePath, boolean justTookPhotoExtra, long justTookPhotoTimestamp, String outputFilename, List<EffectConfig> config) {
		Intent intent = getIntentForStartActivity(activity, filePath, justTookPhotoExtra, justTookPhotoTimestamp, outputFilename, config);
		activity.startActivityForResult(intent, EDIT_IMAGE);
	}

	
	/**
	 * Same as {@link #startActivity(Activity, String, boolean)} but simply returns the Intent to start the Activity without actually starting it.
	 * @see #startActivity(Activity, String, boolean, String, List)
	 */
	public static Intent getIntentForStartActivity(Activity activity, String filePath, boolean justTookPhotoExtra, long justTookPhotoTimestamp) {
		return getIntentForStartActivity(activity, filePath, justTookPhotoExtra, justTookPhotoTimestamp, (List<EffectConfig>)null);
	}
	
	/**
	 * Same as {@link #startActivity(Activity, String, boolean, List)} but simply returns the Intent to start the Activity without actually starting it.
	 * @see #startActivity(Activity, String, boolean, String, List)
	 */
	public static Intent getIntentForStartActivity(Activity activity, String filePath, boolean justTookPhotoExtra, long justTookPhotoTimestamp, List<EffectConfig> config) {
		return getIntentForStartActivity(activity, filePath, justTookPhotoExtra, justTookPhotoTimestamp, null, config);
	}
	
	/**
	 * Same as {@link #startActivity(Activity, String, boolean, String)} but simply returns the Intent to start the Activity without actually starting it.
	 * @see #startActivity(Activity, String, boolean, String, List)
	 */
	public static Intent getIntentForStartActivity(Activity activity, String filePath, boolean justTookPhotoExtra, long justTookPhotoTimestamp, String outputFilename) {
		return getIntentForStartActivity(activity, filePath, justTookPhotoExtra, justTookPhotoTimestamp, outputFilename, null);
	}
	
	/**
	 * Same as {@link #startActivity(Activity, String, boolean, String, List)} but simply returns the Intent to start the Activity without actually starting it.
	 * @see #startActivity(Activity, String, boolean, String, List)
	 */
	public static Intent getIntentForStartActivity(Context context, String filePath, boolean justTookPhotoExtra, long justTookPhotoTimestamp, String outputFilename, List<EffectConfig> config) {
		Intent intent = new Intent(context, SnaprImageEditFragmentActivity.class);
		intent.putExtra(EXTRA_FILEPATH, filePath);
		intent.putExtra(EXTRA_TOOK_PHOTO, justTookPhotoExtra);
		if (justTookPhotoExtra) intent.putExtra(EXTRA_TOOK_PHOTO_TIMESTAMP, justTookPhotoTimestamp);
		if (outputFilename != null) intent.putExtra(EXTRA_OUTPUT, outputFilename);
		if (config != null) intent.putExtra(EXTRA_EFFECT_CONFIG, config instanceof Serializable ? (Serializable) config : new ArrayList<EffectConfig>(config));
		return intent;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * fragment listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	@Override public void onEditComplete(String filePath) {
		Intent i = new Intent();
		i.putExtra(EXTRA_FILEPATH, filePath);
		onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_SHARE_EVENT);
		i.putExtra(EXTRA_ANALYTICS, getAnalytics());
		setResult(RESULT_OK, i);
		finish();
	}

	@Override public void onCancel() {
		onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_PAGE_LOADED);
		Intent i = new Intent();
		i.putStringArrayListExtra(EXTRA_ANALYTICS, getAnalytics());
		setResult(RESULT_CANCELED,i);
		finish();
	}

	@Override public void onAddAnalytic(String value) {
		getAnalytics().add(value);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * Analytics
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	public ArrayList<String> getAnalytics() {
		return mAnalytics;
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * Effect configuration
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	/**
	 * A simple POJO class that bundles data into a configuration for effects. A configuration requires at the id of the effect, in order
	 * to match it against a specific {@link SnaprEffect}. Valid ids can be found as public constants in {@link SnaprEffect}. An effect 
	 * configuration defaults to being unlocked. This behaviour can be changed by supplying an explicit value indicating whether the 
	 * effect is (un)locked. If an effect is locked, an appropriate message MUST be given for displaying to the user.
	 *  
	 */
	public static final class EffectConfig implements Serializable {
		private static final long serialVersionUID = -7415383060387922436L;
		
		public final int mEffectId;
		public boolean mIsLocked;
		public String mUnlockMessage;
		
		/**
		 * Creates a new EffectConfig instance based on the given id and initialises it to being unlocked.
		 * Shorthand for {@link #EffectConfig(effectId, false)}.
		 * @param effectId The id of the effect.
		 * @see {@link #EffectConfig(int, boolean, String)}.
		 */
		public EffectConfig(int effectId) {
			this(effectId, false, null); /* unlocked by default */
		}
		
		/**
		 * Creates a new EffectConfig instance based on the given id, isLocked value and unlockMessage.
		 * Note that if isLocked == TRUE, you should also supply an unlockMessage.
		 * @param effectId The id of the effect. @see {@link SnaprEffect#EFFECT_*}.
		 * @param isLocked Whether the effect is (un)locked.
		 * @param unlockMessage The message that should be displayed in case the effect is locked.
		 * @see SnaprEffect#EFFECT_BONBON_ID
		 * @see SnaprEffect#EFFECT_BUBBLE_ID
		 * @see SnaprEffect#EFFECT_CONFETTI_ID
		 * @see SnaprEffect#EFFECT_DREAMER_ID
		 * @see SnaprEffect#EFFECT_GUMDROP_ID
		 * @see SnaprEffect#EFFECT_LATE_NIGHTS_ID
		 * @see SnaprEffect#EFFECT_LOVE_LETTER_ID
		 * @see SnaprEffect#EFFECT_LUV_ID
		 * @see SnaprEffect#EFFECT_ORIGINAL_ID
		 * @see SnaprEffect#EFFECT_RETRO_ID
		 * @see SnaprEffect#EFFECT_SUMMER_ID
		 * @see SnaprEffect#EFFECT_SUNRISE_ID
		 * @see SnaprEffect#EFFECT_SWEET_TALK_ID
		 */
		public EffectConfig(int effectId, boolean isLocked, String unlockMessage) {
			if (isLocked && unlockMessage == null) throw new IllegalArgumentException("A locked effect should provide an unlock message!");
			mEffectId = effectId;
			mIsLocked = isLocked;
			mUnlockMessage = unlockMessage; 
		}
	}
	
}
