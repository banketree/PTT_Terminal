/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project zzy PTT V1.0
 * Name StateManager.java
 * Description StateManagers
 * Author LiXiaodong
 * Version 1.0
 * Date 2012-3-15
 */

package com.zzy.ptt.service;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.zzy.ptt.R;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.util.PTTConstant;

/**
 * @author Administrator
 * 
 */
public class StateManager {
	
	public enum EnumRegByWho {
		REG_BY_USER,
		REG_BY_CM,
		REG_BY_ERRORCODE
	}

	public static boolean debugFlag = false;
	
	public static boolean exitFlag = false;
	
	/* The current registeration state */
	private static EnumLoginState currentRegisterState = EnumLoginState.UNREGISTERED;
	/* The current cm state */
	private static int cmState = PTTConstant.CM_CONNECTED;
	/* The current init state */
	private static int initState = PTTConstant.INIT_NOT;

	private static Notification notification = null;
	private static NotificationManager nm;
	
	//add by wangjunhui
	private static PendingIntent pendingIntent;
	public static boolean isStopped = false;
	private static Context context;
	private static PendingIntent contentIntent;
	
	private static final String LOG_TAG = "StateManager";
	
	private static PowerManager.WakeLock sPttWakeLock;
	
	private static KeyguardManager.KeyguardLock sPttKeyguardLock;
	
	private static boolean bEarphone;
	
	private static boolean bGrpRecived;
	
	private static EnumRegByWho regStarter;
	
	//edit by wangjunhui
		public static void initNotification(Context context, String ticker, String title, String msg) {
			StateManager.context = context;
			if (notification == null) {
				notification = new Notification(R.drawable.register_ing, ticker,
						System.currentTimeMillis());
			}
			startNotificationThread();
		}

	public static EnumLoginState getCurrentRegState() {
		return currentRegisterState;
	}

	public static void setCurrentRegState(EnumLoginState state) {

		Log.d("StateManager", "setCurrentRegState state : " + state + "notification == null : "
				+ (notification == null));
		if (notification == null) {
			notification = new Notification(R.drawable.register_not, "PTT",
					System.currentTimeMillis());
		}
		currentRegisterState = state;
		switch (state) {
		case UNREGISTERED:
		case ERROR_CODE_NUMBER:
			notification.icon = R.drawable.register_not;
			if (PTTService.instance != null) {
				PTTService.instance.stopNotificationPicture();
			}
			break;
		case REGISTERING:
			notification.icon = R.drawable.register_ing;
			if (PTTService.instance != null) {
				PTTService.instance.stopNotificationPicture();
			}
			break;
		case REGISTERE_SUCCESS:
			notification.icon = R.drawable.register_success;
			break;
		default:
			break;
		}
		if (nm != null) {
			nm.notify(R.string.app_name, notification);
		}
	}
	
	//add by wangjunhui
	public static void stopNotification() {
		isStopped = true;
		if(nm != null)
			nm.cancel(R.string.app_name);
	}
	//add by wangjunhui
	public static PendingIntent getPendingIntent() {
		return pendingIntent;
	}
	//add by wangjunhui
	public static void setPendingIntent(PendingIntent pendingIntent) {
		StateManager.pendingIntent = pendingIntent;
	}
	//add by wangjunhui
	public static void setNotification(Notification notification) {
		StateManager.notification = notification;
	}

	public static Notification getNotification() {
		return notification;
	}

	public static void setNm(NotificationManager nm) {
		StateManager.nm = nm;
	}

	public static int getCmState() {
		return cmState;
	}

	public static void setCmState(int cmState) {
		StateManager.cmState = cmState;
	}

	public static int getInitState() {
		return initState;
	}

	public static void setInitState(int initState) {
		StateManager.initState = initState;
	}

	public static void reset() {
		currentRegisterState = EnumLoginState.UNREGISTERED;
		initState = PTTConstant.INIT_NOT;
		//cmState = PTTConstant.CM_DISCONNECTED;
		//-----add by wangjunhui
		stopNotification();
		nm = null;
		notification = null;
		releaseWakeLock();
	}
	
	//-----add by wangjunhui
	public static void startNotificationThread() {
		isStopped = false;
		ContextWrapper cw = new ContextWrapper(context);
		contentIntent = getPendingIntent();
		RemoteViews contentView = new RemoteViews(cw.getPackageName(),
				R.layout.register_notify);
		notification.contentView = contentView;
		notification.contentIntent = contentIntent;
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		notification.icon = R.drawable.register_ing;
		notification.iconLevel = 0;
		//nm.notify(R.string.app_name, notification);
		Thread thread = new Thread(new MyThread());
		thread.start();
	}
	//-----add by wangjunhui
	static class MyThread implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			while (!isStopped) {
				try {
					Thread.sleep(300);
					handler.sendEmptyMessage(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}
	//-----add by wangjunhui
	private static Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (notification != null) {
				if (notification.iconLevel < 5) {
					notification.iconLevel++;
				} else {
					notification.iconLevel = 0;
				}
				if(nm != null){
					if (!isStopped) {
						nm.notify(R.string.app_name, notification);
					} else {
						nm.cancel(R.string.app_name);
					}
				}
			}
		}
	};
	
	public static void acquireWakeLock(Context context){
		
		Log.d(LOG_TAG, ">>>>>>>accquire cpu wake lock");
		if(sPttWakeLock == null){
            Log.d(LOG_TAG, "accquire cpu wake lock<<<<<<<<<<<");
            PowerManager pm= (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            sPttWakeLock =  pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, LOG_TAG);
            sPttWakeLock.acquire();
			return;
		}

		if (sPttKeyguardLock == null) {
			KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
			sPttKeyguardLock = km.newKeyguardLock(LOG_TAG);
			sPttKeyguardLock.disableKeyguard();
			Log.i("wwwangjh", "acquireWakeLock disableKeyguard()...");
		}
	}
	
	public static void releaseWakeLock(){
        Log.d(LOG_TAG, ">>>>>>>release cpu wake lock");
		if(sPttWakeLock != null && sPttWakeLock.isHeld()){
			sPttWakeLock.release();
			sPttWakeLock = null;
		}
		if(sPttKeyguardLock != null){
			sPttKeyguardLock.reenableKeyguard();
			sPttKeyguardLock = null;
		}
	}

	public static boolean isbEarphone() {
		return bEarphone;
	}

	public static void setbEarphone(boolean bEar) {
		StateManager.bEarphone = bEar;
	}

	public static EnumRegByWho getRegStarter() {
		return regStarter;
	}

	public static void setRegStarter(EnumRegByWho regStarter) {
		StateManager.regStarter = regStarter;
	}

	public static boolean isbGrpRecived() {
		return bGrpRecived;
	}

	public static void setbGrpRecived(boolean bGrpRecived) {
		StateManager.bGrpRecived = bGrpRecived;
	}
}
