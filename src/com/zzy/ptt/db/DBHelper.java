/**
 * Copyright 512 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:DBHelper.java
 * Description:DBHelper , the proxy of db operations
 * Author:LiXiaodong
 * Version:1.0
 * Date:512-3-5
 */

package com.zzy.ptt.db;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.CallLog;
import com.zzy.ptt.util.PTTConstant;

/**
 * @author Administrator
 * 
 */
public class DBHelper {

	private Context context;

	private final String LOG_TAG = "DBHelper";

	private MyDBOpenHelper dbHelper;
	public static int index = 20;

	public DBHelper(Context context) {
		this.context = context;
	}

	public void initDB() {
		dbHelper = new MyDBOpenHelper(context, PTTConstant.DB_NAME, null, PTTConstant.DB_VERSION);
	}

	private class MyDBOpenHelper extends SQLiteOpenHelper {

		public MyDBOpenHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(LOG_TAG, ">>>>>>>>>MyDBOpenHelper onCreate");
			createCallLogTable(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d(LOG_TAG, ">>>>>>>>>MyDBOpenHelper onUpgrade");
		}
	};

	private void createCallLogTable(SQLiteDatabase db) {
		Log.d(LOG_TAG, "createCallLogTable");
		StringBuilder sb = new StringBuilder();
		sb.append("create table ").append(PTTConstant.DB_TABLE_CALLLOG);
		sb.append("(");
		sb.append(PTTConstant.TABLE_CALLLOG_ID).append(" INTEGER PRIMARY KEY autoincrement, ");
		sb.append(PTTConstant.TABLE_CALLLOG_CALLID).append(" INTEGER NOT NULL,");
		sb.append(PTTConstant.TABLE_CALLLOG_NUM).append(" TEXT NOT NULL,");
		sb.append(PTTConstant.TABLE_CALLLOG_LOGTYPE).append(" INTEGER, ");
		sb.append(PTTConstant.TABLE_CALLLOG_DURATION).append(" INTEGER, ");
		sb.append(PTTConstant.TABLE_CALLLOG_REASON).append(" TEXT NOT NULL,");
		sb.append(PTTConstant.TABLE_CALLLOG_TIME).append(" INTEGER ");
		sb.append(")");
		db.execSQL(sb.toString());
	}

	private SQLiteDatabase openDB() {
		if (dbHelper == null) {
			initDB();
		}
		return dbHelper.getWritableDatabase();
	}

	private void closeDB(SQLiteDatabase db) {
		db.close();
		dbHelper.close();
	}
	
	public long insertCallLog(CallLog callLog) throws PTTException, SQLiteDiskIOException {
		SQLiteDatabase db = openDB();

		ContentValues values = new ContentValues();
		values.put(PTTConstant.TABLE_CALLLOG_CALLID, callLog.getCallId());
		values.put(PTTConstant.TABLE_CALLLOG_NUM, callLog.getNum());
		values.put(PTTConstant.TABLE_CALLLOG_LOGTYPE, callLog.getLogType());
		values.put(PTTConstant.TABLE_CALLLOG_DURATION, callLog.getDuration());
		values.put(PTTConstant.TABLE_CALLLOG_TIME, callLog.getTime().getTime());
		values.put(PTTConstant.TABLE_CALLLOG_REASON, callLog.getReason());

		long result = -1;
		try {
			result = db.insert(PTTConstant.DB_TABLE_CALLLOG, null, values);
		} catch (SQLiteDiskIOException e) {
			throw e;
		} catch (SQLiteFullException e) {
			throw e;
		} catch (Exception e) {
			throw new PTTException("insert into db error! num : " + callLog.getNum());
		} finally {
			closeDB(db);
		}

		return result;
	}

	public void resetIndex() {
		index = 20;
	}

	public List<CallLog> queryCallLog(int logType) {
		SQLiteDatabase db = openDB();
		String sqlAll = "select * from " + PTTConstant.DB_TABLE_CALLLOG;
		String where = " where " + PTTConstant.TABLE_CALLLOG_LOGTYPE + "=" + logType;

		String sql = null;
		if (logType == PTTConstant.LOG_ALL) {
			sql = sqlAll;
		} else {
			sql = sqlAll + where;
		}
		sql = sql + " order by " + PTTConstant.TABLE_CALLLOG_TIME + " desc  limit 20 offset 0";
		Cursor cursor = db.rawQuery(sql, null);

		List<CallLog> listCallLog = new ArrayList<CallLog>();

		int idIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_ID);
		int callIdIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_CALLID);
		int numIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_NUM);
		int logTypeIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_LOGTYPE);
		int durationIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_DURATION);
		int timeIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_TIME);
		int reasonIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_REASON);

		CallLog callLog = null;
		if (cursor.moveToFirst()) {
			do {
				callLog = new CallLog();
				callLog.setId(cursor.getInt(idIndex));
				callLog.setCallId(cursor.getInt(callIdIndex));
				callLog.setLogType(cursor.getInt(logTypeIndex));
				callLog.setNum(cursor.getString(numIndex));
				callLog.setDuration(cursor.getLong(durationIndex));
				callLog.setTime(new Timestamp(cursor.getLong(timeIndex)));
				callLog.setReason(cursor.getString(reasonIndex));
				listCallLog.add(callLog);
			} while (cursor.moveToNext());
		}
		closeDB(db);
		cursor.close();
		return listCallLog;
	}

	public List<CallLog> queryCallLogAdd(int logType) {
		SQLiteDatabase db = openDB();
		String sqlAll = "select * from " + PTTConstant.DB_TABLE_CALLLOG;
		String where = " where " + PTTConstant.TABLE_CALLLOG_LOGTYPE + "=" + logType;

		String sql = null;
		if (logType == PTTConstant.LOG_ALL) {
			sql = sqlAll;
		} else {
			sql = sqlAll + where;
		}
		sql = sql + " order by " + PTTConstant.TABLE_CALLLOG_TIME + " desc  limit 20 offset " + index;
		Cursor cursor = db.rawQuery(sql, null);
		index += 20;
		List<CallLog> listCallLog = new ArrayList<CallLog>();

		int idIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_ID);
		int callIdIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_CALLID);
		int numIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_NUM);
		int logTypeIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_LOGTYPE);
		int durationIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_DURATION);
		int timeIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_TIME);
		int reasonIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_REASON);

		CallLog callLog = null;
		if (cursor.moveToFirst()) {
			do {
				callLog = new CallLog();
				callLog.setId(cursor.getInt(idIndex));
				callLog.setCallId(cursor.getInt(callIdIndex));
				callLog.setLogType(cursor.getInt(logTypeIndex));
				callLog.setNum(cursor.getString(numIndex));
				callLog.setDuration(cursor.getLong(durationIndex));
				callLog.setTime(new Timestamp(cursor.getLong(timeIndex)));
				callLog.setReason(cursor.getString(reasonIndex));
				listCallLog.add(callLog);
			} while (cursor.moveToNext());
		}
		
		closeDB(db);
		cursor.close();
		return listCallLog;
	}

	public boolean deleteCallLog(int id, boolean bAll) {
		boolean flag = false;
		SQLiteDatabase db = openDB();

		String sql = "delete from " + PTTConstant.DB_TABLE_CALLLOG;

		if (!bAll) {
			sql += " where " + PTTConstant.TABLE_CALLLOG_ID + " = " + id;
		}
		try {
			db.execSQL(sql);
			flag = true;
		} catch (Exception e) {
			Log.e(LOG_TAG, "deleteCallLog fail, id : " + id + " bAll : " + bAll);
		} finally {
			closeDB(db);
		}
		return flag;
	}

	public boolean deleteTabCallLog(int logType) {
		boolean b = false;
		SQLiteDatabase db = openDB();

		int num = db.delete(PTTConstant.DB_TABLE_CALLLOG, PTTConstant.TABLE_CALLLOG_LOGTYPE + "=" + logType, null);
		if (num > 0) {
			b = true;
		}
		closeDB(db);
		return b;
	}

	public int queryLogCount(int logType) {
		int count = 0;
		SQLiteDatabase db = openDB();
		StringBuilder sb = new StringBuilder();
		sb.append("select count(*) from ").append(PTTConstant.DB_TABLE_CALLLOG);
		if (logType != PTTConstant.LOG_ALL) {
			sb.append(" where ").append(PTTConstant.TABLE_CALLLOG_LOGTYPE).append("=").append(logType);
		}

		Cursor cursor = db.rawQuery(sb.toString(), null);
		cursor.moveToFirst();
		count = cursor.getInt(0);
		cursor.close();

		closeDB(db);
		return count;
	}
	
	public int queryLogCountByTime(long time) {
		int count = 0;
		SQLiteDatabase db = openDB();
		StringBuilder sb = new StringBuilder();
		sb.append("select count(*) from ").append(PTTConstant.DB_TABLE_CALLLOG);
		sb.append(" where ").append(PTTConstant.TABLE_CALLLOG_TIME).append("=").append(time);
		Log.d(LOG_TAG, "sql " + sb.toString());
		Cursor cursor = db.rawQuery(sb.toString(), null);
		cursor.moveToFirst();
		count = cursor.getInt(0);
		cursor.close();

		closeDB(db);
		return count;
	}
	
	

	public CallLog queryLatestLog() {
		CallLog callLog = null;
		SQLiteDatabase db = openDB();
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(PTTConstant.DB_TABLE_CALLLOG).append(" order by time asc limit 1");
		Cursor cursor = db.rawQuery(sb.toString(), null);
		int idIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_ID);
		int callIdIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_CALLID);
		int numIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_NUM);
		int logTypeIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_LOGTYPE);
		int durationIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_DURATION);
		int timeIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_TIME);
		int reasonIndex = cursor.getColumnIndex(PTTConstant.TABLE_CALLLOG_REASON);
		if (cursor.moveToFirst()) {
			callLog = new CallLog();
			callLog.setId(cursor.getInt(idIndex));
			callLog.setCallId(cursor.getInt(callIdIndex));
			callLog.setLogType(cursor.getInt(logTypeIndex));
			callLog.setNum(cursor.getString(numIndex));
			callLog.setDuration(cursor.getLong(durationIndex));
			callLog.setTime(new Timestamp(cursor.getLong(timeIndex)));
			callLog.setReason(cursor.getString(reasonIndex));
		}
		cursor.close();
		closeDB(db);
		return callLog;
	}
}
