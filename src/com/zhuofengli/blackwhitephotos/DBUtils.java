package com.zhuofengli.blackwhitephotos;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class DBUtils extends SQLiteOpenHelper {
	private final static int DATABASE_VERSION = 1;
	private static String DBNAME = "BLACK_WHITE_PHOTOS_DB";
	private static String PHOTOS_TABLE = "PHOTOS_TABLE";

	private static final String createTable = "CREATE TABLE IF NOT EXISTS  "
			+ PHOTOS_TABLE
			+ " ( photo_id INTEGER PRIMARY KEY, photo_uri VARCHAR, thumb_uri VARCHAR, datetime VARCHAR )";

	public DBUtils(Context context) {
		super(context, DBNAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL(createTable);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		db.execSQL("DROP TABLE IF EXISTS " + PHOTOS_TABLE);

		onCreate(db);
	}

	public void insertPhoto(SQLiteDatabase db, PhotoRow pr) {

		ContentValues values = new ContentValues();
		if (pr.getPhotoId() > 0)
			values.put("photo_id", pr.getPhotoId());
		values.put("photo_uri", pr.getPhotoUri().toString());
		values.put("thumb_uri", pr.getThumbUri().toString());
		values.put("datetime", pr.getDatetime().toString());
		
		try {
			Cursor cur = db.rawQuery("select * from " + PHOTOS_TABLE
					+ " where photo_id = " + pr.getPhotoId(), null);
			if (cur.moveToNext()) {
				int r = db.update(PHOTOS_TABLE, values,
						"photo_id = " + pr.getPhotoId(), null);
				Log.i("sql", "update " + PHOTOS_TABLE + " with result " + r);
			} else {

				long r = db.insert(PHOTOS_TABLE, null, values);
				Log.i("sql", "insert into " + PHOTOS_TABLE + " with result "
						+ r + " VALUES: " + pr.getPhotoUri().toString() + " "
						+ pr.getThumbUri().toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public final static int NUM_PHOTOS_PER_PAGE = 20;	//load 20 photos everytime

	public List<PhotoRow> selectPhotos(SQLiteDatabase db, int page) {
		Cursor cur = null;
		List<PhotoRow> result = new ArrayList<PhotoRow>();
		try {
			int start = page * NUM_PHOTOS_PER_PAGE;

			cur = db.rawQuery("SELECT * FROM " + PHOTOS_TABLE
					+ " ORDER BY photo_id DESC LIMIT " + start + ","
					+ NUM_PHOTOS_PER_PAGE, null);
			
			while (cur.moveToNext()) {
				PhotoRow row = new PhotoRow();
				row.setPhotoId(cur.getInt(cur.getColumnIndex("photo_id")));

				String photoUriStr = cur.getString(cur
						.getColumnIndex("photo_uri"));
				row.setPhotoUri(Uri.parse(photoUriStr));

				String thumbUriStr = cur.getString(cur
						.getColumnIndex("thumb_uri"));
				row.setThumbUri(Uri.parse(thumbUriStr));

				row.setDatetime(cur.getString(cur.getColumnIndex("datetime")));
				
				result.add(row);
			}
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (cur != null)
				cur.close();
		}

		Log.i("SQL select data", "num of result = " + result.size());
		return result;
	}

}
