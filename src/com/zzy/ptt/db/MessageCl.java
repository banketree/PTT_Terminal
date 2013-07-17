/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : MessageCl
 * Author : wangjunhui
 * Version : 1.0
 * Date : 2012-04-26
 */
package com.zzy.ptt.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.zzy.ptt.model.Message;

/**
 * @author wangjunhui
 * 
 */
public class MessageCl {

	public static final String ID = "_id";
	public static final String THREAD_ID = "thread_id";
	public static final String ADDRESS = "address";
	public static final String DATE = "date";
	public static final String BODY = "body";
	public static final String TYPE = "type";
	public static final String READSTATUS = "readstatus";
	public static final String SENDSTATUS = "sendstatus";
	public static final String TABLE_NAME = "msgs";

	private SQLiteDatabase db = null;
	private Set<Integer> chatNum = new HashSet<Integer>();

	public MessageCl(Context context) {
		MsgDBHelper.getDBhelper(context);
		db = MsgDBHelper.opDatabase();
	}

	public int getChatNum(String condition) {
		int threadNum = 0;
		Cursor c = null;
		if (condition.equals("")) {
			c = db.query(TABLE_NAME, new String[] { THREAD_ID }, null, null, null, null, null);
		} else {
		}
		while (c.moveToNext()) {
			threadNum = c.getInt(c.getColumnIndex(THREAD_ID));
			if (threadNum != 0) {
				chatNum.add(threadNum);
			}
		}
		c.close();
		return chatNum.size();
	}

	public Set<Integer> getThreadIds() {
		int threadid = 0;
		Set<Integer> threadids = new HashSet<Integer>();
		Cursor c = db.query(TABLE_NAME, new String[] { THREAD_ID }, null, null, null, null, null);
		while (c.moveToNext()) {
			threadid = c.getInt(c.getColumnIndex(THREAD_ID));
			if (threadid != 0) {
				threadids.add(threadid);
			}
		}
		c.close();
		return threadids;
	}

	public Set<String> getAddresses() {
		String address = null;
		Set<String> addresses = new HashSet<String>();
		Cursor c = db.query(TABLE_NAME, new String[] { ADDRESS }, null, null, null, null, null);
		while (c.moveToNext()) {
			address = c.getString(c.getColumnIndex(ADDRESS));
			if (address != null) {
				addresses.add(address);
			}
		}
		c.close();
		return addresses;
	}

	public int getThreadidFromAddress(String caddress) {
		int threadid = 0;
		Cursor c = db.query(TABLE_NAME, new String[] { THREAD_ID, ADDRESS, DATE, BODY, TYPE, READSTATUS, SENDSTATUS },
				ADDRESS + "=?", new String[] { caddress + "" }, null, null, null);
		if (c.getCount() != 0) {
			c.moveToFirst();
			threadid = c.getInt(c.getColumnIndex(THREAD_ID));
		}
		c.close();
		return threadid;
	}

	public int getThreadidFromID(int cid) {
		int threadid = 0;
		Cursor c = db.query(TABLE_NAME, new String[] { THREAD_ID, ADDRESS, DATE, BODY, TYPE, READSTATUS, SENDSTATUS },
				ID + "=?", new String[] { cid + "" }, null, null, null);
		if (c.getCount() != 0) {
			c.moveToFirst();
			threadid = c.getInt(c.getColumnIndex(THREAD_ID));
		}
		c.close();
		return threadid;
	}

	public int getMsgsNumInChat(String address) {
		int count = 0;
		String sql = "select count(*) from " + TABLE_NAME + " where " + ADDRESS + "=\"" + address + "\"";
		Cursor cursor = db.rawQuery(sql, null);
		cursor.moveToFirst();
		count = cursor.getInt(0);
		cursor.close();
		return count;
	}

	public int getIdFromMsg(Message msg) {
		String address = msg.getNumber();
		String date = msg.getDate();
		String body = msg.getBody();
		int type = msg.getType();
		Cursor c = db.query(TABLE_NAME,
				new String[] { ID, THREAD_ID, ADDRESS, DATE, BODY, TYPE, READSTATUS, SENDSTATUS },
				"address=? and date=? and body=? and type=?", new String[] { address + "", date, body, type + "" },
				null, null, null);
		int id = 0;
		if (c.getCount() != 0) {
			c.moveToFirst();
			id = c.getInt(c.getColumnIndex(ID));
		}
		c.close();
		return id;
	}

	public List<Message> getMsgAtrrayfromAddress(String caddress) {
		List<Message> list = new ArrayList<Message>();
		Cursor c = db.query(TABLE_NAME, new String[] { THREAD_ID, ADDRESS, DATE, BODY, TYPE, READSTATUS, SENDSTATUS },
				ADDRESS + "=?", new String[] { caddress + "" }, null, null, null);
		while (c.moveToNext()) {
			int thread_id = c.getInt(c.getColumnIndex(THREAD_ID));
			String address = c.getString(c.getColumnIndex(ADDRESS));
			String date = c.getString(c.getColumnIndex(DATE));
			String body = c.getString(c.getColumnIndex(BODY));
			int type = c.getInt(c.getColumnIndex(TYPE));
			int readstatus = c.getInt(c.getColumnIndex(READSTATUS));
			int sendstatus = c.getInt(c.getColumnIndex(SENDSTATUS));
			Message msg = new Message(thread_id, address, date, body, type, readstatus, sendstatus);
			list.add(msg);
		}
		c.close();
		return list;
	}

	public String getAddressFromThreadid(int cthread_id) {
		Cursor c = db.query(TABLE_NAME, new String[] { THREAD_ID, ADDRESS, DATE, BODY, TYPE, READSTATUS, SENDSTATUS },
				THREAD_ID + "=?", new String[] { cthread_id + "" }, null, null, null);
		c.moveToFirst();
		String address = c.getString(c.getColumnIndex(ADDRESS));
		c.close();
		return address;
	}

	/*
	 * public int getMaxIdWithThreadId(int cthread_id) { List<Integer>
	 * idWithThreadList = new ArrayList<Integer>(); Cursor c =
	 * db.query(TABLE_NAME, new String[] { ID, THREAD_ID, ADDRESS, DATE, BODY,
	 * TYPE, READSTATUS, SENDSTATUS }, THREAD_ID + "=?", new String[] {
	 * cthread_id + "" }, null, null, null); while (c.moveToNext()) { int id =
	 * c.getInt(c.getColumnIndex(ID)); idWithThreadList.add(id); } c.close();
	 * return maxInList(idWithThreadList); }
	 */
	public List<Message> getSessionList() {
		List<Message> lstSession = new ArrayList<Message>();
		String sql = "select * from " + TABLE_NAME + " group by " + THREAD_ID + " order by " + ID + " desc";
		Cursor cursor = db.rawQuery(sql, null);

		int threadIdIndex = cursor.getColumnIndex(THREAD_ID);
		int addressIndex = cursor.getColumnIndex(ADDRESS);
		int dateIndex = cursor.getColumnIndex(DATE);
		int bodyIndex = cursor.getColumnIndex(BODY);
		int typeIndex = cursor.getColumnIndex(TYPE);
		int readstatusIndex = cursor.getColumnIndex(READSTATUS);
		int sendstatusIndex = cursor.getColumnIndex(SENDSTATUS);

		Message message = null;
		if (cursor.moveToFirst()) {
			do {
				message = new Message();
				message.setThreadId(cursor.getInt(threadIdIndex));
				message.setNumber(cursor.getString(addressIndex));
				message.setBody(cursor.getString(bodyIndex));
				message.setDate(cursor.getString(dateIndex));
				message.setType(cursor.getInt(typeIndex));
				message.setReadstatus(cursor.getInt(readstatusIndex));
				message.setSendstatus(cursor.getInt(sendstatusIndex));
				lstSession.add(message);
			} while (cursor.moveToNext());
		}

		cursor.close();
		return lstSession;
	}

	public long addNew(Message ms) {
		
		long result ;
		
		Set<Integer> threadid_set = getThreadIds();
		// List<Integer> threadid_list = new ArrayList<Integer>(threadid_set);
		int thread_id = ms.getThreadId();
		if (threadid_set.isEmpty()) {
			thread_id = 1;
		} else if (thread_id == 0) {
			thread_id = getMaxThreadId() + 1;
		} else {
			thread_id = ms.getThreadId();
		}
		ContentValues values = new ContentValues();
		values.put(THREAD_ID, thread_id);
		values.put(ADDRESS, ms.getNumber());
		values.put(DATE, ms.getDate());
		values.put(BODY, ms.getBody());
		values.put(TYPE, ms.getType());
		values.put(READSTATUS, ms.getReadstatus());
		values.put(SENDSTATUS, ms.getSendstatus());
		
		Log.d("MessageCl", "add msg : " + ms.toString());

		result = db.insert(TABLE_NAME, null, values);
		
		return result;
	}

	private int getMaxThreadId() {

		Cursor c = db.rawQuery("select max(thread_id) from msgs", null);
		c.moveToFirst();
		int maxThreadId = c.getInt(0);

		c.close();

		return maxThreadId;
	}

	public Message getMsgFromThreadid(int cthread_id) {
		Cursor c = db.query(TABLE_NAME, new String[] { THREAD_ID, ADDRESS, DATE, BODY, TYPE, READSTATUS, SENDSTATUS },
				THREAD_ID + "=?", new String[] { cthread_id + "" }, null, null, null);
		c.moveToFirst();
		int thread_id = cthread_id;
		String address = c.getString(c.getColumnIndex(ADDRESS));
		String date = c.getString(c.getColumnIndex(DATE));
		String body = c.getString(c.getColumnIndex(BODY));
		int type = c.getInt(c.getColumnIndex(TYPE));
		int readstatus = c.getInt(c.getColumnIndex(READSTATUS));
		int sendstatus = c.getInt(c.getColumnIndex(SENDSTATUS));
		Message msg = new Message(thread_id, address, date, body, type, readstatus, sendstatus);
		c.close();
		return msg;
	}

	public Message getMsgFromID(int cid) {
		Cursor c = db.query(TABLE_NAME,
				new String[] { ID, THREAD_ID, ADDRESS, DATE, BODY, TYPE, READSTATUS, SENDSTATUS }, ID + "=?",
				new String[] { cid + "" }, null, null, null);
		c.moveToFirst();
		int thread_id = c.getInt(c.getColumnIndex(THREAD_ID));
		;
		String address = c.getString(c.getColumnIndex(ADDRESS));
		String date = c.getString(c.getColumnIndex(DATE));
		String body = c.getString(c.getColumnIndex(BODY));
		int type = c.getInt(c.getColumnIndex(TYPE));
		int readstatus = c.getInt(c.getColumnIndex(READSTATUS));
		int sendstatus = c.getInt(c.getColumnIndex(SENDSTATUS));
		Message msg = new Message(thread_id, address, date, body, type, readstatus, sendstatus);
		c.close();
		return msg;
	}

	public Message getMsgFromAddress(String caddress) {
		Cursor c = db.query(TABLE_NAME, new String[] { THREAD_ID, ADDRESS, DATE, BODY, TYPE, READSTATUS, SENDSTATUS },
				ADDRESS + "=?", new String[] { caddress + "" }, null, null, null);
		c.moveToFirst();
		int thread_id = c.getInt(c.getColumnIndex(THREAD_ID));
		String date = c.getString(c.getColumnIndex(DATE));
		String address = caddress;
		String body = c.getString(c.getColumnIndex(BODY));
		int type = c.getInt(c.getColumnIndex(TYPE));
		int readstatus = c.getInt(c.getColumnIndex(READSTATUS));
		int sendstatus = c.getInt(c.getColumnIndex(SENDSTATUS));
		Message msg = new Message(thread_id, address, date, body, type, readstatus, sendstatus);
		c.close();
		return msg;
	}

	public boolean updateMsgReadStatus(Message msg) {
		ContentValues values = new ContentValues();
		values.put(READSTATUS, msg.getReadstatus());
		int c = db.update(TABLE_NAME, values, THREAD_ID + "=" + msg.getThreadId(), null);
		return c > 0;
	}

	public boolean updateMsgReadStatus(int threadId, int readStatus) {
		ContentValues values = new ContentValues();
		values.put(READSTATUS, readStatus);
		int c = db.update(TABLE_NAME, values, THREAD_ID + "=" + threadId, null);
		return c > 0;
	}

	public boolean checkIfAnyUnreadSession(int threadId) {
		StringBuilder sbSql = new StringBuilder();
		sbSql.append("select count(*) from msgs where ");
		sbSql.append(READSTATUS);
		sbSql.append("=2");

		if (threadId != -1) {
			sbSql.append(" and ");
			sbSql.append(THREAD_ID);
			sbSql.append("=\"").append(threadId).append("\"");
		}

		Cursor c = db.rawQuery(sbSql.toString(), null);
		c.moveToFirst();
		int unreadCount = c.getInt(0);

		c.close();

		return unreadCount != 0;
	}

	public boolean updateMsgSendStatus(Message msg) {
		ContentValues values = new ContentValues();
		values.put(SENDSTATUS, msg.getSendstatus());
		int c = db.update(TABLE_NAME, values, ID + "=" + getIdFromMsg(msg), null);
		return c > 0;
	}

	public boolean deleteMsg(int id) {
		int num = db.delete(TABLE_NAME, ID + "=?", new String[] { id + "" });
		return num == 1;
	}

	public boolean deleteSession(int thread_id) {
		int num = db.delete(TABLE_NAME, THREAD_ID + "=?", new String[] { thread_id + "" });
		return num == 1;
	}

}
