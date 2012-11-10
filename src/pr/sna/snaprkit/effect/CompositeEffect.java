package pr.sna.snaprkit.effect;

import android.graphics.Bitmap;

/**
 * Provides the  Effect
 * @author julius
 *
 */
public class CompositeEffect {

	//  - Effect
	public static native void applyImageEffect(Bitmap source, Bitmap overlay, float alpha, int blendMode);
	public static native void applyAlphaEffect(Bitmap source, Bitmap overlay, float alpha, int blendMode);
	public static native void applyColorEffect(Bitmap source, int colour, float alpha, int blendMode);

}
