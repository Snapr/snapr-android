package pr.sna.snaprkit.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.SanselanFixes;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;
import org.apache.sanselan.formats.tiff.constants.GPSTagConstants;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

import pr.sna.snaprkit.ExifData;
import pr.sna.snaprkit.Global;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;

/**
 * @author Theo
 *
 */
public class CameraUtils
{
    public static String getPictureName()
    {
    	return Global.IMAGE_NAME_PREFIX + android.text.format.DateFormat.format(Global.IMAGE_NAME_DATE_FORMAT, new java.util.Date()).toString() + ".jpg";
    }
	
    public static String getDisabledProvidersString(boolean networkEnabled, boolean wifiEnabled, boolean gpsEnabled)
    {	
    	Vector<String> disabledProviders = new Vector<String>();
    	
    	if (!networkEnabled)
    	{
    		disabledProviders.add("network");
    	}
    	
    	if (!wifiEnabled)
    	{
    		disabledProviders.add("WiFi");
    	}
    	
    	if (!gpsEnabled)
    	{
    		disabledProviders.add("GPS");
    	}
    	
    	// Create string
    	if (disabledProviders.size() == 0)
    	{
    		return null;
    	}
    	else if (disabledProviders.size() == 1)
    	{
    		return disabledProviders.firstElement();
    	}
    	else
    	{
        	String returnString = "";
        	
        	// Concatenate all but the last element using ", "
        	for (int i=0; i<disabledProviders.size() - 1; i++)
        	{
        		returnString = returnString + disabledProviders.get(i) + ", ";
        	}
        	
        	// Remove last ", "
        	returnString = returnString.substring(0, returnString.length() -2);
        	
        	// Concatenate last element using " and "
        	returnString = returnString + " and " + disabledProviders.lastElement();
        	
        	// Return
        	return returnString;
    	}
    }
    
    public static ExifData getExifData(String fileName)
    {
    	// Due to various bugs in both the native and Sanselan EXIF
    	// libraries, we need to read using both libraries to make
    	// sure we always get the location information stored
    	// in the picture
    	
    	// Declare
    	ExifData exifDataAndroid;
    	ExifData exifDataSanselan;
    	
    	// Try to read the data using the Sanselan library first
    	exifDataSanselan = getExifDataSanselan(fileName);
    	
    	// Check results
    	if (exifDataSanselan == null || exifDataSanselan.getLocation() == null)
    	{
        	exifDataAndroid = getExifDataAndroid(fileName);
    		if (exifDataAndroid == null || exifDataAndroid.getLocation() == null)
    		{
    			// Prefer Sanselan data with multiple timestamps
    			return exifDataSanselan;
    		}
    		else
    		{
    			// Prefer Android data with location info
    			return exifDataAndroid;
    		}
    	}
    	else
    	{
    		return exifDataSanselan;
    	}
    }
        
    // 
    private static ExifData getExifDataSanselan(String fileName)
    {
    	// Declare
    	Location exifLocation = null;
    	Date originalDateTime = null;
    	Date modifyDateTime = null;
    	ExifData returnData = new ExifData();
    	
		IImageMetadata metadata = null;
		File file = null;
    	
    	// Create a file object
    	file = new File(fileName);
		
		try
		{
			metadata = Sanselan.getMetadata(file);
        }
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with error " + e);
		}
 
        if (metadata instanceof JpegImageMetadata)
        {
        	JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
        	
        	// Parse EXIF modify date
        	modifyDateTime = ImageUtils.parseExifDateMultiFormat(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME);
        	
        	// Save modify date
        	if (modifyDateTime != null)
        	{
        		returnData.setModifyDateTime(modifyDateTime);
        	}
        	
        	// Parse EXIF original date
        	originalDateTime = ImageUtils.parseExifDateMultiFormat(jpegMetadata, TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        	
        	// Save original date
        	if (originalDateTime != null)
        	{
        		returnData.setOriginalDateTime(originalDateTime);
        	}
        	
        	// Simple interface to GPS data
        	TiffImageMetadata exifMetadata = jpegMetadata.getExif();
        	
        	if (exifMetadata != null)
        	{
        		try 
        		{        			
        			// Get the GPS data
        			TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
        			if (null != gpsInfo)
        			{
        				// Get the longitude and latitude
        				double longitude = gpsInfo.getLongitudeAsDegreesEast();
        				double latitude = gpsInfo.getLatitudeAsDegreesNorth();
        				
        				// Enter them in the return object
        				exifLocation = new Location("exif");
        				exifLocation.setLatitude(latitude);
        				exifLocation.setLongitude(longitude);
        				exifLocation.setAltitude(0);
        				
        				// Add it to the return data
        				returnData.setLocation(exifLocation);
        			}
        		}
        		catch (ImageReadException e)
        		{
        			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Could not read GPS data: " +  e.toString());
        		}
        	}
        }
        
        return returnData;
    }
    
    private static ExifData getExifDataAndroid(String fileName)
    {
    	// Declare
    	Location exifLocation = null;
    	Date originalDateTime = null;
    	ExifData returnData = new ExifData();
    	
    	try
		{
    		// Get interface
			ExifInterface exif = new ExifInterface(fileName);
			
			// Get latitude and longitude
			float[] latLong = new float[2];
			if (exif.getLatLong(latLong))
			{
				// Save latitude and longitude to Location
				exifLocation = new Location("exif");
				exifLocation.setLatitude(latLong[0]);
				exifLocation.setLongitude(latLong[1]);
				exifLocation.setAltitude(0);
			}
			
			// Set location
			returnData.setLocation(exifLocation);
			
			// Get the date and time
			originalDateTime = ImageUtils.parseExifDateMultiFormat(exif, ExifInterface.TAG_DATETIME);
			
			// Set the date and time
			if (originalDateTime != null)
			{
				returnData.setOriginalDateTime(originalDateTime);
			}
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + "Failed with error " + e);
		}
    	
    	// Return
    	return returnData;
    }
    
    /**
	 * Tag the picture with the GPS information
	 * @param fileName Picture filename
	 * @param location GPS location
	 * @return Returns a boolean indicating whether it succeeded
	 * Based on Sanselan usage example; uses openstreetmap Sanselan fixes
	 * 
	 * Always use Sanselan to write EXIF info, since the ExifInterface corrupts
	 * the data on many versions of Android
	 */
	@SuppressLint("UseValueOf")
	public static boolean geotagPicture(String fileName, Location location)
    {
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
		
		// Exit immediately on incorrect parameters
		if (fileName == null || location == null)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
			return false;
		}
		
		try
		{
			File inFile = null;
			File tempFile = null;
			OutputStream tempStream = null;
			
	        try
	        {
	        	// Open the input file
	        	inFile = new File(fileName);
	        	
	        	// Get the output set
	            TiffOutputSet outputSet = getSanselanOutputSet(inFile);
	            
	            // Format date
	            long timestamp = location.getTime();
	            Date time = new Date(timestamp);
	            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy:MM:dd");
				sdfDate.setTimeZone(TimeZone.getTimeZone("UTC"));
				String dateStampString = sdfDate.format(time);
	
				// Get the time hours minutes and seconds
	            final int hour =   time.getHours();
	            final int minute = time.getMinutes();
	            final int second = time.getSeconds();
	
	            /*
	            // Get the altitude and altitude reference
	            double altitude = location.getAltitude();
	            Byte altitudeRef = (byte)((altitude >= 0)? 0:1);
	            
	            // Get provider
	            String provider = location.getProvider();
	            
	            // Get accuracy
	            double accuracy = location.getAccuracy();
	            */
	            
	            // Set the latitude and longitude
				double latitude = location.getLatitude();
				double longitude = location.getLongitude();
	            
	            // Open the GPS directory
	            TiffOutputDirectory exifDirectory = outputSet.getOrCreateGPSDirectory();
	            
	            // Add time field as array of doubles
	            Double[] timeStamp = {new Double(hour), new Double(minute), new Double(second)};
	            TiffOutputField gpsTimeStamp = TiffOutputField.create(
	                    GPSTagConstants.GPS_TAG_GPS_TIME_STAMP,
	                    outputSet.byteOrder, timeStamp);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_TIME_STAMP);
	            exifDirectory.add(gpsTimeStamp);
	
	            // Add date field as string
	            TiffOutputField gpsDateStamp = SanselanFixes.create(
	                    GPSTagConstants.GPS_TAG_GPS_DATE_STAMP,
	                    outputSet.byteOrder, dateStampString);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_DATE_STAMP);
	            exifDirectory.add(gpsDateStamp);
	
	            /*
	            // Add altitude
	            TiffOutputField gpsAltitude = TiffOutputField.create(
	                    GPSTagConstants.GPS_TAG_GPS_ALTITUDE,
	                   outputSet.byteOrder, altitude);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_ALTITUDE);
	            exifDirectory.add(gpsAltitude);
	            
	            // Add altitude ref as byte
	            TiffOutputField gpsAltitudeRef = TiffOutputField.create(
	                    GPSTagConstants.GPS_TAG_GPS_ALTITUDE_REF,
	                   outputSet.byteOrder, altitudeRef);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_ALTITUDE_REF);
	            exifDirectory.add(gpsAltitudeRef);
	            
	            // Add provider as string
	            TiffOutputField gpsProcessingMethod = SanselanFixes.create(
	                    GPSTagConstants.GPS_TAG_GPS_PROCESSING_METHOD,
	                    outputSet.byteOrder, provider);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_PROCESSING_METHOD);
	            exifDirectory.add(gpsProcessingMethod);
	            
	            // Add accuracy
	            TiffOutputField gpsAccuracy = TiffOutputField.create(
	                    GPSTagConstants.GPS_TAG_GPS_DOP,
	                   outputSet.byteOrder, accuracy);
	            exifDirectory.removeField(GPSTagConstants.GPS_TAG_GPS_DOP);
	            exifDirectory.add(gpsAccuracy);
	            */
	            
	            // Add GPS latitude and longitude
	            SanselanFixes.setGPSInDegrees(outputSet, longitude, latitude);
	
	            // Create output temporary file
	            tempFile = new File(fileName + ".tmp");
	            tempStream = new FileOutputStream(tempFile);
	            tempStream = new BufferedOutputStream(tempStream);
	
	            // Update metadata
	            new ExifRewriter().updateExifMetadataLossy(inFile, tempStream, outputSet);
	
	            // Rename temporary output to source filename
	            if (inFile.delete())
	            {
	            	tempFile.renameTo(inFile);
	            }
	            
	            // Close
	            tempStream.close();
	            tempStream = null;
	            
	            // Return
	            if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
	            return true;
	        }
	        finally
	        {
	            if (tempStream != null)
	            {
	                try
	                {
	                    tempStream.close();
	                }
	                catch (IOException e)
	                {
	                }
	            }
	            
	            if (tempFile != null)
	            {
	            	if (tempFile.exists()) tempFile.delete();
	            }
	        }
		}
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.TAG, Global.getCurrentMethod() + ": Failed due to error " + e.toString() + "\n" + ExceptionUtils.getExceptionStackString(e));
		}
		
		if (Global.LOG_MODE) Global.log(Global.TAG, " <- " + Global.getCurrentMethod());
		
		return false;
    }
	
	/**
	 * On Android 4.0, the camera uses EXIF picture orientation to indicate by what degree
	 * the picture should be rotated to be properly displayed on screen, but the Android 
	 * WebView ignores this value, resulting in incorrect picture display (mostly for
	 * portrait orientation pictures). We fix this by unpacking the image, rotating it
	 * and then setting the orientation angle to 0
	 * 
	 * @param imagePath Path to the image
	 */
	public static boolean fixImageOrientation(String imagePath)
	{
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod());
		
		// Declare
		boolean success = false;
		
		// Get the orientation angle
		int angle = ExifOrientationToAngle(getExifOrientation(imagePath));
		
		if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Got rotation angle " + angle);

		// When angle is not 0, rotate image
		if (angle != 0)
		{
			String inPath = imagePath;
			String tempPath = imagePath + ".tmp";
			
			File inFile = new File(inPath);
			File tempFile = null;
			OutputStream tempStream = null;
			
			boolean continueProcessing = true;
			Options options = new Options();
			options.inSampleSize = getInitialScalingFactor(inFile, 1600);
			
			while (continueProcessing == true)
			{
				try
				{
					Matrix mat = new Matrix();
					mat.postRotate(angle);
			
					if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Decoding and rotating image using sample size " + options.inSampleSize + "...");
					
					Bitmap bmp = BitmapFactory.decodeStream(new FileInputStream(inFile), null, options);
					Bitmap correctBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);
					
					if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Compressing new image...");
					
					tempStream = new FileOutputStream(tempPath);
					correctBmp.compress(CompressFormat.JPEG, 100, tempStream);
					tempStream.close();
					
					// Recycle the bitmaps to save memory
					bmp.recycle();
					bmp = null;
					correctBmp.recycle();
					correctBmp = null;
					System.gc();
					
					// Copy the EXIF data
					tempFile = new File(tempPath);
					List<TagInfo> excludedFields = new ArrayList<TagInfo>();
					excludedFields.add(ExifTagConstants.EXIF_TAG_ORIENTATION);
					copyExifData(inFile, tempFile, true, excludedFields);
	
					// Rename temporary output to source filename
					if (Global.LOG_MODE) Global.log(Global.TAG, " -> " + Global.getCurrentMethod() + ": Renaming image...");
		            if (inFile.delete())
		            {
		            	tempFile.renameTo(inFile);
		            }
		            
		            // If we get here, then we have succeeded, so mark flags appropriately
		            success = true;
		            continueProcessing = false;
				}
				catch (IOException e)
				{
					// Do not continue for this type of error
					continueProcessing = false;
					Global.log("Error in setting image: " + ExceptionUtils.getExceptionStackString(e));
				}
				catch(OutOfMemoryError oom)
				{
					// For out of memory errors we keep on going using a larger sample size to reduce overall image size
					continueProcessing = true;
					options.inSampleSize++;
					if (options.inSampleSize == 32)
					{
						// Quit because this is probably not going to work out
						continueProcessing = false;
					}
				}
				finally
				{
					if (tempStream != null)
					{
						try
						{
							tempStream.close();
						}
						catch (IOException e)
						{
						}
					}
					
					if (tempFile != null)
					{
						if (tempFile.exists()) tempFile.delete();
					}
				}
			}
		}
		
		return success;
	}
	
	private static int getExifOrientation(String imagePath)
	{
		int orientation = getExifOrientationSanselan(imagePath); 
		
		if (orientation == ExifInterface.ORIENTATION_UNDEFINED)
		{
			orientation = getExifOrientationAndroid(imagePath);
		}
		
		return orientation;
	}
	
	private static int getExifOrientationAndroid(String imagePath)
	{
		ExifInterface exif;
		
		int orientation = 0;
		
		try
		{
			exif = new ExifInterface(imagePath);
			
			orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		return orientation;
	}
	
	private static int getExifOrientationSanselan(String fileName)
    {
    	// Declare
    	int orientation = 1;
    	
		IImageMetadata metadata = null;
		File file = null;
    	
    	// Create a file object
    	file = new File(fileName);
		
		try
		{
			metadata = Sanselan.getMetadata(file);
        }
		catch (Exception e)
		{
			if (Global.LOG_MODE) Global.log(Global.getCurrentMethod() + ": Failed with error " + e);
		}
 
        if (metadata instanceof JpegImageMetadata)
        {
        	JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
        	
        	TiffField tf = jpegMetadata.findEXIFValue(TiffConstants.EXIF_TAG_ORIENTATION);
			
        	try
        	{
				Integer integer = (Integer)tf.getValue();
				orientation = integer.intValue();
			}
        	catch (ImageReadException e)
			{
				e.printStackTrace();
			}
        }
        
        return orientation;
    }
	
	public static int ExifOrientationToAngle(int exifOrientation)
	{
		int angle = 0;
		
		if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90)
		{
			angle = 90;
		}
		else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180)
		{
			angle = 180;
		}
		else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270)
		{
			angle = 270;
		}
		
		return angle;
	}
	
	public static int AngleToExifOrientation(int angle)
	{
		int exifOrientation = ExifInterface.ORIENTATION_NORMAL;
		
		if (angle == 90)
		{
			exifOrientation = ExifInterface.ORIENTATION_ROTATE_90;
		}
		else if (angle == 180)
		{
			exifOrientation = ExifInterface.ORIENTATION_ROTATE_180;
		}
		else if (angle == 270)
		{
			exifOrientation = ExifInterface.ORIENTATION_ROTATE_270;
		}
		
		return exifOrientation;
		
	}
	
	private static TiffOutputSet getSanselanOutputSet(File jpegImageFile) 
			throws IOException, ImageReadException, ImageWriteException
	{
		TiffOutputSet outputSet = null;
		
		// note that metadata might be null if no metadata is found.
		IImageMetadata metadata = Sanselan.getMetadata(jpegImageFile);
		JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
		if (null != jpegMetadata)
		{
			// note that exif might be null if no Exif metadata is found.
			TiffImageMetadata exif = jpegMetadata.getExif();

			if (null != exif)
			{
				// TiffImageMetadata class is immutable (read-only).
				// TiffOutputSet class represents the Exif data to write.
				//
				// Usually, we want to update existing Exif metadata by
				// changing
				// the values of a few fields, or adding a field.
				// In these cases, it is easiest to use getOutputSet() to
				// start with a "copy" of the fields read from the image.
				outputSet = exif.getOutputSet();
			}
		}

		// if file does not contain any exif metadata, we create an empty
		// set of exif metadata. Otherwise, we keep all of the other
		// existing tags.
		if (null == outputSet)
			outputSet = new TiffOutputSet();
		
		// Return
		return outputSet;
	}
	
	/**
	 * Copies the EXIF data from the source file to the destination file using Sanselan
	 * 
	 * Copying the EXIF data directly from source to destination does not work in Sanselan -- 
	 * Sanselan copies the entire source image data, not just EXIF data, and erases any destination
	 * image changes (such as image rotation between source and destination). To get around that, 
	 * we read the source EXIF OutputSet and the dest file EXIF OutputSet. We loop through all EXIF 
	 * directories and fields in the source OutputSet, manually copying the data to the destination
	 * OutputSet. Then, we apply the destination OutputSet onto a new temp file. The temp file will 
	 * now have the correct image and the correct EXIF data as well. The final setp is to replace the 
	 * dest file with the temp file.
	 * 
	 * @param sourceFile - Source image file
	 * @param destFile   - Destination image file
	 * @param preserveExistingFields - Preserve fields which already exist in the destination
	 * @param excludedFields - List of fields to avoid copying from source to destination
	 */
	private static void copyExifData(File sourceFile, File destFile, boolean preserveExistingFields, List<TagInfo> excludedFields)
	{
		String tempFileName = destFile.getAbsolutePath() + ".tmp";
		File tempFile = null;
		OutputStream tempStream = null;
		
		try
		{
			tempFile = new File (tempFileName);
			
			TiffOutputSet sourceSet = getSanselanOutputSet(sourceFile);
			TiffOutputSet destSet = getSanselanOutputSet(destFile);
			
			destSet.getOrCreateExifDirectory();
			
			// Go through the source directories
			List<?> sourceDirectories = sourceSet.getDirectories(); 
			for (int i=0; i<sourceDirectories.size(); i++)
			{
				TiffOutputDirectory sourceDirectory = (TiffOutputDirectory)sourceDirectories.get(i);
				TiffOutputDirectory destinationDirectory = getOrCreateExifDirectory(destSet, sourceDirectory);
				
				if (destinationDirectory == null) continue; // failed to create
				
				// Loop the fields
				List<?> sourceFields = sourceDirectory.getFields();
				for (int j=0; j<sourceFields.size(); j++)
				{
					// Get the source field
					TiffOutputField sourceField = (TiffOutputField) sourceFields.get(j);
					
					// Check exclusion list
					if (excludedFields.contains(sourceField.tagInfo))
					{
						destinationDirectory.removeField(sourceField.tagInfo);
						continue;
					}
					
					// Check field preservation
					if (preserveExistingFields && (destinationDirectory.findField(sourceField.tagInfo) != null))
					{
						continue;
					}
					
					// Remove any existing field
					destinationDirectory.removeField(sourceField.tagInfo);
					
					// Add field 
					destinationDirectory.add(sourceField);
				}
			}
			
			// Save data to destination
			tempStream = new BufferedOutputStream(new FileOutputStream(tempFile));
			new ExifRewriter().updateExifMetadataLossy(destFile, tempStream, destSet);
			tempStream.close();
			
			// Replace file
			if (destFile.delete())
			{
				tempFile.renameTo(destFile);
			}
		}
		catch (ImageReadException exception)
		{
			exception.printStackTrace();
		}
		catch (ImageWriteException exception)
		{
			exception.printStackTrace();
		}
		catch (IOException exception)
		{
			exception.printStackTrace();
		}
		finally
		{
			if (tempStream != null)
			{
				try
				{
					tempStream.close();
				}
				catch (IOException e)
				{
				}
			}
			
			if (tempFile != null)
			{
				if (tempFile.exists()) tempFile.delete();
			}
		}
	}
	
	private static TiffOutputDirectory getOrCreateExifDirectory(TiffOutputSet outputSet, TiffOutputDirectory outputDirectory)
	{
		TiffOutputDirectory result = outputSet.findDirectory(outputDirectory.type);
		if (null != result)
			return result;
		result = new TiffOutputDirectory(outputDirectory.type);
		try {
			outputSet.addDirectory(result);
		}
		catch (ImageWriteException e)
		{
			return null;
		}
		return result;
	}
	
	public static void scanMedia(Context context, String path)
	{
	    File file = new File(path);
	    Uri uri = Uri.fromFile(file);
	    Intent scanFileIntent = new Intent(
	            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
	    context.sendBroadcast(scanFileIntent);
	}
	
	private static int getInitialScalingFactor(File bitmapFile, int maximumEdge)
	{
		int scalingFactor = 1;
		
		FileInputStream bitmapInputStream;
		try
		{
			bitmapInputStream = new FileInputStream(bitmapFile);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return scalingFactor;
		}
		
		Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(bitmapInputStream, null, options);
		
		int height = options.outHeight;
		int width = options.outWidth;
		
		if (height > width && height > maximumEdge)
		{
			scalingFactor = (int) Math.ceil(height / maximumEdge);
		}
		else if (height < width && width > maximumEdge)
		{
			scalingFactor = (int) Math.ceil(width / maximumEdge);
		}
			
		return scalingFactor;
	}
}
