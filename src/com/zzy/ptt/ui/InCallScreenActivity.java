/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project zzy PTT V1.0
 * Name InCallScreenActivity.java
 * Description InCallScreenActivity displays IncomingCall Screen , Talking Screen, Outing Screen
 * Author LiXiaodong
 * Version 1.0
 * Date 2012-3-12
 */

package com.zzy.ptt.ui;

import java.sql.Timestamp;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.zzy.ptt.R;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.CallLog;
import com.zzy.ptt.model.CallState;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.service.CallStateManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class InCallScreenActivity extends BaseActivity {

	private TextView tvCallType, tvNumber, tvDuration;

	private long duration = 0;
	private Timer timer;
	private TimerTask task;
	private CallLog callLog;

	private boolean bMoFlag = false;

	private static final String LOG_TAG = "InCallScreenActivity";

	private static final int MSG_UPDATE_DURATION = 0;
	private static final int MSG_UPDATE_CALLSTATE = 1;

	private AudioManager audioManager;
	private SharedPreferences prefs;

	private CallStateManager callStateManager;

	public static InCallScreenActivity instance = null;

	private static boolean bDebug = true;
	private PTTUtil pttUtil = PTTUtil.getInstance();

	private AlertDialog pttAlertDialog;
	// add by wangjunhui
	private AnimationDrawable animDance;
	private ImageView imgDance;

	private boolean bMenuShown = false;
	private Menu menu;
	
	private Button answerBtn,soundBtn,endCallBtn;
	private boolean isMyHangUP = false;
	private boolean isOtherHangUP = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// disable lockscreen
		int flag = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
		flag |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
		// flag |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		getWindow().addFlags(flag);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.single_call_page);

		pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>>>InCallScreenActivity onCreate22 ");
		instance = this;

		pttUtil.initOnCreat(this);
		callStateManager = CallStateManager.getInstance();

		tvCallType = (TextView) findViewById(R.id.id_tv_calltype);
		tvNumber = (TextView) findViewById(R.id.id_tv_number);
		tvDuration = (TextView) findViewById(R.id.id_tv_duration);
		// add by wangjunhui
		imgDance = (ImageView) findViewById(R.id.id_image_incallscreen);
		animDance = (AnimationDrawable) imgDance.getBackground();
		
		answerBtn = (Button) findViewById(R.id.btn_answer_call);
		soundBtn = (Button) findViewById(R.id.btn_sound_call);
		endCallBtn = (Button) findViewById(R.id.btn_end_call);

		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		showCallState(callStateManager.getCallState());
		showCallNumber(callStateManager.getCurrentCallState());
		registerReceiverAction();

		initCallLog();
		
		if (callStateManager.getCallState() == PTTConstant.CALL_RINGING) {
			answerBtn.setVisibility(View.VISIBLE);
		}else{
			answerBtn.setVisibility(View.INVISIBLE);
		}
		
		answerBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				answer();
				
			}
		});
		soundBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				changeAudioRoute();
				if (callStateManager.currentAudioRoute == PTTConstant.AR_HANDSET) {
					soundBtn.setText(getString(R.string.menu_handset));
				} else {
					soundBtn.setText(getString(R.string.menu_speaker));
				}
			}
		});
		endCallBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				hangup();
			}
		});
	}

	private void initCallLog() {
		callLog = new CallLog();
		callLog.setTime(new Timestamp(System.currentTimeMillis()));
		int iCallState = callStateManager.getCallState();
		if (iCallState == PTTConstant.CALL_DIALING) {
			callLog.setLogType(PTTConstant.LOG_OUT);
		} else if (iCallState == PTTConstant.CALL_RINGING) {
			callLog.setLogType(PTTConstant.LOG_ANSWERED);
		}
	}

	private void setAR() {
		// boolean handsetFlag =
		// prefs.getBoolean(getString(R.string.sp_ar_handset), true);
		if (callStateManager.currentAudioRoute == PTTConstant.AR_HANDSET) {
			audioManager.setSpeakerphoneOn(false);
			audioManager.setMode(AudioManager.MODE_IN_CALL);
		} else {
			audioManager.setSpeakerphoneOn(true);
			audioManager.setMode(AudioManager.MODE_NORMAL);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		pttUtil.printLog(bDebug, LOG_TAG, "*****onActivityResult*****");
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onResume() {
		super.onResume();
		StateManager.acquireWakeLock(instance);
		int iCallState = callStateManager.getCallState();
		pttUtil.printLog(bDebug, LOG_TAG, "onResume : iCallState " + pttUtil.getCallStateString(iCallState));

		switch (iCallState) {
		case PTTConstant.CALL_RINGING:
			bMoFlag = false;
			break;
		case PTTConstant.CALL_DIALING:
			bMoFlag = true;
			setAR();
			break;
		case PTTConstant.CALL_ENDED:
			if (callStateManager.getFormerCallState() == PTTConstant.CALL_TALKING
					|| callStateManager.getFormerCallState() == PTTConstant.CALL_DIALING
					|| callStateManager.getFormerCallState() == PTTConstant.CALL_RINGING) {
				if (!isMyHangUP) {
					isOtherHangUP = true;
				}
			}
		case PTTConstant.CALL_IDLE:
			closeCallUI();
			break;
		case PTTConstant.CALL_TALKING:
			updateCallState(PTTConstant.CALL_TALKING);
			break;
		default:
			break;
		}
	}

	private synchronized void updateTimer() {

		pttUtil.printLog(bDebug, LOG_TAG, "updateTimer, timer:" + timer + ", task : " + task);
		if (timer == null) {
			timer = new Timer();
		}

		if (task == null) {
			task = new TimerTask() {

				@Override
				public void run() {
					++duration;
					handler.sendEmptyMessage(MSG_UPDATE_DURATION);
				}
			};
			timer.schedule(task, 0, 1000);
		}
	}

	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MSG_UPDATE_DURATION:
				tvDuration.setText(PTTUtil.getInstance().duriation2String(duration));
				break;
			case MSG_UPDATE_CALLSTATE:
				updateCallState(callStateManager.getCallState());
			default:
				break;
			}
		}
	};

	private BroadcastReceiver callReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			pttUtil.printLog(bDebug, LOG_TAG, "callReceiver receive broadcast, action : " + action + ",callstate : "
					+ callStateManager.getCurrentCallState());
			if (action.equals(PTTConstant.ACTION_CALLSTATET)) {
				// CallState callState =
				// intent.getParcelableExtra(PTTConstant.KEY_CALL_STATE);
				CallState callState = callStateManager.getCurrentCallState();
				if (callState.getErrorCode() > PTTConstant.ERROR_CODE_MINI) {
					// pttUtil.errorToast(InCallScreenActivity.this,
					// callState.getErrorCode(), "");
				}

				int iCallState = callState.getCallState();

				if (iCallState == PTTConstant.CALL_TALKING) {
					setAR();
				}

				updateCallState(callState.getCallState());

			} else if (action.equals(PTTConstant.ACTION_PTT)) {

				// just incase
				if (instance == null || callStateManager.getCallState() == PTTConstant.CALL_IDLE) {
					// just ignore
					pttUtil.printLog(bDebug, LOG_TAG, "InCallScreen has closed, ignore the");
					return;
				}

				boolean mBye = intent.getBooleanExtra(PTTConstant.KEY_PTT_MBYE, false);
				if (mBye) {
					if (pttAlertDialog != null && pttAlertDialog.isShowing()) {
						pttAlertDialog.dismiss();
					}
					return;
				} else {
					if (pttAlertDialog != null && pttAlertDialog.isShowing()) {
						// do nothing
						return;
					}
				}

				// alert user
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setTitle(context.getString(R.string.alert_title));
				builder.setMessage(getString(R.string.alert_msg_receive_ptt));
				builder.setPositiveButton(R.string.alert_btn_ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						PTTService.instance.isPttUIClosed = true;
						dialog.dismiss();
						pttUtil.printLog(bDebug, LOG_TAG, "InCallScreenActivity finish manually");
						InCallScreenActivity.this.finish();
						// go to ptt
						callStateManager.setCallState(PTTConstant.CALL_ENDED);
						try {
							SipProxy.getInstance().RequestOpenPtt();
						} catch (PTTException e) {
							pttUtil.printLog(bDebug, LOG_TAG, "Error RequestOpenPtt");
							return;
						}
						// manually open ptt ui
						pttUtil.printLog(bDebug, LOG_TAG, "Open PTT UI manually");
						Intent intent = new Intent(PTTConstant.ACTION_COMMAND);
						// intent.setClass(InCallScreenActivity.this,
						// PTTService.class);
						intent.putExtra(PTTConstant.KEY_SERVICE_COMMAND, PTTConstant.COMM_OPEN_PTT_UI);
						// startService(intent);
						PTTService.instance.sendPttCommand(intent);
					}
				});
				builder.setNegativeButton(R.string.alert_btn_no, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						// maybe should do something
					}
				});
				pttAlertDialog = builder.create();
				pttAlertDialog.show();
			} else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
				int state = intent.getIntExtra("state", 0);
				if (bMenuShown && (callStateManager.getCallState() != PTTConstant.CALL_RINGING)) {
					if (state == 1) {
						menu.getItem(0).setTitle(getString(R.string.menu_earphone));
					} else {
						menu.getItem(0).setTitle(getString(R.string.menu_handset));
					}
				}
			}
		}
	};

	private void registerReceiverAction() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(PTTConstant.ACTION_CALLSTATET);
		filter.addAction(PTTConstant.ACTION_PTT);
		filter.addAction(Intent.ACTION_HEADSET_PLUG);
		this.registerReceiver(callReceiver, filter);
	}

	private void showCallState(int callState) {
		// add by wangjunhui
		imgDance.post(new Runnable() {

			@Override
			public void run() {
				animDance.start();
			}
		});
		switch (callState) {
		case PTTConstant.CALL_DIALING:
		case PTTConstant.CALL_RINGBACK:
			tvCallType.setText(getString(R.string.call_dialing));
			// add by wangjunhui
			imgDance.setBackgroundResource(R.drawable.callout_anim);
			animDance = (AnimationDrawable) imgDance.getBackground();
			break;
		case PTTConstant.CALL_RINGING:
			tvCallType.setText(getString(R.string.call_incoming));
			// add by wangjunhui
			imgDance.setBackgroundResource(R.drawable.callin_anim);
			animDance = (AnimationDrawable) imgDance.getBackground();
			break;
		case PTTConstant.CALL_TALKING:
			tvCallType.setText(getString(R.string.call_single));
			// add by wangjunhui
			imgDance.setBackgroundResource(R.drawable.calling_anim);
			animDance = (AnimationDrawable) imgDance.getBackground();

			break;
		case PTTConstant.CALL_ENDING:
			tvCallType.setText(getString(R.string.call_ending));
			// add by wangjunhui
			imgDance.setBackgroundResource(R.drawable.gd);
			break;
		case PTTConstant.CALL_ENDED:
			tvCallType.setText(getString(R.string.call_ended));
			// add by wangjunhui
			imgDance.setBackgroundResource(R.drawable.gd);
			break;
		default:
			break;
		}
	}

	private void hangup() {
		final int iCallId = callStateManager.getCallId();
		pttUtil.printLog(bDebug, LOG_TAG, "--------hangup : " + callStateManager.getCurrentCallState()
				+ ", instance : " + instance);

		int callstate = callStateManager.getCallState();
		if (iCallId < 0) {
			// pttUtil.errorAlert(this, iCallId, "CallId Error");
			return;
		}
		if (callstate == PTTConstant.CALL_IDLE || callstate == PTTConstant.CALL_ENDED
				|| callStateManager.getCurrentCallState().getNumber() == null) {
			if (instance != null) {
				closeCallUI();
			} else {
				this.finish();
			}

			return;
		}

		switch (callstate) {
		case PTTConstant.CALL_DIALING:
			callstate = PTTConstant.CALL_CANCELING;
			break;
		case PTTConstant.CALL_TALKING:
			callstate = PTTConstant.CALL_ENDING;
			cancelTimer();
			break;
		case PTTConstant.CALL_RINGING:
			callstate = PTTConstant.CALL_REJECTING;
			break;
		default:
			break;
		}
		showCallState(callstate);

		callStateManager.setCallState(callstate);
		callStateManager.setbForceInSC(false);

		isMyHangUP = true;

		closeCallUI();

		Intent intent = new Intent(PTTConstant.ACTION_COMMAND);
		intent.putExtra(PTTConstant.KEY_SERVICE_COMMAND, PTTConstant.COMM_HANGUP_CALL);
		intent.setClass(this, PTTService.class);
		startService(intent);
	}

	protected void updateCallState(int callState) {
		pttUtil.printLog(bDebug, LOG_TAG, ">>>updateCallState");
		showCallState(callState);
		if (callState == PTTConstant.CALL_TALKING) {
			updateTimer();
		}
		if (callState == PTTConstant.CALL_ENDED) {
			cancelTimer();
			if (callStateManager.getFormerCallState() == PTTConstant.CALL_TALKING
					|| callStateManager.getFormerCallState() == PTTConstant.CALL_DIALING
					|| callStateManager.getFormerCallState() == PTTConstant.CALL_RINGING) {
				if (!isMyHangUP) {
					isOtherHangUP = true;
				}
			}
			closeCallUI();
		}
	}

	private void showCallNumber(CallState tempCallState) {
		tvNumber.setText(tempCallState.getNumber());
	}

	@Override
	protected void onDestroy() {
		int iCallState = callStateManager.getCallState();
		if (iCallState == PTTConstant.CALL_RINGING || iCallState == PTTConstant.CALL_TALKING
				|| iCallState == PTTConstant.CALL_DIALING) {
			pttUtil.printLog(bDebug, LOG_TAG, "ignore  onDestroy");
		} else {
			super.onDestroy();
			pttUtil.printLog(bDebug, LOG_TAG, ">>>onDestroy");
			instance = null;
			callStateManager.clear();
			if (callReceiver != null)
				try {
					this.unregisterReceiver(callReceiver);
				} catch (Exception e) {
					pttUtil.printLog(bDebug, LOG_TAG, "Caught exception ,just ignore");
				}
			resetAudioRoute();
			StateManager.releaseWakeLock();
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		pttUtil.printLog(bDebug, LOG_TAG, ">>>onPause");
		super.onPause();
		if (animDance != null) {
			animDance.stop();
		}
		if (pttAlertDialog != null && pttAlertDialog.isShowing()) {
			pttAlertDialog.dismiss();
		}
	}
	
	@Override
	protected void onStop() {
		pttUtil.printLog(bDebug, LOG_TAG, ">>>onStop");
		StateManager.releaseWakeLock();
		isMyHangUP = false;
		isOtherHangUP = false;
		super.onStop();
	}

	public void saveCallLog() {
		
		
		pttUtil.printLog(bDebug, LOG_TAG, ">>>saveCallLog<<<");

		int formerCallState = callStateManager.getFormerCallState();
		if (formerCallState == PTTConstant.CALL_DIALING || formerCallState == PTTConstant.CALL_CANCELING) {
			callLog.setLogType(PTTConstant.LOG_OUT);
		} else if (formerCallState == PTTConstant.CALL_RINGING || formerCallState == PTTConstant.CALL_REJECTING) {
			callLog.setLogType(PTTConstant.LOG_MISSING);
		} else if (formerCallState == PTTConstant.CALL_TALKING || formerCallState == PTTConstant.CALL_ENDING) {
			if (bMoFlag) {
				callLog.setLogType(PTTConstant.LOG_OUT);
			} else {
				callLog.setLogType(PTTConstant.LOG_ANSWERED);
			}
		}

		if (duration > 0) {
			callLog.setDuration(duration);
		}
		
		if (isMyHangUP) {
			callLog.setReason(getString(R.string.log_reason_1));
		}else if (isOtherHangUP) {
			callLog.setReason(getString(R.string.log_reason_2));
		}else{
			callLog.setReason(getString(R.string.log_reason_3));
		}

		callLog.setCallId(callStateManager.getCallId());
		callLog.setNum(callStateManager.getCurrentCallState().getNumber());
		pttUtil.printLog(bDebug, LOG_TAG, ">>>>saveCallLog" + callLog.toString());
		callStateManager.saveCallLog(callLog);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
		case PTTConstant.KEYCODE_W_ENDCALL:
			hangup();
			return true;
		case KeyEvent.KEYCODE_MENU:
			return false;
		case PTTConstant.KEYCODE_W_CALL:
		case KeyEvent.KEYCODE_CALL:
			if (callStateManager.getCallState() == PTTConstant.CALL_RINGING) {
				answer();
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
//	@Override  
//	public boolean dispatchKeyEvent(KeyEvent event) {  
//
//       if(event.getAction() == KeyEvent.ACTION_DOWN)
//       {
//            if(event.getRepeatCount() == 0) 
//            {  
//            	Log.d(LOG_TAG, "onKeyDown  KEYCODE_BACK keyCode"+ event.getKeyCode());
//            	//具体的操作代码
//            	return true;
//            }
//       }	    
//	    return super.dispatchKeyEvent(event);  
//	}  
	private void adjustVolume(int keyCode) {

		int iCallState = callStateManager.getCallState();
		int streamType = AudioManager.STREAM_VOICE_CALL;
		switch (iCallState) {
		case PTTConstant.CALL_RINGING:
			streamType = AudioManager.STREAM_MUSIC;
			break;
		case PTTConstant.CALL_TALKING:
		case PTTConstant.CALL_DIALING:
			streamType = AudioManager.STREAM_VOICE_CALL;
		default:
			break;
		}

		if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
		}
		int musicVolumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		int callVolumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
		Editor editor = prefs.edit();
		if (streamType == AudioManager.STREAM_MUSIC) {
			editor.putInt(getString(R.string.sp_call_ring_volume), musicVolumeLevel);
		} else if (streamType == AudioManager.STREAM_VOICE_CALL) {
			editor.putInt(getString(R.string.sp_call_single_volume), callVolumeLevel);
		}
		editor.commit();
	}

//	@Override
//	public boolean onMenuItemSelected(int featureId, MenuItem item) {
//		int itemId = item.getItemId();
//		switch (itemId) {
//		case MENU_HANGUP:
//		case PTTConstant.KEYCODE_W_ENDCALL: // KEYCODE_W_ENDCALL
//			hangup();
//			break;
//		case MENU_CHANGE_AUDIO_ROUTE:
//			changeAudioRoute();
//			if (callStateManager.currentAudioRoute == PTTConstant.AR_HANDSET) {
//				item.setTitle(getString(R.string.menu_speaker));
//			} else {
//				item.setTitle(getString(R.string.menu_handset));
//			}
//			break;
//		case MENU_ANSWER:
//			answer();
//			break;
//		default:
//			break;
//		}
//		return super.onMenuItemSelected(featureId, item);
//	}

	private void answer() {
		pttUtil.printLog(bDebug, LOG_TAG, ">>>answer");
		Intent intent = new Intent(PTTConstant.ACTION_COMMAND);
		intent.putExtra(PTTConstant.KEY_SERVICE_COMMAND, PTTConstant.COMM_ANSWER_CALL);
		intent.setClass(this, PTTService.class);
		startService(intent);
		callStateManager.setCallState(PTTConstant.CALL_TALKING);
		updateCallState(PTTConstant.CALL_TALKING);
		answerBtn.setVisibility(View.INVISIBLE);
	}

	private void changeAudioRoute() {
		pttUtil.printLog(bDebug, LOG_TAG, "change audio rout to : " + callStateManager.currentAudioRoute);
		if (callStateManager.currentAudioRoute == PTTConstant.AR_SPEAKER) {
			audioManager.setSpeakerphoneOn(false);
			audioManager.setMode(AudioManager.MODE_IN_CALL);
			if (StateManager.isbEarphone()) {
				callStateManager.currentAudioRoute = PTTConstant.AR_EARPHONE;
			} else {
				callStateManager.currentAudioRoute = PTTConstant.AR_HANDSET;
			}
		} else {
			audioManager.setSpeakerphoneOn(true);
			audioManager.setMode(AudioManager.MODE_NORMAL);
			callStateManager.currentAudioRoute = PTTConstant.AR_SPEAKER;
		}
	}

	private void resetAudioRoute() {
		audioManager.setSpeakerphoneOn(false);
		audioManager.setMode(AudioManager.MODE_NORMAL);
	}

	private void closeCallUI() {
		pttUtil.printLog(bDebug, LOG_TAG, "InCallScreenActivity closeCallUI");
		// callStateManager.closeCallUI();
		saveCallLog();
		this.finish();
		// callStateManager.clear();
	}

	private synchronized void cancelTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
			task = null;
		}
	}

//	@Override
//	public boolean onPrepareOptionsMenu(Menu menu) {
//		menu.clear();
//		int index = 0;
//		if (callStateManager.getCallState() == PTTConstant.CALL_RINGING) {
//			menu.add(0, MENU_ANSWER, index++, getString(R.string.menu_answer));
//		} else {
//			String menuText = "";
//			if (callStateManager.currentAudioRoute == PTTConstant.AR_SPEAKER) {
//				if (StateManager.isbEarphone()) {
//					menuText = getString(R.string.menu_earphone);
//				} else {
//					menuText = getString(R.string.menu_handset);
//				}
//			} else {
//				menuText = getString(R.string.menu_speaker);
//			}
//			menu.add(0, MENU_CHANGE_AUDIO_ROUTE, index++, menuText);
//		}
//		menu.add(0, MENU_HANGUP, index++, getString(R.string.btn_hangup));
//		return super.onPrepareOptionsMenu(menu);
//	}
//
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		bMenuShown = true;
//		this.menu = menu;
//		return super.onCreateOptionsMenu(menu);
//	}
//
//	@Override
//	public void onOptionsMenuClosed(Menu menu) {
//		bMenuShown = false;
//		super.onOptionsMenuClosed(menu);
//	}

//	@Override
	/**
	 * disable KEYCODE_HOME
	 */
//	public void onAttachedToWindow() {
//		pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>>>InCallScreenActivity onAttachedToWindow ");
//		this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
//	}

}
