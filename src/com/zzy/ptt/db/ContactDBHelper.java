/**
* Copyright 2012 zzy Tech. Co., Ltd.
* All right reserved.
* Project : zzy PTT V1.0
* Name : ContactDBHelper
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
public class ContactDBHelper extends SQLiteOpenHelper{

	public static ContactDBHelper dbhelper;
	public static SQLiteDatabase db;

	public static ContactDBHelper getDBhelper(Context c){
		if(dbhelper==null){
			dbhelper=new ContactDBHelper(c);
		}
		return dbhelper;
	}
	
	public static SQLiteDatabase opDatabase(){
		if(db==null){
			db=dbhelper.getWritableDatabase();
		}
		return db;
	}
	public ContactDBHelper(Context context, String name, int version) {
		super(context, name,null, version);
		// TODO Auto-generated constructor stub
	}
	public ContactDBHelper(Context context, String name) {
		super(context, name,null, 1);
		// TODO Auto-generated constructor stub
	}
	
	public ContactDBHelper(Context context) {
		super(context, "contact.db",null, 1);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
			db.execSQL("create table contacts(_id Integer primary key autoincrement,imageId int,userName varchar(20),cellphone varchar(20))"
					);
	
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int  oldVersion, int newVersion) {
	}

}
