/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Projectzzy PTT V1.0
 * NamePTTService.java
 * DescriptionPTTService 
 * AuthorLiXiaodong
 * Version1.0
 * Date 2012-3-6
 */

package com.zzy.ptt.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.jni.JniCallback;
import com.zzy.ptt.jni.JniManager;
import com.zzy.ptt.model.CallState;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.model.MemberInfo;
import com.zzy.ptt.model.NumberInfo;
import com.zzy.ptt.model.PttGroupStatus;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.service.PTTManager.PTT_KEY;
import com.zzy.ptt.service.StateManager.EnumRegByWho;
import com.zzy.ptt.ui.AlertActivity;
import com.zzy.ptt.ui.InCallScreenActivity;
import com.zzy.ptt.ui.MainPageActivity;
import com.zzy.ptt.ui.PTTActivity;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class PTTService extends Service {

	private static final String LOG_TAG = "PTTService";

	private NotificationManager nm;
	public SharedPreferences prefs;
	private GroupManager groupManager;
	private PTTManager pttManager;
	private CallStateManager callStateManager;
	private Vibrator vibrator;
	private MediaPlayer ringPlayer;
	private AudioManager audioManager = null;
	private MediaPlayer mp = null;
	private PttInnerReceiver innerReceiver;

	private PTTUtil pttUtil = PTTUtil.getInstance();

	private static boolean bDebug = true;
	private boolean bRingPlaying = false;

	public static PTTService instance;
	public static Activity topActivity;

	public static List<Activity> activityList = new ArrayList<Activity>();

	public boolean isPttUIClosed = true;
	public boolean isPttNotiClosed = true;
	
	private ConnectivityManager connManager;
	private SoundPool mSoundPool;
	private int mMaxStreams = 8;

	NotificationManager manager;
	int[] sNotificationImages;
	Notification notificationPic;
	CharSequence contentTitle = null;
	CharSequence contentText = null;
	Intent notificationIntent = new Intent();
	PendingIntent contentIntent;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.d(LOG_TAG, "PTTService.onCreate");
		instance = this;

		JniManager.initJni();
		JniCallback.setService(this);
		// init notification
		StateManager.initNotification(this, getString(R.string.notify_ticker), getString(R.string.notify_title),
				getString(R.string.notify_content));
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		ringPlayer = new MediaPlayer();
		StateManager.setNm(nm);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// init prefs
		initPrefs();
		// init ringtones
		initRingtones();

		showNotification();

		groupManager = GroupManager.getInstance();
		pttManager = PTTManager.getInstance();
		callStateManager = CallStateManager.getInstance();
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		connManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		mSoundPool = new SoundPool(mMaxStreams, AudioManager.STREAM_MUSIC, 0);

		// init receiver
		initReceiver();

		// check audio route
		checkAR();

		manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		sNotificationImages = new int[] { R.drawable.ewin, R.drawable.wifi };
		notificationPic = new Notification(R.drawable.ewin, null, System.currentTimeMillis());
		contentIntent = PendingIntent.getActivity(instance, 0, notificationIntent, PendingIntent.FLAG_ONE_SHOT);
		
		sendBroadcast(new Intent(PTTConstant.ACTION_PTT_START));
	}

	private void checkAR() {
		boolean bEar = audioManager.isWiredHeadsetOn();
		StateManager.setbEarphone(bEar);
	}

	private void initReceiver() {
		innerReceiver = new PttInnerReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_HEADSET_PLUG);
		filter.addAction(PTTConstant.ACTION_DEBUG);
		filter.addAction(PTTConstant.ACTION_COMMAND);
		filter.addAction(PTTConstant.ACTION_INIT);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		this.registerReceiver(innerReceiver, filter);
	}

	private class PttInnerReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (action == null || action.trim().length() == 0) {
				return;
			}
			if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
				int state = intent.getIntExtra("state", -1);
				pttUtil.printLog(bDebug, LOG_TAG, "action : >>> " + action + ", state : " + state);
				if (state == -1) {
					return;
				}
				StateManager.setbEarphone(1 == state);
				int iStatus = pttManager.getCurrentPttState().getStatus();
				if (iStatus == PTTConstant.PTT_LISTENING || iStatus == PTTConstant.PTT_SPEAKING)
					pttManager.speakerCtrl(0 == state);
			} else if (PTTConstant.ACTION_COMMAND.equals(action)) {
				handleCommand(action, intent);
			} else if (PTTConstant.ACTION_INIT.equals(action)) {
				int initState = intent.getIntExtra(PTTConstant.KEY_INIT_STATE, 0);
				Log.d(LOG_TAG, "<<<<<<<<<<<Receive init state : " + initState + " back " + pttUtil.checkIsBackground());
				if (initState == PTTConstant.INIT_SUCCESS) {
					StateManager.setRegStarter(EnumRegByWho.REG_BY_USER);
					Intent intent2 = new Intent(PTTConstant.ACTION_REGISTER);
					intent2.putExtra(PTTConstant.KEY_REREGISTER_FORCE, true);
					handleRegister(PTTConstant.ACTION_REGISTER, intent2);
				} else if (initState == PTTConstant.INIT_FAIL && pttUtil.checkIsBackground()) {
					Log.d(LOG_TAG, "<<<<<<<<<<< init failed  ");
					pttUtil.errorAlert(context, 0, "init failed!", true);
				}
			} else if(ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
				NetworkInfo mobNetInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			    NetworkInfo wifiNetInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			    if (!mobNetInfo.isConnected() && !wifiNetInfo.isConnected()) {
			    	Toast.makeText(instance, "no net work", Toast.LENGTH_LONG).show();
			    } else {
					if(mobNetInfo.isConnected()) {
						Toast.makeText(instance, "mobNetInfo", Toast.LENGTH_LONG).show();
					}
					if(wifiNetInfo.isConnected()) {
						Toast.makeText(instance, "wifiNetInfo", Toast.LENGTH_LONG).show();
					}
				}
			}
		}
	}

	private void initRingtones() {
		String currentRing = prefs.getString(getString(R.string.sp_current_ringtone), null);
		if (currentRing == null || currentRing.trim().length() == 0) {
			currentRing = pttUtil.getFirstRingName(getResources(), "call_");
			if (currentRing != null) {
				Editor editor = prefs.edit();
				editor.putString(getString(R.string.sp_current_ringtone), currentRing);
				editor.commit();
			}
		}
	}

	private void initPrefs() {
		if (!prefs.getBoolean(getString(R.string.sp_init_flag), false)) {
			PreferenceManager.setDefaultValues(this, R.xml.setting_default, false);
			Editor editor = prefs.edit();
			editor.putBoolean(getString(R.string.sp_init_flag), true);
			editor.commit();
		}
		StateManager.debugFlag = prefs.getBoolean(getString(R.string.sp_debug_on), false);
		StateManager.exitFlag = false;
	}

	private void showNotification() {
		Notification notification = StateManager.getNotification();
		// notification.flags |= Notification.FLAG_AUTO_CANCEL;
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setClass(this, MainPageActivity.class);
		// important!!! switch to the top activity ,not the application entrance
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		PendingIntent pt = PendingIntent.getActivity(this, 0, intent, 0);
		if (notification != null) {
			notification.setLatestEventInfo(this, getString(R.string.notify_title), getString(R.string.notify_content),
					pt);
			nm.notify(R.string.app_name, notification);
		}
	}

	/*
	 * private void cancelNotification() { pttUtil.printLog(bDebug, LOG_TAG,
	 * "cancelNotification"); NotificationManager nm = (NotificationManager)
	 * getSystemService(NOTIFICATION_SERVICE); nm.cancel(R.string.app_name); }
	 */
	private void sendPTTBroadcast(boolean mBye) {
		pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>>sendPTTBroadcast ");
		Intent intent = new Intent(PTTConstant.ACTION_PTT);
		intent.putExtra(PTTConstant.KEY_PTT_MBYE, mBye);
		this.sendBroadcast(intent);
	}

	private void handleInit() {
		// if (PTTConstant.ACTION_INIT.equals(action)) {
		if (StateManager.getInitState() != PTTConstant.INIT_SUCCESS) {
			new InitThread(this).start();
		}
		// }
	}

	private synchronized void handleRegister(String action, Intent intent) {
		if (PTTConstant.ACTION_REGISTER.equals(action)) {
			boolean bForce = intent.getBooleanExtra(PTTConstant.KEY_REREGISTER_FORCE, false);
			StateManager.setbGrpRecived(false);
			new RegisterThread(this, bForce).start(); // !!important
			stopNotificationPicture();
			if (bForce) {
				if (pttManager.getCurrentPttState().getStatus() != PTTConstant.PTT_IDLE) {
					closePttUI();
					pttManager.clear();
				}
				if (callStateManager.getCallState() != PTTConstant.CALL_IDLE) {
					// if ring is playing, stop ringtone
					boolean bRingEnabled = prefs.getBoolean(getString(R.string.sp_ring_enable), true);
					boolean bVibrateEnabled = prefs.getBoolean(getString(R.string.sp_virabate_enable), true);
					if (bRingPlaying) {
						bRingPlaying = false;
						stopRingtone(bRingEnabled, bVibrateEnabled);
					}
					InCallScreenActivity.instance.saveCallLog();
					callStateManager.closeCallUI();
					callStateManager.clear();
				}
				groupManager.clear();
			}
		}
	}

	public void broadcastDebugInfo(String infoStr) {
		if (StateManager.debugFlag) {
			Intent intent = new Intent(PTTConstant.ACTION_PTT_STATUS_DEBUG);
			intent.putExtra(PTTConstant.KEY_STATUS_DEBUG_INFO, infoStr);
			sendBroadcast(intent);
		}
	}

	private void handleCommand(String action, Intent intent) {
		if (PTTConstant.ACTION_COMMAND.equals(action)) {
			int command = intent.getIntExtra(PTTConstant.KEY_SERVICE_COMMAND, -1);
			int result = PTTConstant.RETURN_SUCCESS;
			int iCallState = pttManager.getCallState();
			if (command == -1) {
				return;
			}
			if (StateManager.exitFlag)
				return;

			pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>handleCommand  command : " + pttUtil.getCommandString(command)
					+ "--" + command);

			switch (command) {
			case PTTConstant.COMM_APPLY_PTT_RIGHT:
				if (pttManager.isbPttErrorShowing()) {
					// do nothing
					return;
				}

				boolean bRegister = PTTUtil.getInstance().isRegistered();
				if (!bRegister) {
					pttUtil.errorAlert(this, 0, getString(R.string.register_not), true);
					return;
				} else {
					// check group info
					List<GroupInfo> groups = groupManager.getGroupData();
					if (groups == null || groups.size() == 0) {
						// no group message received
						broadcastDebugInfo("getting group no");
						pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_INIT);
						updatePttUI();
						result = pttManager.getGroupNo();
						if (result != PTTConstant.RETURN_SUCCESS) {
							pttUtil.errorAlert(this, result, getString(R.string.alert_msg_no_group), true);
						}
						return;
					} else {
						// group message receive and check if there is a group
						// no
						if (!checkGroupExist()) {
							pttUtil.errorAlert(this, 0, getString(R.string.alert_msg_no_group), true);
							return;
						}
					}
				}

				// added by lxd incase user pushed ptt key all the time
				pttManager.bPttAlertPlayed = false;

				pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_APPLYING);
				updatePttUI();

				pttUtil.printLog(bDebug, LOG_TAG,
						"applyPttRight" + pttUtil.getCallStateString(pttManager.getCallState()));

				result = pttManager.applyPttRight();
				broadcastDebugInfo("apply waiting for invite ");
				break;
			case PTTConstant.COMM_CANCEL_PTT_RIGHT:
				pttUtil.printLog(bDebug, LOG_TAG, "cancelPttRight");
				playPttAlert(PTTService.instance, PTTConstant.PTT_ALERT_RELEASE);
				String speaker = pttManager.getCurrentPttState().getSpeakerNum();
				if (null == speaker || speaker.trim().length() == 0)
					pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
				else {
					pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_LISTENING);
				}
				broadcastDebugInfo("cancling...");
				result = pttManager.cancelPttRight();
				broadcastDebugInfo("cancle result " + result);
				updatePttUI();
				break;
			case PTTConstant.COMM_HANGUP_PTT_CALL:
				pttUtil.printLog(bDebug, LOG_TAG, "COMM_HANGUP_PTT_CALL---->hangupPttCall "
						+ pttManager.getCurrentPttState().getCallId());
				pttUtil.printLog(bDebug, LOG_TAG, "callstate : " + callStateManager.getCurrentCallState()
						+ ", callstate in ptt : " + pttManager.getCallState());
				playPttAlert(this, PTTConstant.PTT_ALERT_RELEASE);
				pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
				pttManager.getCurrentPttState().setSpeakerNum(null);
				pttManager.setCallState(PTTConstant.CALL_ENDING);
				updatePttUI();
				if ((pttManager.getCallState() != PTTConstant.CALL_IDLE)
						&& (pttManager.getCallState() != PTTConstant.CALL_ENDED)) {
					broadcastDebugInfo("hangup ptt call");
					result = pttManager.hangupPttCall();
				}
				break;
			case PTTConstant.COMM_OPEN_PTT_UI:
				if (!pttManager.isPttUIOnTop() && (!pttManager.isbPttErrorShowing())) {
					openPttUI();
				}
				break;
			case PTTConstant.COMM_START_APP:
				Intent intent2 = new Intent(this, MainPageActivity.class);
				intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent2);
				break;
			case PTTConstant.COMM_UPDATE_PTT_UI:
				playPttAlert(this, PTTConstant.PTT_ALERT_RELEASE);
				if (!pttManager.isbPttErrorShowing()) {
					updatePttUI();
				}
				break;
			case PTTConstant.COMM_CLOSE_PTT:
				closePttUI();
				iCallState = pttManager.getCallState();
				switch (iCallState) {
				case PTTConstant.CALL_RINGING:
				case PTTConstant.CALL_TALKING:
				case PTTConstant.CALL_DIALING:
					pttManager.hangupPttCall();
					break;
				default:
					break;
				}
				pttManager.clear();
				break;
			case PTTConstant.COMM_MAKE_CALL:
				NumberInfo numberInfo = intent.getParcelableExtra("number_info");
				if (numberInfo == null) {
					return;
				}
				if (pttUtil.checkIsGroupNum(numberInfo.getNumber())) {
					numberInfo = null;
					pttUtil.errorAlert(instance, -1, getApplicationContext().getString(R.string.alert_msg_sc_grpnum),
							true);
					return;
				}

				callStateManager.getCurrentCallState().setCallState(PTTConstant.CALL_DIALING);
				callStateManager.getCurrentCallState().setNumber(numberInfo.getNumber());

				try {
					result = SipProxy.getInstance().RequestMakeCall(numberInfo);
				} catch (PTTException e) {
					result = -1;
				}
				if (result != PTTConstant.RETURN_SUCCESS) {
					callStateManager.clear();
				}
				break;
			case PTTConstant.COMM_SEND_DTMF:
				String digit = intent.getStringExtra(PTTConstant.KEY_DTMF);
				if (pttUtil.isStrBlank(digit)) {
					return;
				}
				SipProxy.getInstance().RequestSendDTMF(digit);
				break;
			case PTTConstant.COMM_ANSWER_CALL: {

				int callId = callStateManager.getCallId();
				boolean bRingEnabled = prefs.getBoolean(getString(R.string.sp_ring_enable), true);
				boolean bVibrateEnabled = prefs.getBoolean(getString(R.string.sp_virabate_enable), true);
				if (bRingPlaying) {
					stopRingtone(bRingEnabled, bVibrateEnabled);
				}

				if (callId < 0) {
					pttUtil.printLog(bDebug, LOG_TAG, "can not answer call " + callId);
					return;
				}
				try {
					result = SipProxy.getInstance().RequestAnswer(callId);
				} catch (PTTException e) {
					result = -1;
				}
				if (result != PTTConstant.RETURN_SUCCESS) {
					callStateManager.clear();
					if (callStateManager.isSingleCallOnTop()) {
						callStateManager.closeCallUI();
					}
				}
			}
				break;
			case PTTConstant.COMM_HANGUP_CALL: {
				boolean bRingEnabled = prefs.getBoolean(getString(R.string.sp_ring_enable), true);
				boolean bVibrateEnabled = prefs.getBoolean(getString(R.string.sp_virabate_enable), true);
				if (bRingPlaying) {
					stopRingtone(bRingEnabled, bVibrateEnabled);
				}
				int callId = callStateManager.getCallId();
				if (callId < 0) {
					pttUtil.printLog(bDebug, LOG_TAG, "can not hangup call " + callId);
					return;
				}

				try {
					result = SipProxy.getInstance().RequestHangup(callId);
				} catch (PTTException e) {
					result = -1;
				}
				if (result != PTTConstant.RETURN_SUCCESS) {
					callStateManager.clear();
					if (callStateManager.isSingleCallOnTop()) {
						callStateManager.closeCallUI();
					}
				}
			}
				break;
			default:
				break;
			}
		}
	}

	private void openSingleCallUI(Intent intent) {
		if (AlertActivity.instance != null) {
			AlertActivity.instance.finish();
		}

		if (StateManager.exitFlag)
			return;

		startActivity(intent);
	}

	private void updatePttUI() {
		synchronized (pttManager) {
			if (pttManager.isPttUIOnTop()) {
				sendPTTBroadcast(false);
			} else {
				openPttUI();
			}
		}
	}

	private boolean checkGroupExist() {
		List<GroupInfo> groupData = GroupManager.getInstance().getGroupData();
		pttUtil.printLog(bDebug, LOG_TAG, "groupData : " + groupData);
		if (groupData == null || groupData.size() == 0) {
			return false;
		}
		return true;
	}

	public synchronized void playPttAlert(Context context, final int pttAlertType) {

		pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>playPttAlert");
		
		if(PTTConstant.DUMMY) {
			int soundId = mSoundPool.load(context, R.raw.pttaccept, 1);
			int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			mSoundPool.play(soundId, 1, 1, 1, 0, 1);
			return;
		}

		switch (pttAlertType) {
		case PTTConstant.PTT_ALERT_ACCEPT:
			if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_UP) {
				// pttManager.bPttAlertPlayed = false;
				return;
			}
			mp = MediaPlayer.create(context, R.raw.pttaccept);
			break;
		case PTTConstant.PTT_ALERT_RELEASE:
			/*if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {
				// pttManager.bPttAlertPlayed = false;
				return;
			}*///by yjp 2013-4-1
			mp = MediaPlayer.create(context, R.raw.pttrelease);
			break;
		default:
			break;
		}

		mp.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if (mp != null && mp.isPlaying()) {
					mp.stop();
				}
				mp.release();
				int iPttStatus = pttManager.getCurrentPttState().getStatus();
				switch (iPttStatus) {
				case PTTConstant.PTT_LISTENING:
				case PTTConstant.PTT_WAITING:
				case PTTConstant.PTT_REJECTED:
					if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_UP
							&& !pttUtil.isStrBlank(pttManager.getCurrentPttState().getSpeakerNum())) {
						pttManager.speakerCtrl(true);
					}
					break;
				default:
					pttManager.speakerCtrl(false);
					break;
				}
			}
		});
		mp.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				pttUtil.printLog(bDebug, LOG_TAG, "MediaPlayer Error Occurs>>>>>>>>>>>>>>>>>>>>");
				mp.release();
				return true;
			}
		});

		if (audioManager == null) {
			audioManager = (AudioManager) PTTService.instance.getSystemService(Context.AUDIO_SERVICE);
		}

		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		pttUtil.printLog(bDebug, LOG_TAG,
				"11<><><><><><><> alert volume : " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
						+ ", ispeakerOn : " + audioManager.isSpeakerphoneOn());
                //by yjp 2013-4-2
        if(audioManager.isWiredHeadsetOn()){ 
	        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
			audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/2, 0);
        } else {
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
		}
        audioManager.setSpeakerphoneOn(true);
        audioManager.setMode(AudioManager.MODE_NORMAL);
                //end
		pttUtil.printLog(bDebug, LOG_TAG,
				"22<><><><><><><> alert volume : " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
						+ ", ispeakerOn : " + audioManager.isSpeakerphoneOn());

		if (mp != null || mp.isPlaying()) {
			mp.stop();
		}
		pttUtil.printLog(bDebug, LOG_TAG, "music volume : " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
				+ ",pttAlertType : " + pttAlertType + " pttManager.bPttAlertPlayed " + pttManager.bPttAlertPlayed);
		mp.setLooping(false);
		try {
			mp.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		switch (pttAlertType) {
		case PTTConstant.PTT_ALERT_ACCEPT:
			if (!pttManager.bPttAlertPlayed) {
				pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>>>>>>PTT_ALERT_ACCEPT  start play ");
				mp.start();
			}

			break;
		case PTTConstant.PTT_ALERT_RELEASE:
			// if (pttManager.bPttAlertPlayed)
			pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>>>>>>PTT_ALERT_RELEASE  start play ");
			mp.start();
			break;
		default:
			break;
		}
		pttManager.bPttAlertPlayed = (pttAlertType == PTTConstant.PTT_ALERT_ACCEPT);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		pttUtil.printLog(bDebug, LOG_TAG, "PTTService.onStart");
		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		pttUtil.printLog(bDebug, LOG_TAG, "PTTService.onDestroy");
		super.onDestroy();

		stopNotificationPicture();

		StateManager.reset();
		pttManager.clear();
		callStateManager.clear();
		groupManager.clear();

		if (innerReceiver != null) {
			unregisterReceiver(innerReceiver);
		}
		try {
			SipProxy.getInstance().RequestDestory();
		} catch (PTTException e) {
			Log.e(LOG_TAG, "Error to RequestDestory");
		}

		Intent intentTOHO = new Intent("com.zzy.action.start");
		sendBroadcast(intentTOHO);

		pttUtil.printLog(bDebug, LOG_TAG, "sip stack has destoryed ,close all ui");

		instance = null;
		sendBroadcast(new Intent(PTTConstant.ACTION_DEINIT));

	}

	/**
	 * method invoked by JNI, update the register state according the result
	 * passed by C, invoke setCurrentState and updateReigsterState
	 */
	public void JNIUpdateRegisterState(EnumLoginState currentState, int state, int errorCode) {
		pttUtil.printLog(bDebug, LOG_TAG, "JNIUpdateRegisterState Broadcast has sent message : " + state);
		Intent intent = new Intent();
		intent.putExtra(PTTConstant.KEY_REGISTER_STATE, state);
		if (state == PTTConstant.REG_ERROR) {
			intent.putExtra(PTTConstant.KEY_REGISTER_STATE, errorCode);
		}
		intent.setAction(PTTConstant.ACTION_REGISTER);
		sendBroadcast(intent);
		broadcastDebugInfo("reg state " + pttUtil.getRegStateString(state));

		switch (state) {
		case PTTConstant.REG_ERROR:
			// clear state
			if (pttManager.getCurrentPttState().getStatus() != PTTConstant.PTT_IDLE) {
				pttManager.hangupPttCall();
				closePttUI();
				pttManager.clear();
			}
			if (callStateManager.getCallState() != PTTConstant.CALL_IDLE) {
				Intent intent2 = new Intent(PTTConstant.ACTION_COMMAND);
				intent2.putExtra(PTTConstant.KEY_SERVICE_COMMAND, PTTConstant.COMM_HANGUP_CALL);
				handleCommand(PTTConstant.ACTION_COMMAND, intent2);
				callStateManager.closeCallUI();
				callStateManager.clear();
			}
			groupManager.clear();

			pttUtil.regErrorAlert(errorCode, getString(R.string.alert_msg_register_fail));
			break;
		case PTTConstant.REG_ED:
			pttUtil.printLog(bDebug, LOG_TAG, "JNIUpdateRegisterState: " + AlertActivity.instance);
			if (AlertActivity.instance != null || "com.zzy.ptt.ui.AlertActivity".equals(getTopActivity())) {
				AlertActivity.instance.finish();
			}
			AlertActivity.restartCount();
			// String ip = getLocalAddress();
			// pttUtil.printLog(bDebug, LOG_TAG, "<><><><><> get ip : " + ip);
			break;
		case PTTConstant.REG_NOT:
			// clear state
			if (pttManager.getCurrentPttState().getStatus() != PTTConstant.PTT_IDLE) {
				closePttUI();
				pttManager.clear();
			}
			if (callStateManager.getCallState() != PTTConstant.CALL_IDLE) {
				callStateManager.closeCallUI();
				callStateManager.clear();
			}
			groupManager.clear();
			pttUtil.regErrorAlert(state, getString(R.string.alert_msg_register_fail));
			break;
		default:
			break;
		}
	}

	/**
	 * Broadcast intent to InCallScreenActivity
	 * 
	 * @param callState
	 */
	public void JNIUpdateCallState(CallState callState) {
		pttUtil.printLog(bDebug, LOG_TAG, "JNIUpdateCallState " + callState.toString());
		if (callState.getCallState() == callStateManager.getFormerCallState()) {
			// ignore
			pttUtil.printLog(bDebug, LOG_TAG, "receive same status just do it");
			// return;
		}

		boolean bRingEnabled = prefs.getBoolean(getString(R.string.sp_ring_enable), true);
		boolean bVibrateEnabled = prefs.getBoolean(getString(R.string.sp_virabate_enable), true);

		callStateManager.setCurrentCallState(callState);

		pttUtil.printLog(bDebug, LOG_TAG, "JNIUpdateCallState CallManager" + callStateManager);

		Intent intent = new Intent(PTTConstant.ACTION_CALLSTATET);
		// intent.putExtra(PTTConstant.KEY_CALL_STATE, callState);
		int iCallState = callState.getCallState();

		switch (iCallState) {
		case PTTConstant.CALL_RINGING:
			// playringtone
			bRingPlaying = true;
			playRingtone(bRingEnabled, bVibrateEnabled);

			intent.setClass(getApplicationContext(), InCallScreenActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			openSingleCallUI(intent);

			break;
		case PTTConstant.CALL_DIALING:
			// just make sure we make single call by SipProxy.RequestMakeCall
			if (!callStateManager.isbForceInSC()) {
				break;
			}

			intent.setClass(getApplicationContext(), InCallScreenActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			openSingleCallUI(intent);

			callStateManager.setAR();
			callStateManager.setSCVolume(iCallState);
			break;
		case PTTConstant.CALL_TALKING:
			if (bRingPlaying) {
				bRingPlaying = false;
				stopRingtone(bRingEnabled, bVibrateEnabled);
			}

			if (InCallScreenActivity.instance == null) {
				intent.setClass(getApplicationContext(), InCallScreenActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				openSingleCallUI(intent);
				break;
			} else {
				sendBroadcast(intent);
			}
			callStateManager.setSCVolume(iCallState);
			break;
		case PTTConstant.CALL_ENDED:
			pttUtil.printLog(bDebug, LOG_TAG, "JNIUpdateCallState CALL_ENDED instance " + instance);
			if (bRingPlaying) {
				bRingPlaying = false;
				stopRingtone(bRingEnabled, bVibrateEnabled);
			}
			if (InCallScreenActivity.instance == null) {
				callState.setCallState(PTTConstant.CALL_IDLE);
				callStateManager.setCurrentCallState(callState);
			}
			sendBroadcast(intent);
			break;
		default:
			break;
		}
	}

	private void playRingtone(boolean ringEnable, boolean vibrateEnable) {
		String ringtoneFile = prefs.getString(getString(R.string.sp_current_ringtone), null);
		pttUtil.printLog(bDebug, LOG_TAG, " playRingtone , ringEnable : " + ringEnable + ", vibrateEnable : "
				+ vibrateEnable + ", callstate : " + callStateManager.getCurrentCallState());
		if (ringtoneFile == null) {
			return;
		}
		if (ringEnable) {
			String ringFile = prefs.getString(getString(R.string.sp_current_ringtone), null);
			if (ringFile == null || ringFile.length() == 0) {
				ringFile = PTTUtil.getInstance().getFirstRingName(getResources(), "call_");
			}
			AssetFileDescriptor fd = PTTUtil.getInstance().getFirstRingFD(getResources(), ringFile);
			if (fd != null) {
				try {
					if (ringPlayer == null) {
						ringPlayer = new MediaPlayer();
					}
					ringPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
					ringPlayer.prepare();

					ringPlayer.setOnCompletionListener(new OnCompletionListener() {

						@Override
						public void onCompletion(MediaPlayer mp) {
							pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>Ring Player Completed<<<<<<<");
						}
					});
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					return;
				} catch (IllegalStateException e) {
					e.printStackTrace();
					return;
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}

				if (ringPlayer != null) {
					// int maxSysVolume = audioManager
					// .getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
					int maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

					int volume = prefs.getInt(getString(R.string.sp_call_ring_volume), maxMusicVolume);
					// volume = (maxMusicVolume * volume / maxSysVolume);
					audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_PLAY_SOUND);
					audioManager.setSpeakerphoneOn(true);
					audioManager.setMode(AudioManager.MODE_NORMAL);
					ringPlayer.setLooping(true);
					ringPlayer.start();
					pttUtil.printLog(bDebug, LOG_TAG, "Player start play ringtone");
				}
			}
		}

		if (vibrateEnable) {
			vibrator.vibrate(new long[] { 1000, 2000, 3000, 4000 }, 0);
			pttUtil.printLog(bDebug, LOG_TAG, "Player start play vibrator");
		}
	}

	private void stopRingtone(boolean ringEnable, boolean vibrateEnable) {

		pttUtil.printLog(bDebug, LOG_TAG, "stopRingtone1");
		try {
			if (ringEnable && (ringPlayer != null) && ringPlayer.isPlaying()) {
				ringPlayer.stop();
				ringPlayer.release();
				ringPlayer = null;
				audioManager.setMode(AudioManager.MODE_NORMAL);
				audioManager.setSpeakerphoneOn(false);
				pttUtil.printLog(bDebug, LOG_TAG, "stopRingtone2");
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "Error occurs while stopRingtone" + e.getMessage());
		}

		if (vibrateEnable && (vibrator != null)) {
			vibrator.cancel();
		}

		// when stop ringtone, set Audio Route
		callStateManager.setAR();
	}

	private boolean isDeleted(String currentGrpNum, GroupInfo[] groupInfos) {
		int iLength = groupInfos.length;

		for (int i = 0; i < iLength; i++) {
			if (currentGrpNum.equals(groupInfos[i].getNumber())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent == null) {
			handleInit();
			return super.onStartCommand(intent, flags, startId);
		}

		String action = intent.getAction();
		if (action == null || action.trim().length() == 0) {
			handleInit();
			return super.onStartCommand(intent, flags, startId);
		}

		handleRegister(action, intent);

		handleCommand(action, intent);

		return super.onStartCommand(intent, flags, startId);
	}

	public void JNIAddGroupInfo(GroupInfo[] groupInfos) {
		Log.d(LOG_TAG, groupInfos + ", bDynamic : " + groupManager.isbDynamic());

		if (groupInfos == null || groupInfos.length == 0) {
			groupManager.getGroupData().clear();
			pttUtil.setCurrentGrp("");
			groupManager.getGroupNumMebersMap().clear();
		} else {
			groupManager.addGroups(groupInfos);
			GroupInfo currentGrpInfo = pttUtil.getCurrentGroupInfo();
			if (currentGrpInfo == null) {
				pttUtil.setCurrentGrp(groupInfos[0].getNumber());
			} else {
				if (isDeleted(currentGrpInfo.getNumber(), groupInfos)) {
					pttUtil.setCurrentGrp(groupInfos[0].getNumber());
				}
			}
		}
		StateManager.setbGrpRecived(true);

		GroupInfo currentGrpInfo = pttUtil.getCurrentGroupInfo();
		broadcastDebugInfo("group msg received");
		if (pttManager.getCurrentPttState().getStatus() == PTTConstant.PTT_INIT) {
			if (currentGrpInfo == null || pttUtil.isStrBlank(currentGrpInfo.getNumber())) {
				pttManager.clear();
				closePttUI();
				pttUtil.errorAlert(this, 0, getString(R.string.alert_msg_no_group), true);
			} else {
				pttManager.getCurrentPttState().setGroupNum(currentGrpInfo.getNumber());
				if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {
					Intent intent1 = new Intent(PTTConstant.ACTION_COMMAND);
					intent1.putExtra(PTTConstant.KEY_SERVICE_COMMAND, PTTConstant.COMM_APPLY_PTT_RIGHT);
					handleCommand(PTTConstant.ACTION_COMMAND, intent1);
				} else {
					pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
					updatePttUI();
				}
			}
		}

		if (groupManager.isbDynamic()) {

			Log.d(LOG_TAG, ">>>>>>>>>>Send Broadcast : " + PTTConstant.ACTION_DYNAMIC_REGRP);
			if (AlertActivity.instance != null) {
				AlertActivity.instance.finish();
			}

			int iCallState = callStateManager.getCallState();
			if (iCallState != PTTConstant.CALL_IDLE && (callStateManager.isSingleCallOnTop())) {
				// ignore for single call
			} else {
				int iPttStatus = pttManager.getCurrentPttState().getStatus();
				if (iPttStatus != PTTConstant.PTT_IDLE) {
					String pttGrpNum = pttManager.getCurrentPttState().getGroupNum();
					pttUtil.printLog(bDebug, LOG_TAG, "groupInfos " + groupInfos + " pttGrpNum " + pttGrpNum);
					if ((groupInfos == null || groupInfos.length == 0) || (isDeleted(pttGrpNum, groupInfos))) {
						pttUtil.errorAlert(this, -1,
								String.format(getString(R.string.alert_msg_deleted_from_ptt), pttGrpNum), false);
						if (iPttStatus == PTTConstant.PTT_SPEAKING && pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {
							pttManager.hangupPttCall();
						}
						closePttUI();
						pttManager.clear();
					}
				}
			}
			
			groupManager.getGroupNumMebersMap().clear();
			if (groupInfos != null && groupInfos.length > 0) {
				for (int i = 0; i < groupInfos.length; i++) {
					try {
						SipProxy.getInstance().RequestGetMember(groupInfos[i].getNumber());
					} catch (PTTException e) {
						e.printStackTrace();
					}
				}
			}
		}
		// update ui
		this.sendBroadcast(new Intent(PTTConstant.ACTION_DYNAMIC_REGRP));

		groupManager.setbDynamic(false);
	}

	public void JNIReceiveMessage(String number, String text) {

		pttUtil.printLog(bDebug, LOG_TAG, "JNIReceiveMessage  number : " + number);

		if (number == null || number.trim().length() == 0) {
			// receive dynamic regroup message
			groupManager.setbDynamic(true);
			isPttUIClosed = true;
		} else {
			Intent intent = new Intent();
			intent.setAction(PTTConstant.ACTION_NEW_MESSAGE);
			intent.putExtra(PTTConstant.KEY_MESSAGE_NUMBER, number);
			intent.putExtra(PTTConstant.KEY_MESSAGE_TEXT, text);
			this.sendBroadcast(intent);
		}
	}

	/**
	 * 
	 * @param status
	 *            only 200 is OK, otherwise show alert
	 */
	public void JNISendMessageResult(int status) {
		Intent intent = new Intent();
		intent.setAction(PTTConstant.ACTION_MESSAGE_RESULT);
		intent.putExtra(PTTConstant.KEY_MESSAGE_RESULT, status);
		this.sendBroadcast(intent);
	}

	public void JNIReceiveMemberInfo(String groupNum, MemberInfo[] members) {
		groupManager.addReceivedMembers(groupNum, members);
		if (groupManager.checkReceiveAll()) {
			Intent intent = new Intent();
			intent.setAction(PTTConstant.ACTION_MEMBERS_INFO);
			this.sendBroadcast(intent);
		} else {
			return;
		}
	}

	public void JNIUpdatePTTState(PttGroupStatus objPttGroupStatus) {
		pttUtil.printLog(bDebug, LOG_TAG, "JNIUpdatePTTState objPttGroupStatus : " + objPttGroupStatus);
		pttUtil.printLog(bDebug, LOG_TAG, " JNIUpdatePTTState currentPttState : " + pttManager.getCurrentPttState());
		pttUtil.printLog(bDebug, LOG_TAG, "call State : " + callStateManager.getCurrentCallState());
		pttUtil.printLog(bDebug, LOG_TAG, "call State isbForceInSC: " + callStateManager.isbForceInSC());
		pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>speaker : " + audioManager.isSpeakerphoneOn());

		// broadcastDebugInfo("update " +
		// pttUtil.getPttStatusString(objPttGroupStatus.getStatus()));

		synchronized (pttManager) {
			int iFormerPttStatus = pttManager.getCurrentPttState().getStatus();
			int iStatus = objPttGroupStatus.getStatus();
			int iCallState = callStateManager.getCallState();

			// !!important
			if (iStatus == PTTConstant.PTT_FREE) {

				if (!callStateManager.isSingleCallOnTop()
						|| (iCallState != PTTConstant.CALL_TALKING && iCallState != PTTConstant.CALL_RINGING&& iCallState != PTTConstant.CALL_DIALING)){
					pttManager.speakerCtrl(false);
				}

					switch (iFormerPttStatus) {
					case PTTConstant.PTT_APPLYING:
					case PTTConstant.PTT_WAITING:
					case PTTConstant.PTT_ACCEPTED:
					case PTTConstant.PTT_SPEAKING:
					case PTTConstant.PTT_REJECTED:
						if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {
							// do nothing
							// broadcastDebugInfo("ignore free in PTT_KEY_DOWN");
							pttUtil.printLog(bDebug, LOG_TAG,
									"^^^^^^when waiting or applying or speaking or accepted , ignore the PTT_FREE status");
							break;
						}
						// go through
					default:
						pttManager.setCurrentPttState(objPttGroupStatus);// !!important
						break;
					}
			} else {
				if (iStatus == PTTConstant.PTT_WAITING || iStatus == PTTConstant.PTT_REJECTED) {
					if (iFormerPttStatus == PTTConstant.PTT_SPEAKING
							&& pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {
						return;
					} else {
						// need to reset group num
						objPttGroupStatus.setGroupNum(pttManager.getCurrentPttState().getGroupNum());
						objPttGroupStatus.setSpeakerNum(pttManager.getCurrentPttState().getSpeakerNum());
						pttManager.setCurrentPttState(objPttGroupStatus);// !!important
					}
				}
			}

			iCallState = callStateManager.getCallState();
			// int iCallState = pttManager.getCallState();
			if (callStateManager.isbForceInSC() || callStateManager.isSingleCallOnTop()) {
				if (iStatus == PTTConstant.PTT_LISTENING) {
					if (callStateManager.isbForceInSC()) {
						String callNum = callStateManager.getCurrentCallState().getNumber();
						if (callNum == null) {
							return;
						}
						if (pttUtil.checkIsGroupNum(callNum)) {
							pttUtil.printLog(bDebug, LOG_TAG,
									"^^^^^^In single call, we dial the group number, so just ignore");
							return;
						}
					}
					sendPTTBroadcast(false);
				} else {
					pttUtil.printLog(bDebug, LOG_TAG, "^^^^^^In single call, we got m-Info, just ignore");
				}
				return;
			}

			String user = pttUtil.getSipUserFromPrefs(prefs).getUsername();
			if ((pttManager.getCallState() == PTTConstant.CALL_TALKING) && (iStatus != PTTConstant.PTT_SPEAKING)
					&& (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN)
					&& (user.equals(objPttGroupStatus.getSpeakerNum()))) {
				pttUtil.printLog(bDebug, LOG_TAG, "^^^^^^reset status to PTT_SPEAKING");
				pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_SPEAKING);
				pttManager.getCurrentPttState().setSpeakerNum(user);
			}

			iStatus = pttManager.getCurrentPttState().getStatus();
			if (iStatus == PTTConstant.PTT_LISTENING || iStatus == PTTConstant.PTT_SPEAKING) {
				if (user.equals(objPttGroupStatus.getSpeakerNum())) {
					if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_UP) {
						// find if any ptt call exist
						iCallState = pttManager.getCallState();
						if (iCallState == PTTConstant.CALL_TALKING) {
							pttManager.hangupPttCall();
						}
						pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
						pttManager.getCurrentPttState().setSpeakerNum(null);
					} else {
						pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_SPEAKING);
						playPttAlert(this, PTTConstant.PTT_ALERT_ACCEPT);
					}
					sendPTTBroadcast(false);
				}
			}

			Log.d(LOG_TAG, "<><><><><><>user :" + user + "speaker : " + objPttGroupStatus.getSpeakerNum());

			pttUtil.printLog(bDebug, LOG_TAG, "*********************** : " + pttManager.getCurrentPttState());

			switch (pttManager.ePttKeyStatus) {
			case PTT_KEY_DOWN:
				break;
			case PTT_KEY_UP:
				iStatus = pttManager.getCurrentPttState().getStatus();
				int result = -1;
				switch (iStatus) {
				case PTTConstant.PTT_SPEAKING:
					if (callStateManager.getCallId() == pttManager.getCurrentPttState().getCallId()
							&& (iCallState != PTTConstant.CALL_ENDED && iCallState != PTTConstant.CALL_IDLE))
						result = pttManager.hangupPttCall();
					if (result != PTTConstant.RETURN_SUCCESS) {
						pttUtil.printLog(bDebug, LOG_TAG, "error to hangupPttCall , result : " + result);
						// pttUtil.errorAlert(this, result,
						// "hangupPttCall : " + callStateManager.getCallId());
					}
					break;
				case PTTConstant.PTT_WAITING:
				case PTTConstant.PTT_APPLYING:
					result = pttManager.cancelPttRight();
					break;
				case PTTConstant.PTT_ACCEPTED:
					iCallState = pttManager.getCallState();
					if (iCallState == PTTConstant.CALL_IDLE || iCallState == PTTConstant.CALL_ENDED) {
						pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
						pttManager.getCurrentPttState().setSpeakerNum(null);
					}
					break;
				default:
					break;
				}
				break;
			default:
				break;
			}

			int iPttStatus = pttManager.getCurrentPttState().getStatus();
			switch (iPttStatus) {
			case PTTConstant.PTT_SPEAKING:
				audioManager.setSpeakerphoneOn(false);
				// audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
				// 0, 0);
				break;
			case PTTConstant.PTT_LISTENING:
				pttManager.adjustPttVolume(true);
				break;

			default:
				break;
			}

			if (!pttManager.isPttUIOnTop()
					&& (pttManager.getCurrentPttState().getStatus() == PTTConstant.PTT_LISTENING)) {
				openPttUI();
			} else {
				sendPTTBroadcast(false);
			}
		}
	}

	/**
	 * Receive mInivte
	 * 
	 * @param groupNum
	 */
	public void mInviteIndicate(String groupNum, String speaker) {

		pttUtil.printLog(bDebug, LOG_TAG, "mInviteIndicate speaker " + speaker + ", groupNum " + groupNum);

		// deal with the connection timeout
		if (groupNum == null) {
			pttUtil.printLog(bDebug, LOG_TAG,
					"<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>We got a timeout message, clear ptt state!!!");
			pttUtil.printLog(bDebug, LOG_TAG,
					"<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>We got a timeout message, clear ptt state!!!");
			pttUtil.printLog(bDebug, LOG_TAG,
					"<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>We got a timeout message, clear ptt state!!!");
			pttUtil.printLog(bDebug, LOG_TAG,
					"<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>We got a timeout message, clear ptt state!!!");
			pttUtil.printLog(bDebug, LOG_TAG,
					"<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>We got a timeout message, clear ptt state!!!");
			pttUtil.printLog(bDebug, LOG_TAG,
					"<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>We got a timeout message, clear ptt state!!!");
			pttUtil.printLog(bDebug, LOG_TAG,
					"<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>We got a timeout message, clear ptt state!!!");
			if (PTTActivity.instance != null) {
				PTTActivity.instance.finish();
			}

			pttManager.clear();
			StateManager.setRegStarter(EnumRegByWho.REG_BY_ERRORCODE);
			pttManager.setbPttErrorShowing(true);// error occurs
			pttUtil.errorAlert(this, PTTConstant.ERROR_CODE_504, "", false);

			return;
		}

		// check if single call exist
		int iSingleCallState = callStateManager.getCallState();
		pttUtil.printLog(bDebug, LOG_TAG,
				"------mInviteIndicate, iCallState : " + pttUtil.getCallStateString(iSingleCallState) + ",pttstate : "
						+ pttUtil.getPttStatusString(pttManager.getCurrentPttState().getStatus()));
		if (iSingleCallState != PTTConstant.CALL_ENDED && iSingleCallState != PTTConstant.CALL_IDLE) {
			if ((!callStateManager.isSingleCallOnTop()) && (!callStateManager.isbForceInSC())) {
				pttUtil.printLog(bDebug, LOG_TAG, "^^^^^^SingleCallState Error, reset");
				callStateManager.clear();
			} else {
				pttUtil.printLog(bDebug, LOG_TAG, "^^^^^^In single call, we got a PTT");
				if (!pttUtil.checkIsGroupNum(groupNum)) {
					// do nothing, ignore
					return;
				}
				if (speaker != null && speaker.trim().length() > 0) {
					pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_LISTENING);
					pttManager.getCurrentPttState().setSpeakerNum(speaker);
					pttManager.getCurrentPttState().setGroupNum(groupNum);
				}
				sendPTTBroadcast(false);
				return;
			}
		}

		String user = pttUtil.getCurrentUserName(this);
		int iPttStatus = pttManager.getCurrentPttState().getStatus();

		if (speaker == null || speaker.trim().length() == 0) {
			switch (iPttStatus) {
			case PTTConstant.PTT_ACCEPTED:
			case PTTConstant.PTT_APPLYING:
				return;
				
			case PTTConstant.PTT_LISTENING:
				pttManager.hangupPttCall();
				break;
			case PTTConstant.PTT_WAITING:
				pttManager.hangupPttCall();
				pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_APPLYING);//如果继续维持在PTT_WAITING 后续到来的mFree消息会挂断sip呼叫
				pttManager.getCurrentPttState().setSpeakerNum(null);
				return;
			default:
				break;
			}
			pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
			pttManager.getCurrentPttState().setSpeakerNum(null);
			JNIUpdatePTTState(pttManager.getCurrentPttState());
			pttManager.speakerCtrl(false);
			// return;
		} else {
			pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_LISTENING);
			pttManager.getCurrentPttState().setSpeakerNum(speaker);
			
			openPttUI();

			if (speaker.equals(user)) {
				pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_SPEAKING);
			} else {
				// open speaker
				pttManager.adjustPttVolume(true);

				switch (iPttStatus) {
				case PTTConstant.PTT_WAITING:
				case PTTConstant.PTT_APPLYING:
					if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {
						pttManager.getCurrentPttState().setStatus(iPttStatus);
						JNIUpdatePTTState(pttManager.getCurrentPttState());
						pttUtil.printLog(bDebug, LOG_TAG, "^^^^^mInviteIndicate in state "
								+ pttManager.getCurrentPttState().getStatus() + " ignore PTT_LISTENING");
						return;
					}
				case PTTConstant.PTT_REJECTED:
					if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {
						pttManager.getCurrentPttState().setStatus(iPttStatus);
						JNIUpdatePTTState(pttManager.getCurrentPttState());
						pttUtil.printLog(bDebug, LOG_TAG, "^^^^^mInviteIndicate in PTT_REJECTED ignore PTT_LISTENING");
						return;
					}
				}
				pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_LISTENING);
			}
			JNIUpdatePTTState(pttManager.getCurrentPttState());
			// return;
		}

		PttGroupStatus groupStatus = pttManager.getCurrentPttState();

		groupStatus.setGroupNum(groupNum);
		iPttStatus = pttManager.getCurrentPttState().getStatus();
		switch (iPttStatus) {
		case PTTConstant.PTT_IDLE:
			groupStatus.setSpeakerNum(speaker);
			if (user.equals(speaker)) {
				groupStatus.setStatus(PTTConstant.PTT_SPEAKING);
			} else {
				groupStatus.setStatus(PTTConstant.PTT_LISTENING);
			}
			// playPttAlert(this, PTTConstant.PTT_ALERT_ACCEPT);
			openPttUI();
			break;
		case PTTConstant.PTT_FREE:
		case PTTConstant.PTT_REJECTED:
		case PTTConstant.PTT_CANCELED:
		case PTTConstant.PTT_CANCELING:
			if (pttManager.isPttUIOnTop()) {
				pttUtil.printLog(bDebug, LOG_TAG, "jni call openPttUI, but it's already opened, so send broadcast");
				sendPTTBroadcast(false);
				return;
			} else {
				openPttUI();
			}
			break;
		default:
			break;
		}
		// adjustPttVolume();
	}

	/**
	 * Receive mBye
	 */
	public void mByeIndicate() {
		pttUtil.printLog(bDebug, LOG_TAG,
				"--------------------mByeIndicate,PTTActivity.instance : " + PTTActivity.instance + ", ptt status : "
						+ pttUtil.getPttStatusString(pttManager.getCurrentPttState().getStatus()));
		pttManager.clear();
		closePttUI();
		setPttUIClosed(true);
		isPttNotiClosed = true;

		int iCallState = callStateManager.getCallState();
		if (iCallState != PTTConstant.CALL_TALKING && iCallState != PTTConstant.CALL_RINGING && iCallState != PTTConstant.CALL_DIALING) {
			pttManager.speakerCtrl(false);
		}
	}

	public void JNIReceiveMemberStateChange(MemberInfo member) {
		boolean bNeedRegister = false;
		pttUtil.printLog(bDebug, LOG_TAG, "JNIReceiveMemberStateChange " + member);
		if (member == null) {
			// all terminals should start register in 5 seconds
			bNeedRegister = true;
			// sleep(random seconds < 5 seconds)
			try {
				long seconds = pttUtil.generateRandom(10);
				pttUtil.printLog(bDebug, LOG_TAG, "JNIReceiveMemberStateChange sleep " + seconds + " senconds");
				Thread.sleep(seconds);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			EnumLoginState loginState = StateManager.getCurrentRegState();
			int memberStatus = member.getStatus();
			String name = member.getName();
			String user = pttUtil.getSipUserFromPrefs(prefs).getUsername();
			if (user != null && user.trim().length() > 0) {
				if (user.equals(name)) {
					if ((memberStatus == PTTConstant.MEMBER_OFFLINE && loginState == EnumLoginState.REGISTERE_SUCCESS)
							|| (memberStatus == PTTConstant.MEMBER_ONLINE && loginState == EnumLoginState.UNREGISTERED)) {
						// do another register
						bNeedRegister = true;
					}
				} else {
					// TODO maybe we should update the member in
					// GroupManager here ,
					// so no need to get member status
				}
			}

		}

		if (bNeedRegister) {
			Intent intent = new Intent();
			intent.putExtra(PTTConstant.KEY_REREGISTER_FORCE, bNeedRegister);
			handleRegister(PTTConstant.ACTION_REGISTER, intent);
		}
	}

	public void openPttUI() {
		pttUtil.printLog(bDebug, LOG_TAG, ">>>>> open ptt ui isPttUIClosed " + isPttUIClosed);
		if (AlertActivity.instance != null) {
			AlertActivity.instance.finish();
		}
		if (pttManager.getCurrentPttState().getStatus() == PTTConstant.PTT_LISTENING) {
			pttManager.adjustPttVolume(true);
		}

		if (!isPttUIClosed()) {
			return;
		}

		if (StateManager.exitFlag)
			return;

		Intent intent = new Intent(this, PTTActivity.class);
		intent.setAction(PTTConstant.ACTION_PTT);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		this.startActivity(intent);

	}

	public void sendPttCommand(Intent intent) {
		sendBroadcast(intent);
		pttUtil.printLog(bDebug, LOG_TAG, "COMMAND has sent out");
	}

	public String getTopActivity() {
		ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);

		// get the info from the currently running task
		List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
		ComponentName componentName = taskInfo.get(0).topActivity;
		pttUtil.printLog(bDebug, LOG_TAG, "getTopActivity " + componentName.getClassName());
		return componentName.getClassName();
	}

	private void closePttUI() {
		sendPTTBroadcast(true);
		if (PTTActivity.instance != null) {
			PTTActivity.instance.finish();
		}
	}

	public boolean isPttUIClosed() {
		return isPttUIClosed;
	}

	public void setPttUIClosed(boolean isPttUIClosed) {
		this.isPttUIClosed = isPttUIClosed;
	}

	public void notificationPicture(int temp) {

		notificationPic.flags = Notification.FLAG_ONGOING_EVENT;
		notificationPic.flags |= Notification.FLAG_NO_CLEAR;

		notificationPic.icon = sNotificationImages[temp];
		notificationPic.setLatestEventInfo(instance, contentTitle, contentText, contentIntent);
		// notification.icon;
		manager.notify(1, notificationPic);
	}

	public void stopNotificationPicture() {

		manager.cancel(1);
	}

}
