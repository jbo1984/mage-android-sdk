package mil.nga.giat.mage.sdk.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class MediaUtility {
	
	private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }
	
	public static String getMimeType(String url)
	{
	    String type = null;
	    String extension = MimeTypeMap.getFileExtensionFromUrl(url.replaceAll("\\s*", ""));
	    if (extension != null) {
	        MimeTypeMap mime = MimeTypeMap.getSingleton();
	        type = mime.getMimeTypeFromExtension(extension);
	    }
	    return type;
	}
	
	public static Boolean isImage(String filePath) {
		String mime = getMimeType(filePath);
		if(mime == null) {
			return false;
		}
		return mime.toLowerCase().matches("image/.*");
	}
	
	public static void addImageToGallery(Context c, Uri contentUri) {
	    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	    mediaScanIntent.setData(contentUri);
	    c.sendBroadcast(mediaScanIntent);
	}
	
	public static File createImageFile() throws IOException {
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = "MAGE_" + timeStamp;
	    File storageDir = Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_PICTURES);
	    return File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */
	        storageDir      /* directory */
	    );
	}
	
	public static File getMediaStageDirectory() {
		File sd = Environment.getExternalStorageDirectory();
		File mediaFolder = new File(sd, "/MAGE/Media");
		if (sd.canWrite()) {
			if (!mediaFolder.exists()) {
				mediaFolder.mkdirs();
			}
		}
		return mediaFolder;
	}
	
	public static File getAvatarDirectory() {
		File sd = Environment.getExternalStorageDirectory();
		File mediaFolder = new File(sd, "/MAGE/Media/user/avatars");
		if (sd.canWrite()) {
			if (!mediaFolder.exists()) {
				mediaFolder.mkdirs();
			}
		}
		return mediaFolder;
	}
	
	public static File getUserIconDirectory() {
		File sd = Environment.getExternalStorageDirectory();
		File mediaFolder = new File(sd, "/MAGE/Media/user/icons");
		if (sd.canWrite()) {
			if (!mediaFolder.exists()) {
				mediaFolder.mkdirs();
			}
		}
		return mediaFolder;
	}
	
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     * @author paulburke
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     * @author paulburke
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     * @author paulburke
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author paulburke
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {

                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br>
     * <br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     * 
     * @param context The context.
     * @param uri The Uri to query.
     * @see #isLocal(String)
     * @see #getFile(Context, Uri)
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {


        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // LocalStorageProvider
//            if (isLocalStorageDocument(uri)) {
//                // The path is the id
//                return DocumentsContract.getDocumentId(uri);
//            }
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
	
	public static String getFileAbsolutePath(Uri uri, Context c) 
	{
	    String fileName = null;
	    String scheme = uri.getScheme();
	    if (scheme.equals("file")) {
	        fileName = uri.getPath();
	    }
	    else if (scheme.equals("content")) {
	    	Cursor cursor = null;
	    	  try { 
	    	    String[] proj = { MediaStore.Images.Media.DATA };
	    	    cursor = c.getContentResolver().query(uri,  proj, null, null, null);
	    	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	    	    cursor.moveToFirst();
	    	    return cursor.getString(column_index);
	    	  } finally {
	    	    if (cursor != null) {
	    	      cursor.close();
	    	    }
	    	  }
	    }
	    return fileName;
	}
	
	public static Bitmap getFullSizeOrientedBitmap(Uri uri, Context c) throws FileNotFoundException, IOException {
		InputStream is = c.getContentResolver().openInputStream(uri);
		return MediaUtility.orientBitmap(BitmapFactory.decodeStream(is, null, null), getFileAbsolutePath(uri, c));
	}
	
	public static Bitmap getThumbnailFromContent(Uri uri, int thumbsize, Context c) throws FileNotFoundException, IOException {
		InputStream is = c.getContentResolver().openInputStream(uri);
		return MediaUtility.getThumbnail(is, thumbsize, getFileAbsolutePath(uri, c));
	}
	
	public static Bitmap getThumbnail(File file, int thumbsize) throws FileNotFoundException, IOException {
		FileInputStream input = new FileInputStream(file);
		return MediaUtility.getThumbnail(input, thumbsize, file.getAbsolutePath());
    }
	
	// TODO: this will only allow thumbnails based on the max of width or height.  We should allow choosing either height or width.
	// Be aware that this method also rotates the image so height/width potentially could change and I think we should probably
	// not rotate until it is resized to save memory
	public static Bitmap getThumbnail(InputStream input, int thumbsize, String absoluteFilePath) throws FileNotFoundException, IOException {
		BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > thumbsize) ? (originalSize / thumbsize) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither=true;//optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        input = new FileInputStream(absoluteFilePath);
        
        Bitmap bitmap = MediaUtility.orientBitmap(BitmapFactory.decodeStream(input, null, bitmapOptions), absoluteFilePath);
        input.close();
        return bitmap;
	}
	
	public static Bitmap getThumbnail(InputStream input, int thumbsize) throws IOException {
		BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
            return null;

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > thumbsize) ? (originalSize / thumbsize) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither=true;//optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        return BitmapFactory.decodeStream(input, null, bitmapOptions);
	}
	
	public static Bitmap orientBitmap(Bitmap bitmap, String absoluteFilePath) throws IOException {
		// Rotate the picture based on the exif data
        ExifInterface exif = new ExifInterface(absoluteFilePath);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        Matrix matrix = new Matrix();
        if (orientation == 6) {
            matrix.postRotate(90);
        }
        else if (orientation == 3) {
            matrix.postRotate(180);
        }
        else if (orientation == 8) {
            matrix.postRotate(270);
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true); // rotating bitmap
	}
	
	public static Bitmap orientImage(File original) {
		try {
			Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(original), null, null);
			return orientBitmap(bitmap, original.getAbsolutePath());
		} catch (Exception e) {
			Log.e("MediaUtils", "Error loading bitmap from " + original.getAbsolutePath());
		}
		return null;
	
	}

}
