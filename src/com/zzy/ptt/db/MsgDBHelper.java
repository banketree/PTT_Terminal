/**
* Copyright 2012 zzy Tech. Co., Ltd.
* All right reserved.
* Project : zzy PTT V1.0
* Name : MsgDBHelper
* Author : wangjunhui
* Version : 1.0
* Date : 2012-04-26
*/
package com.zzy.ptt.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
/**
 * @author wangjunhui
 * 
 */
public class MsgDBHelper extends SQLiteOpenHelper {
	public static MsgDBHelper dbhelper;
	public static SQLiteDatabase db;

	public static MsgDBHelper getDBhelper(Context c) {
		if (dbhelper == null) {
			dbhelper = new MsgDBHelper(c);
		}
		return dbhelper;
	}

	public static SQLiteDatabase opDatabase() {
		if (db == null) {
			db = dbhelper.getWritableDatabase();
		}
		return db;
	}

	public MsgDBHelper(Context context, String name, int version) {
		super(context, name, null, version);
		// TODO Auto-generated constructor stub
	}

	public MsgDBHelper(Context context, String name) {
		super(context, name, null, 1);
		// TODO Auto-generated constructor stub
	}

	public MsgDBHelper(Context context) {
		super(context, "msg.db", null, 1);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL("create table msgs(_id Integer primary key autoincrement,thread_id Integer,address text,date text,body text,type Integer,readstatus Integer,sendstatus Integer)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

}
