package com.zhuofengli.blackwhitephotos;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.util.TypedValue;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

public class ImageUtils {
	public static final int RESULT_PHOTO_SIZE_CAP = 1024;
	public static final int RESULT_PHOTO_THUMB_SIZE_IN_DP = 100;
	public static final int RESULT_PHOTO_JPEG_QUALITY = 90;

	public static final String FOLDER_NAME_FOR_PHOTOS = "BlackWhitePhotos";
	public static final String FOLDER_NAME_FOR_CACHE = ".blackwhitephotos_cache";

	// this function prepares parameters and passes them to native code which resizes the image, applies
	// black&white effect, and saves to disk. It returns an error message if there's any.
	public static String processImage(String oriImgUriString, String oriImagePath, Context context, List<Uri> resultUris) {
		try {

			// 1. see if it's a http uri, if yes, download the file first
			Uri oriUri = Uri.parse(oriImgUriString);
			if (oriUri.getScheme().equals("http")) {
				oriImagePath = fetchFileFromWeb(oriImgUriString);
			}
			
			// 2. get src type
			int srcType = getSrcImageTypeNumber(context, oriUri);
			Log.i("Src Img Type = ", ""+srcType);
			if(srcType < 0){
				return context.getString(R.string.file_type_not_supported);
			}
			
			// 3. get src size
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(oriImagePath, options);

			int srcWidth = options.outWidth;
			int srcHeight = options.outHeight;
			
			// 4. get built in orientation
			int orientation = getRotationFromImage(oriImagePath);
			
			// 5.get destination file path, thumbnail file path, thumbnail size
			String fileName = newFileName(oriImagePath);
			String dstImagePath = getPathForNewFile(fileName);
			String thumbName = "thumb_" + fileName;
			String thumbImagePath = createThumbImagePath(thumbName);
			int thumbSizeInPx = dipToPixel(RESULT_PHOTO_THUMB_SIZE_IN_DP,
					context);

			// 6.decode the original image file, resize and apply effect in
			// native code
			NativeLib n = new NativeLib();
			int ret = n.processAndSaveFile(oriImagePath, srcType, srcWidth, srcHeight, orientation,
					RESULT_PHOTO_SIZE_CAP, dstImagePath, RESULT_PHOTO_JPEG_QUALITY, thumbSizeInPx,
					thumbImagePath);

			if (ret == 0) { // failed in NDK
				return context.getString(R.string.ndk_processing_failed);
			}

			// 7.add it in the android content system
			Uri resultImgUri = createUriForFile(context, fileName, dstImagePath);
			Uri thumbImgUri = createUriForFile(context, thumbName,
					thumbImagePath);

			resultUris.add(resultImgUri);
			resultUris.add(thumbImgUri);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private static String fetchFileFromWeb(String urlString)
			throws MalformedURLException, IOException, Exception {

		String filePath = null;

		URL u = new URL(urlString);
		URLConnection uc = u.openConnection();
		
		String contentType = uc.getContentType();
		int contentLength = uc.getContentLength();
		Log.i("Fetch from web", "contentType="+contentType+", contentLength="+contentLength);
		
		if ((contentType != null && contentType.startsWith("text/"))
				|| contentLength == -1) {
			throw new IOException("This is not a binary file.");
		}
		InputStream raw = uc.getInputStream();


		if (checkSDCard()) {
			try {
				filePath = writeToExternal(urlString, raw, contentLength);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return filePath;
	}

	private static boolean checkSDCard() {

		boolean hasSDCard = Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED);
		return hasSDCard;
	}

	private static String writeToExternal(String urlString, InputStream is,
			int len) throws Exception { // throws FileNotFoundException,
										// IOException

		String filePath = null;

		FileOutputStream stream = null;
		InputStream in = null;
		try {
			filePath = getExternalCacheFolder() + "/" + newFileName(urlString);

			stream = new FileOutputStream(filePath);
			in = new BufferedInputStream(is);
			byte[] data = new byte[len];
			int bytesRead = 0;
			int offset = 0;
			while (offset < len) {
				bytesRead = in.read(data, offset, data.length - offset);
				if (bytesRead == -1)
					break;
				offset += bytesRead;
			}
			if (offset != len) {
				throw new IOException("Only read " + offset
						+ " bytes; Expected " + len + " bytes");
			}

			stream.write(data);
			stream.flush();

		} finally {
			if (stream != null) {
				stream.close();
			}
			if (in != null) {
				in.close();
			}
			if (is != null) {
				is.close();
			}
		}

		return filePath;
	}


	private static String cacheFolderStr = null;
	// get the hidden cache folder for our app
	public static String getExternalCacheFolder() throws Exception {

		if (cacheFolderStr == null) {
			File extBaseDir = Environment.getExternalStorageDirectory();
			File file = new File(extBaseDir.getAbsoluteFile() + "/"
					+ FOLDER_NAME_FOR_CACHE);
			if (!file.exists()) {
				if (!file.mkdirs()) {
					throw new Exception("Could not create directories, "
							+ file.getAbsolutePath());
				}
			}

			cacheFolderStr = file.getAbsolutePath();
		}
		return cacheFolderStr;
	}

	@SuppressLint("NewApi")
	public static String getRealPathFromURI(Uri contentUri, Activity context) {
	
		try {
			Cursor cursor = null;
			String[] proj = { MediaColumns.DATA };

			int sdk_version = android.os.Build.VERSION.SDK_INT;
			if (sdk_version >= 11) {
				String selection = null;
				String[] selectionArgs = null;
				String sortOrder = null;

				// Requires SDK 11 to support below
				CursorLoader cursorLoader = new CursorLoader(
						// this need to happen in main thread in 4.0
						context, contentUri, proj, selection, selectionArgs,
						sortOrder);

				cursor = cursorLoader.loadInBackground();
			} else {
				cursor = context.managedQuery(contentUri, proj, null, null,
						null);
			}

			if (cursor == null) {
				Log.d("getRealPathFromURI", "cursor is null, URI path  : "
						+ contentUri.getPath());
				return contentUri.getPath();
			}

			int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
			cursor.moveToFirst();
			String ret = cursor.getString(column_index);

			cursor.close();
			return ret;

		} catch (Exception e) {
			e.printStackTrace();
			String path = contentUri.getPath();
			return path;
		}
	}

	// create a new name for the upcoming new file, created from oriPath
	private static String newFileName(String oriPath) {
		String oriFileName = "";
		if (oriPath != null)
			oriFileName = new File(oriPath).getName();
		
		int i = oriFileName.indexOf(".");
		if(i>=0){
			oriFileName = oriFileName.substring(0, i);
		}
		
		String newName = String.valueOf(System.currentTimeMillis()) + "_" + oriFileName
				+ ".jpg";
		
		return newName;
	}

	private static String getPathForNewFile(String fileName) throws Exception {
		File extBaseDir = Environment.getExternalStorageDirectory();
		File file = new File(extBaseDir.getAbsoluteFile() + "/"
				+ FOLDER_NAME_FOR_PHOTOS);
		if (!file.exists()) {
			if (!file.mkdirs()) {
				throw new Exception("Could not create directories, "
						+ file.getAbsolutePath());
			}
		}
		return file.getAbsolutePath() + "/" + fileName;
	}

	// get thumbnail file path for the new b&w photo
	private static String createThumbImagePath(String dstFileName)
			throws Exception {
		
		String filePath = getExternalCacheFolder() + "/" + dstFileName;
		return filePath;
	}

	private static int dipToPixel(int dip, Context context) {

		Resources r = context.getResources();
		int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				dip, r.getDisplayMetrics());

		return px;
	}

	public static int getRotationFromImage(String imagePath) {
		int ret = 0;
		
		try {
			ExifInterface exif = new ExifInterface(imagePath);
			int orientation = exif.getAttributeInt(
					ExifInterface.TAG_ORIENTATION, 0);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				ret = 1;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				ret = 2;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				ret = 3;
				break;
			default:
				ret = 0;
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Log.i("Photo orientation", "Code="+ret);
		
		return ret;
	}
	private static int getSrcImageTypeNumber(Context context, Uri imageUri){
		String mimeType = null;
		if (imageUri.getScheme().equals("http")) {
			mimeType = getMimeType(imageUri.toString());
		}
		else{
			mimeType = getMimeType(context, imageUri);
		}
		
		if(mimeType == null){
			return -1;
		}
		if(mimeType.contains("jpeg") || mimeType.contains("jpg") 
				|| mimeType.contains("JPEG") || mimeType.contains("JPG")){
			return 0;
		}
		else if(mimeType.contains("png") || mimeType.contains("PNG")){
			return 1;
		}
		
		return -1;
	}
	private static String getMimeType(Context context, Uri imageUri) {
		String strMimeType = null;

		Cursor cursor = context.getContentResolver().query(imageUri,
				new String[] { MediaStore.MediaColumns.MIME_TYPE }, null, null,
				null);

		if (cursor != null && cursor.moveToNext()) {
			strMimeType = cursor.getString(0);
		}
		
		Log.i("MIME TYPE = ", strMimeType);
		return strMimeType;
	}
	
	public static String getMimeType(String url)
	{
	    String type = null;
	    String extension = MimeTypeMap.getFileExtensionFromUrl(url);
	    if (extension != null) {
	        MimeTypeMap mime = MimeTypeMap.getSingleton();
	        type = mime.getMimeTypeFromExtension(extension);
	    }
	    return type;
	}
	

	public static Uri createUriForFile(Context con, String fileName,
			String filePath) throws Exception {
		long currentTime = System.currentTimeMillis();
		long size = new File(filePath).length();

		ContentValues values = new ContentValues(6);
		values.put(MediaColumns.TITLE, fileName);
		values.put(MediaColumns.DISPLAY_NAME, fileName);
		values.put(MediaColumns.DATE_ADDED, currentTime);
		values.put(MediaColumns.MIME_TYPE, "image/jpeg");
		values.put(ImageColumns.ORIENTATION, 0);
		values.put(MediaColumns.DATA, filePath);
		values.put(MediaColumns.SIZE, size);

		return con.getContentResolver().insert(
				Images.Media.EXTERNAL_CONTENT_URI, values);
	}

	// cache thumbnails in memory, to avoid read from sd card everytime when
	// showing up a photo row

	private static Map<String, Drawable> thumbDrawableMap = null;
	private final static int MAX_THUMB_IN_MAP = 50;

	public static void getThumbForPhotoRow(ImageView imageView, Uri uri,
			Context context) {
		imageView.setImageDrawable(null);

		if (thumbDrawableMap == null)
			thumbDrawableMap = new HashMap<String, Drawable>();

		String uriString = uri.toString();

		if (thumbDrawableMap.containsKey(uriString)) { // if we already have the
														// drawable in map
			Drawable dr = (Drawable) thumbDrawableMap.get(uriString);
			imageView.setImageDrawable(dr);

		} 
		else { // if not, read the drawable from uri and store it in map

			String thumbPath = getRealPathFromURI(uri, (Activity) context);
			Drawable dr = Drawable.createFromPath(thumbPath);

			if (thumbDrawableMap.size() >= MAX_THUMB_IN_MAP) {
				thumbDrawableMap.clear();
			}
			
			if(dr!=null)
				thumbDrawableMap.put(uriString, dr);

			imageView.setImageDrawable(dr);
		}
	}

}
