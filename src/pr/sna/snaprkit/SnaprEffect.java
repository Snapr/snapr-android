package pr.sna.snaprkit;

import pr.sna.snaprkit.SnaprImageEditFragmentActivity.EffectConfig;

public class SnaprEffect {
	
	/** constant for no effect. */
	public static final int EFFECT_ORIGINAL_ID 		= 0;
	/** constant for Luv effect. */
	public static final int EFFECT_LUV_ID 			= 1;
	/** constant for Confetti effect. */
	public static final int EFFECT_CONFETTI_ID 		= 2;
	/** constant for Bon Bon effect. */
	public static final int EFFECT_BONBON_ID 		= 3;
	/** constant for Sunrise effect. */
	public static final int EFFECT_SUNRISE_ID		= 4;
	/** constant for Late Nights effect. */
	public static final int EFFECT_LATE_NIGHTS_ID	= 5;
	/** constant for Dreamer effect. */
	public static final int EFFECT_DREAMER_ID 		= 6;
	/** constant for Sweet Talk effect. */
	public static final int EFFECT_SWEET_TALK_ID	= 7;
	/** constant for Gumdrop effect. */
	public static final int EFFECT_GUMDROP_ID 		= 8;
	/** constant for Love Letter effect. */
	public static final int EFFECT_LOVE_LETTER_ID	= 9;
	/** constant for Summer effect. */
	public static final int EFFECT_SUMMER_ID 		= 10;
	/** constant for Retro effect. */
	public static final int EFFECT_RETRO_ID 		= 11;
	/** constant for Bubble effect. */
	public static final int EFFECT_BUBBLE_ID 		= 12;

	final int mEffectId;
	int mImageResId;
	int mNameResId;
	boolean mChosen;
	boolean mLocked;
	String mUnlockMessage;

	public SnaprEffect(int effectId, int imageResId, int nameResId, boolean chosen) {
		this(effectId, imageResId, nameResId, chosen, false);
	}

	public SnaprEffect(int effectId, int imageResId, int nameResId, boolean chosen, boolean locked) {
		this(effectId, imageResId, nameResId, chosen, locked, null);
	}

	public SnaprEffect(int effectId, int imageResId, int nameResId, boolean chosen, EffectConfig config) {
		this(effectId, imageResId, nameResId, chosen, config.mIsLocked, config.mUnlockMessage);
	}
	
	public SnaprEffect(int effectId, int imageResId, int nameResId, boolean chosen, boolean locked, String unlockMessage) {
		if (locked && unlockMessage == null) throw new IllegalArgumentException("A locked effect should provide an unlock message!");
		mEffectId = effectId;
		mImageResId = imageResId;
		mNameResId = nameResId;
		mChosen = chosen;
		mLocked = locked;
		mUnlockMessage = unlockMessage;
	}

}
