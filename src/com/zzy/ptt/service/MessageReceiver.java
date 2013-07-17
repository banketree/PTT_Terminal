/**
* Copyright 2012 zzy Tech. Co., Ltd.
* All right reserved.
* Project : zzy PTT V1.0
* Name : MessageReceiver
* Author : wangjunhui
* Version : 1.0
* Date : 2012-04-26
*/
package com.zzy.ptt.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteFullException;

import com.zzy.ptt.R;
import com.zzy.ptt.db.MessageCl;
import com.zzy.ptt.model.Message;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;
/**
 * @author wangjunhui
 * 
 */
public class MessageReceiver extends BroadcastReceiver {
	private Message msg;
	private MessageCl cl;
	private int thread_id;
	public static NotificationManager nm;
	private Notification notification;
	public static final int TYPEME = 1;// me
	public static final int TYPEHE = 2;// others

	public static final int READ = 1;
	public static final int UNREAD = 2;
	
	public static final String FRESHVIEW = "com.wangjh.fresh";
	public static final String SENDSTATUS = "com.wangjh.sendstatus";
	public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
	
	private static boolean isSend= false;

	@Override
	public void onReceive(Context context, Intent intent) {
		cl = new MessageCl(context);
		cl.getThreadIds();
		nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		String action = intent.getAction();
		if (action.equals(PTTConstant.ACTION_NEW_MESSAGE)) {
			String address = intent
					.getStringExtra(PTTConstant.KEY_MESSAGE_NUMBER);
			String body = intent.getStringExtra(PTTConstant.KEY_MESSAGE_TEXT);
			String date = getDate();
			int type = TYPEHE;
			int readstatus = UNREAD;
			thread_id = cl.getThreadidFromAddress(address);
			msg = new Message(thread_id, address, date, body, type, readstatus);
			
			notification = new Notification(R.drawable.unread_msg, body,
					System.currentTimeMillis());
			notification.defaults = Notification.DEFAULT_SOUND;
			Intent intent2 = new Intent();
			intent2.setAction(FRESHVIEW);
			intent2.putExtra("fresh", 200);
			intent2.putExtra("newMsg", msg);
			PendingIntent pi = PendingIntent
					.getActivity(context, 0, intent2, 0);
			notification.setLatestEventInfo(context, "new message", body, pi);
			nm.notify(UNREAD, notification);
			try {
				cl.addNew(msg);
			} catch (SQLiteFullException e) {
				// TODO Auto-generated catch block
				PTTUtil.getInstance().errorAlert(context, -1, context.getString(R.string.alert_msg_no_enough_space_my), false);
				e.printStackTrace();
			}
			context.sendBroadcast(intent2);
		}
		if (action.equals(PTTConstant.ACTION_MESSAGE_RESULT) && isSend()) {
			int sendstatus = intent.getIntExtra(PTTConstant.KEY_MESSAGE_RESULT,
					666);
			Intent intent3 = new Intent();
			intent3.setAction(SENDSTATUS);
			intent3.putExtra("sendstatus", sendstatus);
			context.sendBroadcast(intent3);
			setSend(false);
		}
//		if (action.equals(BOOT_COMPLETED)) {
//			if (cl.getChatNum("") != 0) {
//				Set<String> addset = cl.getAddresses();
//				List<String> addlist = new ArrayList<String>(addset);
//				for (int i = 0; i < addlist.size(); i++) {
//					Message msg = cl.getMsgFromAddress(addlist.get(i));
//					int readstatus = msg.getReadstatus();
//					if (readstatus == UNREAD) {
//						notification = new Notification(R.drawable.unread_msg, "",
//								System.currentTimeMillis());
//						notification.defaults = Notification.DEFAULT_SOUND;
//						Intent intent2 = new Intent();
//						PendingIntent pi = PendingIntent
//								.getActivity(context, 0, intent2, 0);
//						notification.setLatestEventInfo(context, "new message", "", pi);
//						nm.notify(UNREAD, notification);
//					}
//				}
//			}
//		}
	}

	private String getDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
		Date d = new Date();
		return sdf.format(d);
	}

	public static boolean isSend() {
		return isSend;
	}

	public static void setSend(boolean issend) {
		isSend = issend;
	}

}
