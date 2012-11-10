package pr.sna.snaprkit.effect.pink;

import pr.sna.snaprkit.R;
import pr.sna.snaprkit.SnaprPhotoHelper;
import pr.sna.snaprkit.effect.BlendMode;
import pr.sna.snaprkit.effect.CompositeEffect;
import pr.sna.snaprkit.effect.HSBEffect;
import pr.sna.snaprkit.effect.LevelsEffect;
import android.content.Context;
import android.graphics.Bitmap;

public class PinkEffectConfetti {

	public static void applyEffects(Bitmap image, Context context) {

		HSBEffect.applyEffect(image, 0, 0, 1.0f);

		LevelsEffect.applyLevel(image, 0, 1, 255, 159, 255); // lightness +65
		LevelsEffect.applyLevel(image, 147, 1.0f, 235, 0, 255);

		CompositeEffect.applyColorEffect(image, 0xf3f3e8, 1.0f, BlendMode.BLEND_MODE_MULTIPLY);

		Bitmap temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_confetti_border);
		Bitmap bit0 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit0)
			CompositeEffect.applyImageEffect(image, bit0, 1.0f, BlendMode.BLEND_MODE_NORMAL);
		bit0.recycle();

		temp = SnaprPhotoHelper.getBitmapFromAssets(context, R.raw.snaprkit_pink_confetti);
		Bitmap bit1 = SnaprPhotoHelper.getResizedBitmap(temp, image.getHeight(), image.getHeight());
		temp.recycle();
		if(null!=bit1)
			CompositeEffect.applyImageEffect(image, bit1, 1.0f, BlendMode.BLEND_MODE_MULTIPLY);
		bit1.recycle();

	}

}
