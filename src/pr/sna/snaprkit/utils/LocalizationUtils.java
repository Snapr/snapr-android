package pr.sna.snaprkit.utils;

import java.util.Locale;

public class LocalizationUtils
{
	public static String getLanguageCode()
	{
		return Locale.getDefault().getLanguage();
	}
	
	public static String getLocaleCode()
	{
		return Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry();
	}
}
