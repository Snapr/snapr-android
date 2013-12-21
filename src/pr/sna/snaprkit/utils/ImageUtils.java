package pr.sna.snaprkit.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TagInfo;

import pr.sna.snaprkit.Global;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.media.ExifInterface;

public class ImageUtils
{
	// Constants
	private static final String EXIF_DATE_FORMAT_1 = "yyyy:MM:dd HH:mm:ss";
	private static final String EXIF_DATE_FORMAT_2 = "yyyy/MM/dd HH:mm:ss";
	private static final int INSTAGRAM_SIZE = 612;
	
	// ------------------------------------------------------------------------
	// Picture store functions
	// ------------------------------------------------------------------------
		
	// Gets the last image id from the file system
	// Used on low end devices that fail to insert the camera picture 
	// in the MediaStore
	public static String getLastImagePath()
	{
		// Declare
		String imageFolder = FileUtils.getDCIMCameraDirectory();
		
		// Loop through files and find the last modified file
		return getLastModifiedFile(imageFolder);
	}
	
	public static String getLastModifiedFile(String directory)
	{
		File dir = new File(directory);

		File[] files = dir.listFiles();
		if (files.length == 0)
		{
		    return null;
		}

		File lastModifiedFile = files[0];
		for (int i = 1; i < files.length; i++)
		{
		   if (lastModifiedFile.lastModified() < files[i].lastModified())
		   {
		       lastModifiedFile = files[i];
		   }
		}
		
		// Return
		return lastModifiedFile.getAbsolutePath();
	}
	
	// ------------------------------------------------------------------------
	// Thumbnail functions
	// ------------------------------------------------------------------------
	
	public static String getThumbFileName(String fileName)
	{
		File file = new File(fileName);
		return file.getAbsolutePath() + "/thumb_" + file.getName();
	}
	
	// ------------------------------------------------------------------------
	// EXIF Date Parsing for Sanselan
	// ------------------------------------------------------------------------
	
	public static Date parseExifDate(JpegImageMetadata jpegMetadata, TagInfo fieldId, String fieldFormat)
    {
    	TiffField field;
    	String fieldString;
    	Date returnDateTime = null;
    	
		// Get modify date time
		try
		{
	    	field = jpegMetadata.findEXIFValue(fieldId);
			fieldString = field.getStringValue(); // EXIF stores dates as string with no timezone, so assume current timezone
			SimpleDateFormat sdf = new SimpleDateFormat(fieldFormat);
			returnDateTime = sdf.parse(fieldString);
		}
		catch (Exception e)
		{
			// Log
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " Failed to parse date with taginfo " + fieldId + " and format " + fieldFormat);
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " Got error " + e);
		}
		
		return returnDateTime;
    }
    
    public static Date parseExifDateMultiFormat(JpegImageMetadata jpegMetadata, TagInfo fieldId)
    {
    	// Declare
    	Date returnDate = null;
    	
    	// Get date time - use regular format
    	returnDate = parseExifDate(jpegMetadata, fieldId, EXIF_DATE_FORMAT_1);
    	
    	// Get date time - use HTC Sensation erroneous format
    	if (returnDate == null)
    	{
    		returnDate = parseExifDate(jpegMetadata, fieldId, EXIF_DATE_FORMAT_2);
    	}
    	
    	// Return
    	return returnDate;
    }
	
	// ------------------------------------------------------------------------
	// EXIF Date Parsing for ExifInterface
	// ------------------------------------------------------------------------
	
	public static Date parseExifDate(ExifInterface exif, String fieldId, String fieldFormat)
    {
		String dateTimeString = null;
    	Date returnDateTime = null;
    	
		// Get modify date time    	
		try
		{
			dateTimeString = exif.getAttribute(fieldId);
			SimpleDateFormat sdf = new SimpleDateFormat(fieldFormat);
			returnDateTime = sdf.parse(dateTimeString);
		}
		catch (Exception e)
		{
			// Log
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " Failed to parse date '" + dateTimeString + "' with taginfo '" + fieldId + "' and format '" + fieldFormat+"'");
			if (Global.LOG_MODE) Global.log(" -> " + Global.getCurrentMethod() + " Got error " + e);
		}
		
		return returnDateTime;
    }
    
    public static Date parseExifDateMultiFormat(ExifInterface exif, String fieldId)
    {
    	// Declare
    	Date returnDate = null;
    	
    	// Get date time - use regular format
    	returnDate = parseExifDate(exif, fieldId, EXIF_DATE_FORMAT_1);
    	
    	// Get date time - use HTC Sensation erroneous format
    	if (returnDate == null)
    	{
    		returnDate = parseExifDate(exif, fieldId, EXIF_DATE_FORMAT_2);
    	}
    	
    	// Return
    	return returnDate;
    }
    
    /**
     * Takes a photo and and modifies it to be compatible with instagrams limitations
     *  (namely that it must be at least 612x612px)
     * @param original the complete url to the original photo (must be of the format "file://<file info>.jpg")
     * @return the path to the new photo created, or (param)original if no changes were needed, or null if there was an issue		
     */
	public static String getInstagramCompatiblePhoto(String original) {
		try {
			Bitmap orig = BitmapFactory.decodeFile(original.replace("file://", ""));
			if (orig.getWidth() > INSTAGRAM_SIZE && orig.getHeight() > INSTAGRAM_SIZE) {
				// If we meet the sizing requirements, return the original
				orig.recycle();
				return original;
			} else {
				// Otherwise we need to scale the photo up
				int width, height;
				if (orig.getWidth() < orig.getHeight()) {
					// Need to scale width
					width = INSTAGRAM_SIZE;
					height = orig.getHeight() * width / orig.getWidth();
				} else {
					// Need to scale height
					height = INSTAGRAM_SIZE;
					width = orig.getWidth() * height / orig.getHeight();
				}
				
				//Create the new image
				Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(result);
				canvas.drawBitmap(orig, null, new RectF(0, 0, width, height), null);
				
				//Write it to a new file
				String newFile = original.replace(".jpg", "") + "_instagram.png";
				File file = new File(newFile.replace("file://", ""));
				file.createNewFile();
				FileOutputStream out = new FileOutputStream(file);
				result.compress(Bitmap.CompressFormat.PNG, 90, out);
				
				result.recycle();
				orig.recycle();
				
				out.flush();
				out.close();
				
				return newFile;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
    }
}