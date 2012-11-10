package pr.sna.snaprkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import nz.co.juliusspencer.android.JSAImageUtil;
import pr.sna.snaprkit.SnaprImageEditFragmentActivity.EffectConfig;
import pr.sna.snaprkit.SnaprSwipeGestureDetector.SnaprSwipeGestureListener;
import pr.sna.snaprkit.effect.pink.PinkEffectBonBon;
import pr.sna.snaprkit.effect.pink.PinkEffectBubble;
import pr.sna.snaprkit.effect.pink.PinkEffectConfetti;
import pr.sna.snaprkit.effect.pink.PinkEffectDreamer;
import pr.sna.snaprkit.effect.pink.PinkEffectGumDrop;
import pr.sna.snaprkit.effect.pink.PinkEffectLateNights;
import pr.sna.snaprkit.effect.pink.PinkEffectLoveLetter;
import pr.sna.snaprkit.effect.pink.PinkEffectPNK;
import pr.sna.snaprkit.effect.pink.PinkEffectRetro;
import pr.sna.snaprkit.effect.pink.PinkEffectSummer;
import pr.sna.snaprkit.effect.pink.PinkEffectSunrise;
import pr.sna.snaprkit.effect.pink.PinkEffectSweetTalk;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SnaprImageEditFragment extends Fragment implements OnClickListener, SnaprSwipeGestureListener {

	private FragmentListener mFragmentListener;
	private boolean mViewsInitialised;

	private ImageView mEditedImageView;
	private LinearLayout mHorizontalLayout;
	private TextView mMessageTextView;
	private View mMessageLayout;
	private View mConfirmButton;

	private static final String CHOSEN_PHOTO_FILENAME = "source.jpg";
	private static final String CHOSEN_EFFECT = "CHOSEN_EFFECT";

	private List<EffectConfig> mEffectConfigs;
	private List<SnaprEffect> mEffects = new ArrayList<SnaprEffect>();

	private Bitmap mCurrentBitmap;
	private String mOriginalFilePath;
	private String mSaveFilename;
	private long mImageRequestTimestamp;

	private ProgressDialog mDialog;

	private int mLastChoice = R.string.snaprkit_original;

	private int mLength;

	private Handler h = new Handler();

	private GestureDetector mGestureDetector;
	private View.OnTouchListener mGestureListener;

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * on create view
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if((null!=savedInstanceState)&&(0!=savedInstanceState.getInt(CHOSEN_EFFECT)))
			mLastChoice = savedInstanceState.getInt(CHOSEN_EFFECT);

		return inflater.inflate(R.layout.snaprkit_edit_fragment, container, false);
	}

	@SuppressWarnings("unchecked") public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		super.setRetainInstance(true);

		// bind the widget views
		mHorizontalLayout = (LinearLayout) getView().findViewById(R.id.horizontal_layout);
		mEditedImageView = (ImageView) getView().findViewById(R.id.edited_image);
		mMessageTextView = (TextView) getView().findViewById(R.id.message_textview);
		mMessageLayout = getView().findViewById(R.id.message_layout);

		mLength = ((WindowManager) SnaprKitApplication.getInstance().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth();
		mEditedImageView.getLayoutParams().height = mLength;
		mEditedImageView.getLayoutParams().width = mLength;
		
		mConfirmButton = getView().findViewById(R.id.confirm_button);
		mConfirmButton.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				// save bitmap and change file
				new SaveEditedBitmapToFileAsyncTask().execute();
			}
		});

		getView().findViewById(R.id.cancel_button).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				mFragmentListener.onCancel();
			}
		});

		/* 
		 * Particularly on small & ldpi devices (with a relative rectangular aspect ratio) there may be less space available in
		 * the height than width. For those cases, we're attaching a ViewTreeObserver to get notified of layout changes. We'll
		 * remove it as soon as both the width and height are known. If after that the width and height are equal, there was 
		 * enough space available height-wise, so we don't need to do anything else. However, if they're not identical, use the
		 * smallest value to set the size of the ImageView. 
		 * 
		 *  Since mMessageLayout is sized based on the bounds of the ImageView, it will get resized correctly too.
		 */
		mEditedImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override public void onGlobalLayout() {
				int width = mEditedImageView.getWidth();
				int height = mEditedImageView.getHeight();
				if (width == 0 || height == 0) return;
				mEditedImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
				if (width == height) return;
				mEditedImageView.getLayoutParams().width = Math.min(width, height);
				mEditedImageView.getLayoutParams().height = Math.min(width, height);
			}
		});
		
		Bundle extras = getActivity().getIntent().getExtras();
		if(null!=extras.getString(SnaprImageEditFragmentActivity.EXTRA_OUTPUT)) {
			mSaveFilename = extras.getString(SnaprImageEditFragmentActivity.EXTRA_OUTPUT);
		}
		
		if (extras.getSerializable(SnaprImageEditFragmentActivity.EXTRA_EFFECT_CONFIG) != null) {
			mEffectConfigs = (List<EffectConfig>) extras.getSerializable(SnaprImageEditFragmentActivity.EXTRA_EFFECT_CONFIG);
		}
		
		if(null!=getActivity().getIntent().getStringExtra(SnaprImageEditFragmentActivity.EXTRA_FILEPATH)) {
			mOriginalFilePath = getActivity().getIntent().getStringExtra(SnaprImageEditFragmentActivity.EXTRA_FILEPATH);

			mDialog = ProgressDialog.show(getActivity(), "", getString(R.string.snaprkit_loading), true);
			if(getActivity().getIntent().getBooleanExtra(SnaprImageEditFragmentActivity.EXTRA_TOOK_PHOTO, true)) { 
				mImageRequestTimestamp = getActivity().getIntent().getLongExtra(SnaprImageEditFragmentActivity.EXTRA_TOOK_PHOTO_TIMESTAMP, -1);
				if (mImageRequestTimestamp == -1) throw new IllegalArgumentException("Time stamp of image request missing!");
				
				// Tell the media scanner about the new file so that it is
				// immediately available to the user.

				MediaScannerConnection.scanFile(
						SnaprKitApplication.getInstance(),
						new String[] { mOriginalFilePath.toString() }, null,
						new MediaScannerConnection.OnScanCompletedListener() {
							public void onScanCompleted(String path, Uri uri) {
								loadImage();
							}
						});
			} else {
				loadImage();
			}
		}

		// Gestures
		mGestureDetector = new GestureDetector(new SnaprSwipeGestureDetector(this));
		mGestureListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (mGestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		};
		mEditedImageView.setOnTouchListener(mGestureListener);

		mViewsInitialised = true;

		if(mLastChoice!=R.string.snaprkit_original) {
			selectEffectView();
		}

		// Create effect list
		createEffectsList();

		// Create and configure effect views
		addEffectsViews();
		
		updateViews();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if(mLastChoice!=R.string.snaprkit_original)
			outState.putInt(CHOSEN_EFFECT, mLastChoice);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onDestroy() {

		if(null!=mDialog)
			mDialog.dismiss();

		super.onDestroy();
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * effects view management
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */


	private void addEffectsViews() {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		for(SnaprEffect effect : mEffects) {
			View effectItem = inflater.inflate(R.layout.snaprkit_effect_item, null);

			ImageView iv = ((ImageView) effectItem.findViewById(R.id.effect_imageview));
			iv.setImageResource(effect.mImageResId);

			TextView tv = ((TextView) effectItem.findViewById(R.id.effect_textview));
			tv.setText(effect.mNameResId);

			iv.setTag(effect.mNameResId);
			effectItem.setTag(effect.mNameResId);
			iv.setOnClickListener(this);

			mHorizontalLayout.addView(effectItem, mEffects.indexOf(effect));
		}
	}

	private void createEffectsList() {
		mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_ORIGINAL_ID, R.drawable.snaprkit_noeffect_button, R.string.snaprkit_original, true, getConfiguration(SnaprEffect.EFFECT_ORIGINAL_ID)));
		mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_LUV_ID, R.drawable.snaprkit_pinkpnkeffect_button, R.string.snaprkit_pnk, false, getConfiguration(SnaprEffect.EFFECT_LUV_ID)));
		mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_CONFETTI_ID, R.drawable.snaprkit_pinkconfettieffect_button, R.string.snaprkit_confetti, false, getConfiguration(SnaprEffect.EFFECT_CONFETTI_ID)));
		mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_BONBON_ID, R.drawable.snaprkit_pinkbonboneffect_button, R.string.snaprkit_bonbon, false, getConfiguration(SnaprEffect.EFFECT_BONBON_ID)));
		mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_SUNRISE_ID, R.drawable.snaprkit_pinksunriseeffect_button, R.string.snaprkit_sunrise, false, getConfiguration(SnaprEffect.EFFECT_SUNRISE_ID)));
		mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_LATE_NIGHTS_ID, R.drawable.snaprkit_pinklatenightseffect_button, R.string.snaprkit_late_nights, false, getConfiguration(SnaprEffect.EFFECT_LATE_NIGHTS_ID)));
		mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_DREAMER_ID, R.drawable.snaprkit_pinkdreamereffect_button, R.string.snaprkit_dreamer, false, getConfiguration(SnaprEffect.EFFECT_DREAMER_ID)));
		mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_SWEET_TALK_ID, R.drawable.snaprkit_pinksweettalkeffect_button, R.string.snaprkit_sweet_talk, false, getConfiguration(SnaprEffect.EFFECT_SWEET_TALK_ID)));
		mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_GUMDROP_ID, R.drawable.snaprkit_pinkgumdropeffect_button, R.string.snaprkit_gumdrop, false, getConfiguration(SnaprEffect.EFFECT_GUMDROP_ID)));
		mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_LOVE_LETTER_ID, R.drawable.snaprkit_pinklovelettereffect_button, R.string.snaprkit_love_letter, false, getConfiguration(SnaprEffect.EFFECT_LOVE_LETTER_ID)));
		/* locked by default */ mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_SUMMER_ID, R.drawable.snaprkit_pinksummereffect_button, R.string.snaprkit_summer, false, getConfiguration(SnaprEffect.EFFECT_SUMMER_ID)));
		/* locked by default */ mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_RETRO_ID, R.drawable.snaprkit_pinkretroeffect_button, R.string.snaprkit_retro, false, getConfiguration(SnaprEffect.EFFECT_RETRO_ID)));
		/* locked by default */ mEffects.add(new SnaprEffect(SnaprEffect.EFFECT_BUBBLE_ID, R.drawable.snaprkit_pinkbubbleeffect_button, R.string.snaprkit_bubble, false, getConfiguration(SnaprEffect.EFFECT_BUBBLE_ID)));
	}

	/* Gets the configuration for the effect with given id, or a default one */
	private EffectConfig getConfiguration(int effectId) {
		if (mEffectConfigs == null || mEffectConfigs.isEmpty()) return getDefaultEffectConfiguration(effectId);
		for (EffectConfig config : mEffectConfigs) {
			if (config.mEffectId != effectId) continue;
			return config;
		}
		return getDefaultEffectConfiguration(effectId);
	}

	/* Gets the default configuration for an effect */
	private EffectConfig getDefaultEffectConfiguration(int effectId) {
		switch(effectId) {
			case SnaprEffect.EFFECT_SUMMER_ID: return new EffectConfig(effectId, true, getString(R.string.snaprkit_not_enough_pink_points_need_1000));
			case SnaprEffect.EFFECT_RETRO_ID: return new EffectConfig(effectId, true, getString(R.string.snaprkit_not_enough_pink_points_need_10000));
			case SnaprEffect.EFFECT_BUBBLE_ID: return new EffectConfig(effectId, true, getString(R.string.snaprkit_not_enough_pink_points_need_25000));
			/* all other effects are unlocked by default */
			default: return new EffectConfig(effectId); 
		}
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 *  update views
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	
	private void updateViews() {
		if (!isAdded() || !mViewsInitialised) return;

		SnaprEffect selected = null;
		for(SnaprEffect effect : mEffects) {
			View effectItem = mHorizontalLayout.findViewWithTag(effect.mNameResId);
			((ImageView) effectItem.findViewById(R.id.chosen_imageview)).setSelected(effect.mChosen);
			((ImageView) effectItem.findViewById(R.id.locked_overlay_imageview)).setVisibility(effect.mLocked ? View.VISIBLE : View.INVISIBLE);
			if (effect.mChosen) selected = effect;
		}
		
		mMessageLayout.setVisibility(selected.mLocked ? View.VISIBLE : View.INVISIBLE);
		if (selected.mUnlockMessage != null) mMessageTextView.setText(selected.mUnlockMessage);
		mConfirmButton.setEnabled(!selected.mLocked);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 *  image setup
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */


	/**
	 * Loads the image for the first time
	 */
	private void loadImage() {
		fetchOriginalImage();

		h.post(new Runnable() {
			@Override
			public void run() {
				mEditedImageView.setImageBitmap(mCurrentBitmap);
				if(null!=mDialog)
					mDialog.dismiss();
			}
		});

		mFragmentListener.onAddAnalytic(SnaprImageEditFragmentActivity.ANALYTIC_PAGE_LOADED);
	}

	private void fetchOriginalImage() {
		if(null!=mCurrentBitmap) {
			getResizedCroppedPhoto();
		} else {
			mCurrentBitmap = null;

			int scale = JSAImageUtil.getLoadLargerImageScale(new File(mOriginalFilePath), mLength, mLength);
			Options opts = new Options();
			opts.inSampleSize = scale;
			opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
			mCurrentBitmap = JSAImageUtil.loadImageFile(new File(mOriginalFilePath), opts);

			if(null==mCurrentBitmap) {
				Toast.makeText(getActivity(), R.string.snaprkit_unable_to_load_image_try_another_, Toast.LENGTH_SHORT).show();
				mFragmentListener.onCancel();
			} else {

				// Check the orientation of the photo in the Mediastore
				try {
					int orientation = 0;
					
					/*
					 * Because of a bug on some Android devices, we're not querying the database on the location or name of the file, but request
					 * all images added after a specific timestamp (that gets initialised whenever the camera intent is fired off). This will give
					 * us either one or two results. The case with a single result is trivial. However, when two results are returned, it means that
					 * the image stored out the requested output location doesn't contain proper exif data or values in the MediaStore. Hence, we're 
					 * grabbing those from the 'other' result by means of a simple Math.max() call (which works since the incorrect orientation data
					 * defaults to '0').
					 * 
					 * See: http://code.google.com/p/android/issues/detail?id=19268
					 */
					// list to store the orientations
					List<Integer> orientations = new ArrayList<Integer>();
					Cursor cursor = null;
					try {
						// query params
						String[] proj = { MediaStore.Images.Media.ORIENTATION};
						String selection = mImageRequestTimestamp > 0 ? MediaStore.Images.Media.DATE_ADDED + ">=?" : MediaStore.Images.Media.DATA + "=?";
						String[] selectionArgs = { mImageRequestTimestamp > 0 ? Long.toString(mImageRequestTimestamp / 1000l) : mOriginalFilePath };
						String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
						// execute query
				        cursor = SnaprKitApplication.getInstance().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, selection, selectionArgs, sortOrder);
				        cursor.moveToFirst();
						while(!cursor.isAfterLast()) {
							orientations.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION)));
							cursor.moveToNext();
						}
			    	} catch (Exception e) {
			    		e.printStackTrace();
			    		throw new RuntimeException("couldn't initialize media store cursor!");
			    	} finally {
			    		if (cursor != null && !cursor.isClosed()) cursor.close();
			    	}

					// get orientation from single result, or the largest from two
					if (!orientations.isEmpty()) orientation = orientations.size() == 1 ? orientations.get(0) : Math.max(orientations.get(0), orientations.get(1));
					
					if(orientation!=0) {
						// Rotate and save
						Matrix m = new Matrix();
						m.preRotate(orientation);
						int width = mCurrentBitmap.getWidth();
						int height = mCurrentBitmap.getHeight();
						Bitmap b = Bitmap.createBitmap(mCurrentBitmap, 0, 0, width, height, m, false);
						OutputStream out;
						out = new FileOutputStream(mOriginalFilePath);
						b.compress(CompressFormat.JPEG, 100, out);
						ExifInterface eiw = new ExifInterface(mOriginalFilePath);
						eiw.setAttribute(ExifInterface.TAG_ORIENTATION, "0");
						eiw.saveAttributes();

						// Tell the media scanner about the new file so that it is
						// immediately available to the user.
						MediaScannerConnection.scanFile(
								SnaprKitApplication.getInstance(),
								new String[] { mOriginalFilePath.toString() }, null,
								new MediaScannerConnection.OnScanCompletedListener() {
									public void onScanCompleted(String path, Uri uri) {
									}
								});

						mCurrentBitmap = b;
					}			
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				mCurrentBitmap = SnaprPhotoHelper.cropBitmap(mCurrentBitmap);

				saveResizedCroppedPhoto();

				if(mLastChoice!=R.string.snaprkit_original) {
					applyEffect(mLastChoice, mCurrentBitmap);
				}
			}
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * effect click listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	@Override
	public void onClick(View v) {
		if(mLastChoice != (Integer) v.getTag()) {

			mLastChoice = (Integer) v.getTag();

			selectEffectView();

			new PerformEffect((Integer) v.getTag(), true).execute();
		}
	}

	private void selectEffectView() {
		for(SnaprEffect effect : mEffects) {
			effect.mChosen = effect.mNameResId == mLastChoice;
		}
		updateViews();
	}
	
	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
	 * AsyncTasks - apply effect / save image
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	class PerformEffect extends AsyncTask<Void, Void, Integer> {

		private int mNameResId;
		private boolean mSetOnImageView;

		public PerformEffect(int nameResId, boolean setOnImageView) {
			mNameResId = nameResId;
			mSetOnImageView = setOnImageView;
		}

		@Override
		protected Integer doInBackground(Void... params) {

			int errorResId = 0;

			mLastChoice = mNameResId;

			fetchOriginalImage();

			applyEffect(mNameResId, mCurrentBitmap);

			return errorResId;
		}

		@Override
		protected void onPreExecute() {

			mDialog = ProgressDialog.show(getActivity(), "", getString(R.string.snaprkit_applying), true);

			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);

			if(mSetOnImageView)
				mEditedImageView.setImageBitmap(mCurrentBitmap);

			if(null!=mDialog)
				mDialog.dismiss();

		}

	}

	class SaveEditedBitmapToFileAsyncTask extends AsyncTask<Void, Void, Integer> {

		private File tempFile;

		@Override
		protected Integer doInBackground(Void... params) {

			int errorResId = 0;

			if(null!=mSaveFilename)
				tempFile = new File(mSaveFilename);
			else
				tempFile = new File(mOriginalFilePath);

			try {

				// Make sure the base output directory exists
				File baseDir = tempFile.getParentFile();
				baseDir.mkdirs();

				// Make the bitmap
				Bitmap b = null;

				// Fetch a larger bitmap
				int width = SnaprPhotoHelper.getMaxImageWidth(getActivity());
				int scale = JSAImageUtil.getLoadImageScale(new File(mOriginalFilePath), width, width);
				Options opts = new Options();
				opts.inSampleSize = scale;
				opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
				b = JSAImageUtil.loadImageFile(new File(mOriginalFilePath), opts);
				b = SnaprPhotoHelper.cropBitmap(b);
				applyEffect(mLastChoice, b);

				FileOutputStream fos = new FileOutputStream(tempFile.getAbsolutePath());
				b.compress(CompressFormat.JPEG, 100, fos);

				// Tell the media scanner about the new file so that it is
				// immediately available to the user.
				MediaScannerConnection.scanFile(
						SnaprKitApplication.getInstance(),
						new String[] { tempFile.toString() }, null, null);
			} catch(FileNotFoundException e) {
				e.printStackTrace();
				errorResId = R.string.snaprkit_problem_saving_check_storage;
			}

			return errorResId;
		}

		@Override
		protected void onPreExecute() {

			mDialog = ProgressDialog.show(getActivity(), "", getString(R.string.snaprkit_saving_), true);

			mEditedImageView.setVisibility(View.GONE);
			mCurrentBitmap = null;
			System.gc();

			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);

			if(null!=mDialog)
				mDialog.dismiss();

			if(0==result) {
				mFragmentListener.onEditComplete(tempFile.getAbsolutePath());
			} else {
				Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show();
			}

		}
	}

	public void applyEffect(int mNameResId, Bitmap bitmapToAlter) {
		if(bitmapToAlter == null) return;
		
		if (mNameResId == R.string.snaprkit_pnk) PinkEffectPNK.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		else if (mNameResId == R.string.snaprkit_confetti) PinkEffectConfetti.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		else if (mNameResId == R.string.snaprkit_bonbon) PinkEffectBonBon.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		else if (mNameResId == R.string.snaprkit_sunrise) PinkEffectSunrise.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		else if (mNameResId == R.string.snaprkit_late_nights) PinkEffectLateNights.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		else if (mNameResId == R.string.snaprkit_dreamer) PinkEffectDreamer.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		else if (mNameResId == R.string.snaprkit_sweet_talk) PinkEffectSweetTalk.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		else if (mNameResId == R.string.snaprkit_gumdrop) PinkEffectGumDrop.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		else if (mNameResId == R.string.snaprkit_love_letter) PinkEffectLoveLetter.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		else if (mNameResId == R.string.snaprkit_summer) PinkEffectSummer.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		else if (mNameResId == R.string.snaprkit_retro) PinkEffectRetro.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		else if (mNameResId == R.string.snaprkit_bubble) PinkEffectBubble.applyEffects(bitmapToAlter, SnaprKitApplication.getInstance());
		
		/*
		 * ignore anything else: 
		 * 
		 * if (mNameResId == R.string.snaprkit_original) { }
		 * else { }
		 */ 
	}


	private void saveResizedCroppedPhoto() {
		File picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File tempFile = new File(picDir, CHOSEN_PHOTO_FILENAME);

		// Make sure the Pictures directory exists.
		picDir.mkdirs();

		FileOutputStream fos;
		try {
			fos = new FileOutputStream(tempFile.getAbsolutePath());
			mCurrentBitmap.compress(CompressFormat.JPEG, 100, fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	private void getResizedCroppedPhoto() {

		File picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		File tempFile = new File(picDir, CHOSEN_PHOTO_FILENAME);

		FileInputStream fis;
		try {
			fis = new FileInputStream(tempFile.getAbsolutePath());
			Options opts = new Options();
			opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

			mCurrentBitmap = BitmapFactory.decodeStream(fis,null, opts);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * fragment listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	public void setFragmentListener(FragmentListener listener) {
		mFragmentListener = listener;
	}


	public static interface FragmentListener {
		void onEditComplete(String filePath);
		void onCancel();
		void onAddAnalytic(String value);
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * swipe listener
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */
	@Override
	public void goNext() {
		mLastChoice = mEffects.get(getIndexOfNextEffect()).mNameResId;
		selectEffectView();

		new PerformEffect(mLastChoice, true).execute();
	}

	@Override
	public void goPrevious() {
		mLastChoice = mEffects.get(getIndexOfPreviousEffect()).mNameResId;
		selectEffectView();

		new PerformEffect(mLastChoice, true).execute();
	}

	private int getIndexOfNextEffect() {
		int ci = getIndexOfCurrentEffect();
		if(ci==mEffects.size()-1) return 0;
		return ci+1;
	}

	private int getIndexOfPreviousEffect() {
		int ci = getIndexOfCurrentEffect();
		if(ci==0) return mEffects.size()-1;
		return ci-1;
	}

	private int getIndexOfCurrentEffect() {
		for(SnaprEffect effect : mEffects) {
			if(effect.mNameResId == mLastChoice)
				return mEffects.indexOf(effect);
		}
		return 0;
	}

	/* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	 * JNI library loader
	 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - */

	static {
		System.loadLibrary("snapr-jni");
		//		Log.i(TAG,"loading snapr juice...");
	}

}
