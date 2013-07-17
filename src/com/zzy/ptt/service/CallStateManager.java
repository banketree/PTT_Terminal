/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : GroupManager.java
 * Author : LiXiaodong
 * Version : 1.0
 * Date : 2012-4-12
 */
package com.zzy.ptt.service;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.media.AudioManager;
import android.util.Log;

import com.zzy.ptt.R;
import com.zzy.ptt.db.DBHelper;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.CallLog;
import com.zzy.ptt.model.CallState;
import com.zzy.ptt.ui.InCallScreenActivity;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author lxd
 * 
 */
public class CallStateManager {

	private static final String LOG_TAG = "CallStateManager";

	private CallState currentCallState;

	private int formerState;
	private AudioManager audioManager;
	public int currentAudioRoute;

	private PTTUtil pttUtil = PTTUtil.getInstance();

	private static final boolean bDebug = true;

	private boolean bForceInSC = false;

	private DBHelper dbHelper;

	private CallStateManager() {
		currentCallState = new CallState();
		if (PTTService.instance != null) {
			audioManager = (AudioManager) PTTService.instance.getSystemService(Context.AUDIO_SERVICE);
			dbHelper = new DBHelper(PTTService.instance);
		}
	}

	private static CallStateManager instance = new CallStateManager();

	public static CallStateManager getInstance() {
		if (instance == null) {
			instance = new CallStateManager();
		}
		return instance;
	}

	public CallState getCurrentCallState() {
		return currentCallState;
	}

	public void setCurrentCallState(CallState srcCallState) {
		if (srcCallState == null)
			return;

		formerState = currentCallState.getCallState();
		currentCallState.setCallId(srcCallState.getCallId());
		currentCallState.setCallState(srcCallState.getCallState());
		currentCallState.setCallState(srcCallState.getCallState());
		currentCallState.setErrorCode(srcCallState.getErrorCode());
		currentCallState.setNumber(srcCallState.getNumber());
		currentCallState.setSeconds(srcCallState.getSeconds());
		Log.d(LOG_TAG, "setCurrentCallState : " + currentCallState.toString());
	}

	public void setCallState(int callState) {
		Log.d(LOG_TAG, "******************setCallState : " + pttUtil.getCallStateString(callState));
		formerState = currentCallState.getCallState();
		currentCallState.setCallState(callState);
	}

	public int getCallState() {
		return currentCallState == null ? PTTConstant.CALL_IDLE : currentCallState.getCallState();
	}

	public int getCallId() {
		return currentCallState.getCallId();
	}

	public int getFormerCallState() {
		return formerState;
	}

	public void clear() {
		pttUtil.printLog(bDebug, LOG_TAG, "---------------------------CallStateManager clear");
		currentCallState = new CallState();
		currentCallState.setCallState(PTTConstant.CALL_IDLE);
		formerState = PTTConstant.CALL_IDLE;
		bForceInSC = false;
		PTTService.instance.isPttNotiClosed = true;
	}

	public boolean isSingleCallOnTop() {
		// pttUtil.printLog(bDebug, LOG_TAG, "isSingleCallOnTop : "
		// + (InCallScreenActivity.instance != null));
		//PTTService.instance.getTopActivity();
		return (InCallScreenActivity.instance != null && "com.zzy.ptt.ui.InCallScreenActivity"
				.equals(PTTService.instance.getTopActivity()));
	}

	public boolean checkErrorCode(int errorCode) {
		return ((errorCode > 300) && (errorCode != 487));
		// return false;
	}

	public boolean isbForceInSC() {
		return bForceInSC;
	}

	public void setbForceInSC(boolean bForceInSC) {
		this.bForceInSC = bForceInSC;
	}

	public void closeCallUI() {
		if (InCallScreenActivity.instance != null) {
			InCallScreenActivity.instance.finish();
			// InCallScreenActivity.instance = null;
		}
	}

	public String toString() {
		return "CallStateManager [currentCallState=" + currentCallState + ", formerState="
				+ pttUtil.getCallStateString(formerState) + ", bForceInSC=" + bForceInSC + "]";
	}

	public void setAR() {
		boolean handsetFlag = PTTService.instance.prefs.getBoolean(
				PTTService.instance.getString(R.string.sp_ar_handset), true);
		if (audioManager == null) {
			audioManager = (AudioManager) PTTService.instance.getSystemService(Context.AUDIO_SERVICE);
		}

		if (handsetFlag) {
			currentAudioRoute = PTTConstant.AR_HANDSET;
			audioManager.setSpeakerphoneOn(false);
			audioManager.setMode(AudioManager.MODE_IN_CALL);
		} else {
			currentAudioRoute = PTTConstant.AR_SPEAKER;
			audioManager.setSpeakerphoneOn(true);
			audioManager.setMode(AudioManager.MODE_NORMAL);
		}

		pttUtil.printLog(bDebug, LOG_TAG, "Audio Route has changed to " + pttUtil.getARString(currentAudioRoute));
	}

	public synchronized void saveCallLog(final CallLog callLog) {

		pttUtil.printLog(bDebug, LOG_TAG, callLog + " save beging");
		if (callLog == null || callLog.getNum() == null || callLog.getLogType() == PTTConstant.LOG_ALL) {
			return;
		}

		if (dbHelper == null) {
			dbHelper = new DBHelper(PTTService.instance);
		}

		int count = dbHelper.queryLogCountByTime(callLog.getTime().getTime());
		pttUtil.printLog(bDebug, LOG_TAG, callLog + "count " + count);
		if (count > 0) {
			pttUtil.printLog(bDebug, LOG_TAG, callLog + " already in db");
			return;
		}

//		new Thread(new Runnable() {
//
//			public void run() {}
//		}).start();
		

		int maxCount = -1;
		try {
			maxCount = dbHelper.queryLogCount(PTTConstant.LOG_ALL);
		} catch (Exception e) {
			pttUtil.printLog(bDebug, LOG_TAG, "error to query callLog : " + callLog);
			pttUtil.errorAlert(PTTService.instance, -1, "error in query callLog!", false);
		}
		if (maxCount >= PTTConstant.CALLLOG_MAX) {
			// delete
			CallLog log = null;
			try {
				log = dbHelper.queryLatestLog();
			} catch (Exception e) {
				pttUtil.printLog(bDebug, LOG_TAG, "error to query LatestLog" + log);
				pttUtil.errorAlert(PTTService.instance, -1, "error in query LatestLog!", false);
			}
			if (log == null) {
				return;
			} else {
				try {
					dbHelper.deleteCallLog(log.getId(), false);
				} catch (Exception e) {
					pttUtil.printLog(bDebug, LOG_TAG, "error to delete CallLog" + log);
					pttUtil.errorAlert(PTTService.instance, -1, "error in delete CallLog!", false);
				}
			}
		}

		try {
			dbHelper.insertCallLog(callLog);
		} catch (PTTException e) {
			pttUtil.printLog(bDebug, LOG_TAG, "error to insert callLog : " + callLog);
			pttUtil.errorAlert(PTTService.instance, -1,
					PTTService.instance.getString(R.string.alert_msg_calllog_save_fail), false);
		} catch (SQLiteDiskIOException e) {
			pttUtil.printLog(bDebug, LOG_TAG, "No enough disk space for calllog " + callLog);
			pttUtil.errorAlert(PTTService.instance, -1,
					PTTService.instance.getString(R.string.alert_msg_calllog_save_fail), false);
		} catch (SQLiteFullException e) {
			pttUtil.printLog(bDebug, LOG_TAG, "No enough disk space for calllog " + callLog);
			pttUtil.errorAlert(PTTService.instance, -1,
					PTTService.instance.getString(R.string.alert_msg_calllog_save_fail), false);
		} catch (SQLiteConstraintException e) {
			if (callLog.getNum() == null) {
				pttUtil.printLog(bDebug, LOG_TAG, "SQLiteConstraintException error to insert callLog : "
						+ callLog);
			}
		} catch (Exception e) {
			if (callLog.getNum() == null) {
				pttUtil.printLog(bDebug, LOG_TAG, "Exception error to insert callLog : " + callLog);
			}
		}

	
		pttUtil.printLog(bDebug, LOG_TAG, callLog + " save over");
	}

	public void setSCVolume(int callstate) {
		Context context = PTTService.instance;

		if (audioManager == null) {
			audioManager = (AudioManager) PTTService.instance.getSystemService(Context.AUDIO_SERVICE);
		}

		switch (callstate) {
		case PTTConstant.CALL_RINGING:
			audioManager.setStreamVolume(
					AudioManager.STREAM_MUSIC,
					PTTService.instance.prefs.getInt(context.getString(R.string.sp_call_ring_volume),
							audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);
			break;
		case PTTConstant.CALL_TALKING:
			audioManager.setStreamVolume(
					AudioManager.STREAM_VOICE_CALL,
					PTTService.instance.prefs.getInt(context.getString(R.string.sp_call_single_volume),
							audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)), 0);
			break;
		default:
			break;
		}
	}

}
