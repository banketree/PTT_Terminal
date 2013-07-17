package com.zzy.ptt.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AutoCompDb {

	public static final String KEY_DAILS = "dails";
	public static final String KEY_NAME = "name";
	public static final String KEY_ROWID = "_id";

	private static final String TAG = "AutoCompDb";
	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private static final String DATABASE_CREATE = "create table autodial (_id integer primary key autoincrement, "
			+ "dails integer, name text not null);";

	private static final String DATABASE_NAME = "autocomp";
	private static final String DATABASE_TABLE = "autodial";
	private static final int DATABASE_VERSION = 2;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS autodial");
			onCreate(db);
		}
	}

	public AutoCompDb(Context ctx) {
		this.mCtx = ctx;
	}

	public AutoCompDb open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	public long createAutoComp(int dails, String name) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_DAILS, dails);
		initialValues.put(KEY_NAME, name);

		return mDb.insert(DATABASE_TABLE, null, initialValues);
	}

	public Cursor fetchAllAutoComp() {

		return mDb.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_DAILS,
				KEY_NAME }, null, null, null, null, null);
	}

	public Cursor fetchAutoComp(String str) throws SQLException {
		String selection = "name like \'" + str + "%\'";
		Cursor mCursor =

		mDb.query(true, DATABASE_TABLE, new String[] { KEY_ROWID, KEY_DAILS,
				KEY_NAME }, selection, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;

	}

}
