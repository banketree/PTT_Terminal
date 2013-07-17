/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : ContactUserCl
 * Author : wangjunhui
 * Version : 1.0
 * Date : 2012-04-26
 */
package com.zzy.ptt.db;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.zzy.ptt.model.ContactUser;

/**
 * @author wangjunhui
 * 
 */
public class ContactUserCl {
	public static final String ID = "_id";
	public static final String IMGID = "imageId";
	public static final String USERNAME = "userName";
	public static final String PHONENUM = "cellphone";
	public static final String DBNAME = "contacts";
	public static final String COMULNS = "count(*)";
	public int index = 20;

	private SQLiteDatabase db = null;

	public ContactUserCl(Context context) {
		ContactDBHelper.getDBhelper(context);
		db = ContactDBHelper.opDatabase();
	}

	// get contacts nums
	public int getTotalUserNum(String condition) {
		int totalNum = 0;
		Cursor c;
		if (condition.equals("")) {
			c = db.query(DBNAME, new String[] { COMULNS }, null, null, null, null, null);
		} else {
			c = db.query(DBNAME, new String[] { COMULNS }, "userName like '" + condition + "%' or cellphone like '"
					+ condition + "%'", null, null, null, null);
		}
		while (c.moveToNext()) {
			totalNum = c.getInt(c.getColumnIndex(COMULNS));
		}
		c.close();
		return totalNum;
	}

	public String getNameFromNum(String num) {
		String userName = null;
		Cursor c = db.query(DBNAME, null, " cellphone like '" + num + "%'",
				null, null, null, null);
		while (c.moveToNext()) {
			userName = c.getString(c.getColumnIndex(USERNAME));
		}
		c.close();
		return userName;
	}

	// add contact
	public long addNew(ContactUser ub) {
		ContentValues values = new ContentValues();
		values.put(IMGID, ub.getImageId());
		values.put(USERNAME, ub.getName());
		values.put(PHONENUM, ub.getCellphone());

		return db.insert(DBNAME, null, values);
	}

	public ArrayList<String> getUserNums() {
		ArrayList<String> al = new ArrayList<String>();
		Cursor c = db.query(DBNAME, null, null, null, null, null, null);
		while (c.moveToNext()) {
			String cellphone = c.getString(c.getColumnIndex(PHONENUM));
			al.add(cellphone);
		}
		c.close();
		return al;
	}

	public void resetIndex() {
		index = 20;
	}

	// get userslist
	public ArrayList<HashMap<String, String>> getUserListInit() {
		ArrayList<HashMap<String, String>> al = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> map = null;
		Cursor c = db.rawQuery("select * from contacts limit 20 offset 0", null);
		// Cursor c = db.query(DBNAME, null, null, null, null, null, null);
		while (c.moveToNext()) {
			int _id = c.getInt(c.getColumnIndex(ID));
			int imageId = c.getInt(c.getColumnIndex(IMGID));
			String userName = c.getString(c.getColumnIndex(USERNAME));
			String cellphone = c.getString(c.getColumnIndex(PHONENUM));

			map = new HashMap<String, String>();
			map.put(ID, _id + "");
			map.put(IMGID, imageId + "");
			map.put(USERNAME, userName);
			map.put(PHONENUM, cellphone);
			al.add(map);
		}
		c.close();
		return al;
	}

	// get userslist
	public ArrayList<HashMap<String, String>> getUserListAdd() {
		ArrayList<HashMap<String, String>> al = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> map = null;
		Cursor c = db.rawQuery("select * from contacts limit 20 offset " + index, null);
		// Cursor c = db.query(DBNAME, null, null, null, null, null, null);
		while (c.moveToNext()) {
			int _id = c.getInt(c.getColumnIndex(ID));
			int imageId = c.getInt(c.getColumnIndex(IMGID));
			String userName = c.getString(c.getColumnIndex(USERNAME));
			String cellphone = c.getString(c.getColumnIndex(PHONENUM));

			map = new HashMap<String, String>();
			map.put(ID, _id + "");
			map.put(IMGID, imageId + "");
			map.put(USERNAME, userName);
			map.put(PHONENUM, cellphone);
			al.add(map);
		}
		index += 20;
		c.close();
		return al;
	}

	// update contact
	public int updateUser(ContactUser ub) {
		ContentValues values = new ContentValues();
		values.put(IMGID, ub.getImageId() + "");
		values.put(USERNAME, ub.getName());
		values.put(PHONENUM, ub.getCellphone());

		return db.update(DBNAME, values, ID + "=?", new String[] { ub.get_id() + "" });
	}

	// delete contact
	public boolean delete(int id) {
		boolean b = false;

		int num = db.delete(DBNAME, ID + "=?", new String[] { id + "" });
		if (num == 1) {
			b = true;
		}
		return b;
	}

	public ArrayList<HashMap<String, String>> selectBySearch(String condition) {
		ArrayList<HashMap<String, String>> al = new ArrayList<HashMap<String, String>>();
		;
		HashMap<String, String> map = null;
		Cursor c = db.query(DBNAME, null, "userName like '" + condition + "%' or cellphone like '" + condition + "%'",
				null, null, null, null);
		while (c.moveToNext()) {
			int _id = c.getInt(c.getColumnIndex(ID));
			int imageId = c.getInt(c.getColumnIndex(IMGID));
			String userName = c.getString(c.getColumnIndex(USERNAME));
			String cellphone = c.getString(c.getColumnIndex(PHONENUM));

			map = new HashMap<String, String>();
			map.put(ID, _id + "");
			map.put(IMGID, imageId + "");
			map.put(USERNAME, userName);
			map.put(PHONENUM, cellphone);
			al.add(map);
		}
		c.close();
		return al;
	}

}
