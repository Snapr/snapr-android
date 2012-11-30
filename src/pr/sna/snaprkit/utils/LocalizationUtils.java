package pr.sna.snaprkit.utils;

import java.util.Locale;

public class LocalizationUtils
{
	/**
	 * Puts together an HTML style language code in the format xx-YY where
	 * xx is an ISO 639 language code and YY is an ISO 3166 country code
	 * @return
	 */
	public static String getLanguageCode()
	{
		return Locale.getDefault().getLanguage() + "-" + Locale.getDefault().getCountry();
	}
}
