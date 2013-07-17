/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : PTTActivity.java
 * Author : wangjunhui
 * Version : 1.0
 * Date : 2012-4-27
 */
package com.zzy.ptt.ui;

import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.model.PttGroupStatus;
import com.zzy.ptt.service.GroupManager;
import com.zzy.ptt.service.PTTManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author wangjunhui
 * 
 */
public class PTTActivity extends BaseActivity {

	public static PTTActivity instance;
	public static boolean bOnTop;
	private TextView ptt_status, ptt_group, ptt_speaker, ptt_group_name, ptt_info;
	private MyPTTReciver reciver;
	private PttGroupStatus pttGroupStatus;

	private PTTManager pttManager = PTTManager.getInstance();
	private PTTUtil pttUtil = PTTUtil.getInstance();
	private AudioManager audioManager = null;
	private SharedPreferences prefs = null;

	private Timer timer;
	private TimerTask task;
	private int duration = 0;

	private boolean debug = true;
	private static final String LOG_TAG = "PTTActivity";

	private Button pttKetButton;

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			int status;
			int result = duration % 4;
			String append = "", originalString = null;
			switch (result) {
			case 0:
				append = "";
				break;
			case 1:
				append = ".";
				break;
			case 2:
				append = "..";
				break;
			case 3:
				append = "...";
				break;
			default:
				break;
			}
			pttGroupStatus = pttManager.getCurrentPttState();// fixed by lxd
			if (pttGroupStatus == null) {
				return;
			}
			status = pttGroupStatus.getStatus();
			switch (status) {
			case PTTConstant.PTT_APPLYING:
			case PTTConstant.PTT_ACCEPTED:
				// case PTTConstant.PTT_INIT:
				originalString = getString(R.string.ptt_applying);
				break;
			case PTTConstant.PTT_SPEAKING:
			case PTTConstant.PTT_LISTENING:
				getPTTStatus(new Intent(PTTConstant.ACTION_PTT));
				cancelTimer();
				break;
			default:
				break;
			}
			if (originalString != null) {
				String statusLabel = getString(R.string.ptt_status);
				ptt_status.setText(statusLabel + originalString + append);
			}
		}
	};

	// private boolean bManual = false;

	// private Timer timer;
	// private MyTimerTask task;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// disable lockscreen
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ptt_layout1);

		instance = this;
		ptt_group = (TextView) findViewById(R.id.et_current_grp);
		ptt_speaker = (TextView) findViewById(R.id.et_ptt_talker);
		ptt_status = (TextView) findViewById(R.id.et_ptt_status);
		ptt_group_name = (TextView) findViewById(R.id.et_current_grp_name);
		pttKetButton = (Button) findViewById(R.id.btn_pttkey);
		ptt_info = (TextView) findViewById(R.id.et_ptt_debuginfo);

		// timer = new Timer();
		// task = new MyTimerTask();

		PTTUtil.getInstance().initOnCreat(this);

		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		pttUtil.printLog(debug, LOG_TAG, "OnCreate Over");

		pttKetButton.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				Intent intentPTTKeyDown = new Intent("android.intent.action.PTT.down");
				sendBroadcast(intentPTTKeyDown);
				return false;
			}
		});

		pttKetButton.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				// case MotionEvent.ACTION_DOWN:
				// Intent intentPTTKeyDown = new
				// Intent("android.intent.action.PTT.down");
				// sendBroadcast(intentPTTKeyDown);
				// break;
				case MotionEvent.ACTION_UP:
					Intent intentPTTKeyUp = new Intent("android.intent.action.PTT.up");
					sendBroadcast(intentPTTKeyUp);
					break;

				default:
					break;
				}
				return false;
			}
		});
	}

	class MyPTTReciver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(PTTConstant.ACTION_PTT)) {
				getPTTStatus(intent);
			}
			if (intent.getAction().equals(PTTConstant.ACTION_PTT_STATUS_DEBUG)) {
				ptt_info.setText(intent.getStringExtra(PTTConstant.KEY_STATUS_DEBUG_INFO));
			}
		}
	}

	public void getPTTStatus(Intent intent) {
		int status;
		String groupNum, speakerNum, groupTV, groupNameTV, speakerTV, statusTV, groupName = null, grp, grpn = null;
		if (intent == null) {
			return;
		}

		// Bundle bundle = intent.getExtras();
		pttGroupStatus = pttManager.getCurrentPttState();// fixed by lxd
		if (pttGroupStatus == null) {
			return;
		}
		pttUtil.printLog(debug, LOG_TAG, " onReceive ----> getPTTStatus : " + pttGroupStatus);
		if (intent.getBooleanExtra(PTTConstant.KEY_PTT_MBYE, false)) {
			this.finish();
			return;
		}

		StateManager.acquireWakeLock(getApplicationContext());

		status = pttGroupStatus.getStatus();
		groupNum = pttGroupStatus.getGroupNum();
		
		if (groupNum != null && groupNum.trim().length() > 0)
			groupName = GroupManager.getInstance().getGroupInfo(groupNum).getName();
		
		speakerNum = pttGroupStatus.getSpeakerNum();
		groupTV = getApplicationContext().getString(R.string.current_grp);
		groupNameTV = getApplicationContext().getString(R.string.current_grp_name);		
		speakerTV = getApplicationContext().getString(R.string.ptt_talker);
		statusTV = getApplicationContext().getString(R.string.ptt_status);
		if (speakerNum == null || speakerNum.trim().length() == 0) {
			speakerNum = getString(R.string.ptt_nospeaker);
		}
		grp = generateGrp(groupNum);
		grpn = generateGrpName(groupName);
		switch (status) {
		case PTTConstant.PTT_CLOSED:
		case PTTConstant.PTT_IDLE:
			grp=PreferenceManager.getDefaultSharedPreferences(this).getString(PTTConstant.SP_CURR_GRP_NUM, "");
			if (grp != null && grp.trim().length() > 0)
				grpn = GroupManager.getInstance().getGroupInfo(grp).getName();
			ptt_group.setText(groupTV
					+grp );			
			ptt_group_name.setText(groupNameTV + grpn);
			ptt_speaker.setText(speakerTV + getString(R.string.ptt_nospeaker));
			ptt_status.setText(statusTV + getApplicationContext().getString(R.string.ptt_free));
			break;
		case PTTConstant.PTT_CANCELING:
			ptt_group.setText(groupTV + grp);
			ptt_group_name.setText(groupNameTV + grpn);
			ptt_speaker.setText(speakerTV + speakerNum);
			ptt_status.setText(statusTV + getApplicationContext().getString(R.string.ptt_free));
			break;
		case PTTConstant.PTT_INIT:
			ptt_group.setText(groupTV + getApplicationContext().getString(R.string.ptt_getting));

			ptt_speaker.setText(speakerTV + speakerNum);
			ptt_status.setText(statusTV + getApplicationContext().getString(R.string.ptt_getting_grp));
			break;
		case PTTConstant.PTT_FREE:
			ptt_group.setText(groupTV + grp);
			ptt_group_name.setText(groupNameTV + grpn);
			ptt_speaker.setText(speakerTV + getString(R.string.ptt_nospeaker));
			ptt_status.setText(statusTV + getApplicationContext().getString(R.string.ptt_free));
			break;
		case PTTConstant.PTT_LISTENING:
			ptt_group.setText(groupTV + grp);
			ptt_group_name.setText(groupNameTV + grpn);
			ptt_speaker.setText(speakerTV + speakerNum);
			ptt_status.setText(statusTV + getApplicationContext().getString(R.string.ptt_listening));
			break;
		case PTTConstant.PTT_SPEAKING:
			ptt_group.setText(groupTV + grp);
			ptt_group_name.setText(groupNameTV + grpn);
			ptt_speaker.setText(speakerTV + speakerNum + "<" + getString(R.string.ptt_myself) + ">");
			ptt_status.setText(statusTV + getApplicationContext().getString(R.string.ptt_speaking));
			break;
		case PTTConstant.PTT_WAITING:
			ptt_group.setText(groupTV + grp);
			ptt_group_name.setText(groupNameTV + grpn);
			ptt_speaker.setText(speakerTV + speakerNum);
			// ptt_status.setText(statusTV +
			// getApplicationContext().getString(R.string.ptt_witing));
			ptt_status.setText(statusTV + buildWaitingMsg(pttGroupStatus));
			break;
		case PTTConstant.PTT_APPLYING:
			ptt_group.setText(groupTV + grp);
			ptt_group_name.setText(groupNameTV + grpn);
			ptt_speaker.setText(speakerTV + speakerNum);
			ptt_status.setText(statusTV + getString(R.string.ptt_applying));
			break;
		case PTTConstant.PTT_ACCEPTED:
			ptt_group.setText(groupTV + grp);
			ptt_group_name.setText(groupNameTV + grpn);
			ptt_speaker.setText(speakerTV + speakerNum);
			ptt_status.setText(statusTV + getString(R.string.ptt_applying));
			break;
		case PTTConstant.PTT_REJECTED:
			ptt_group.setText(groupTV + grp);
			ptt_group_name.setText(groupNameTV + grpn);
			ptt_speaker.setText(speakerTV + speakerNum);
//			ptt_status.setText(statusTV + getString(R.string.ptt_applying));
			ptt_status.setText(statusTV + buildRejectMsg(pttGroupStatus));
			ptt_info.setText("PTT_REJECTED " + pttGroupStatus.getRejectReason());
			break;
		case PTTConstant.PTT_CANCELED:
			ptt_group.setText(groupTV + grp);
			ptt_group_name.setText(groupNameTV + grpn);
			ptt_speaker.setText(speakerTV + speakerNum);
			ptt_status.setText(statusTV + getString(R.string.ptt_applying));
			break;
		default:
			break;
		}

		// update timer
		switch (status) {
		case PTTConstant.PTT_APPLYING:
		case PTTConstant.PTT_INIT:
		case PTTConstant.PTT_ACCEPTED:
			dynamicShowMsg();
			break;
		default:
			cancelTimer();
			break;
		}
	}

	private String generateGrp(String grpNum) {

		if (grpNum == null || grpNum.trim().length() == 0) {
			return "";
		} else {
			return grpNum;
		}
	}

	private String generateGrpName(String grpName) {
		if (grpName == null || grpName.trim().length() == 0) {
			return "";
		} else {
			return grpName;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case PTTConstant.KEYCODE_PTT:
			return false;
		case PTTConstant.KEYCODE_W_ENDCALL:
		case KeyEvent.KEYCODE_BACK:
		case KeyEvent.KEYCODE_ENDCALL:
			PTTActivity.this.finish();
			// exit this activity
			int iPttStatus = pttManager.getCurrentPttState().getStatus();
			switch (iPttStatus) {
			case PTTConstant.PTT_APPLYING:
				Intent intent = new Intent(PTTConstant.ACTION_COMMAND);
				// intent.setClass(this, PTTService.class);
				intent.putExtra(PTTConstant.KEY_SERVICE_COMMAND, PTTConstant.COMM_CANCEL_PTT_RIGHT);
				// startService(intent);
				PTTService.instance.sendPttCommand(intent);
				// go through
			case PTTConstant.PTT_FREE:
			case PTTConstant.PTT_INIT:
			case PTTConstant.PTT_ACCEPTED:
			case PTTConstant.PTT_LISTENING:
			case PTTConstant.PTT_CANCELING:
			case PTTConstant.PTT_CANCELED:
				PTTActivity.this.finish();
				PTTService.instance.setPttUIClosed(false);
				// pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_BACKGROUND);//
				// FIXME
				break;
			default:
				break;
			}
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
			adjustVolume(keyCode);
			return true;
		default:
			return true;
		}
	}

	private void adjustVolume(int keyCode) {

		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE,
					AudioManager.FLAG_SHOW_UI);
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER,
					AudioManager.FLAG_SHOW_UI);
		}
		Editor editor = prefs.edit();
		editor.putInt(getString(R.string.sp_call_ptt_volume),
				audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
		editor.commit();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		pttUtil.printLog(debug, LOG_TAG, "&&&&&&&&&&&&&&&onDestroy");
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (reciver == null) {
			reciver = new MyPTTReciver();
			IntentFilter filter = new IntentFilter();
			filter.addAction(PTTConstant.ACTION_PTT);
			this.registerReceiver(reciver, filter);
		}
		instance = this;
		StateManager.acquireWakeLock(getApplicationContext());
		pttUtil.printLog(debug, LOG_TAG, " onResume : " + pttManager.getCurrentPttState());
		getPTTStatus(new Intent(PTTConstant.ACTION_PTT));
		pttUtil.printLog(debug, LOG_TAG, "&&&&&&&&&&&&&&&onResume instance : " + instance);
	}

	@Override
	protected void onPause() {
		super.onPause();
		StateManager.releaseWakeLock();
		try {
			unregisterReceiver(reciver);
			reciver = null;
		} catch (Exception e) {
			pttUtil.printLog(debug, LOG_TAG, "exception occured while unregisterReciver, ignore it");
		}

		cancelTimer();
		instance = null;
		pttUtil.printLog(debug, LOG_TAG, "&&&&&&&&&&&&&&&onPause");
	}

	private void dynamicShowMsg() {
		if (instance == null || !pttManager.isPttUIOnTop()) {
			cancelTimer();
		}

		if (timer == null) {
			timer = new Timer();
		}
		if (task == null) {
			task = new TimerTask() {

				@Override
				public void run() {
					++duration;
					handler.sendEmptyMessage(0);
				}
			};
			timer.schedule(task, 0, 1000);
		}
	}

	private void cancelTimer() {
		// cancel timer
		pttUtil.printLog(debug, LOG_TAG, "&&&&&&&&&&&&&&&cancelTimer");
		if (timer != null) {
			timer.cancel();
			timer = null;
			task.cancel();
			task = null;
			duration = 0;
		}
	}

	private String buildWaitingMsg(PttGroupStatus pttGroupStatus) {
		StringBuilder sb = new StringBuilder();
		sb.append(getApplicationContext().getString(R.string.ptt_waiting));
		if (pttGroupStatus.getTotalWaitingCount() <= 0) {
			return sb.toString();
		} else {
			sb.append(" ").append(pttGroupStatus.getCurrentWaitingPos()).append("/")
					.append(pttGroupStatus.getTotalWaitingCount());
			return sb.toString();
		}
	}
	
	private String buildRejectMsg(PttGroupStatus pttGroupStatus) {
		StringBuilder sb = new StringBuilder();
		int rejectReason = pttGroupStatus.getRejectReason();
		if (pttGroupStatus.getRejectReason() <= 0) {
			sb.append(getApplicationContext().getString(R.string.ptt_rejected));
			return sb.toString();
		} else {
			String s = null;
			switch (rejectReason) {
			case 1:
				s = getApplicationContext().getString(R.string.alert_msg_no_ptt);
				break;
			case 2:
				s = getApplicationContext().getString(R.string.ptt_waiting_full);
				break;
			case 3:
				s = getApplicationContext().getString(R.string.ptt_waiting_droped);
				break;
			case 4:
				s = getApplicationContext().getString(R.string.ptt_waiting_timeout);
				break;
			default:
				s = getApplicationContext().getString(R.string.ptt_rejected);
				break;
			}
			sb.append(s);
			return sb.toString();
		}
	}
	@Override
	/**
	 * disable KEYCODE_HOME
	 */
	public void onAttachedToWindow() {
		this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
	}

}
