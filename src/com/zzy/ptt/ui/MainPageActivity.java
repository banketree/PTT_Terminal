/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:MainPageActivity.java
 * Description:MainPageActivity
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-5
 */

package com.zzy.ptt.ui;

import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.zzy.ptt.R;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.service.CallStateManager;
import com.zzy.ptt.service.GroupManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.service.StateManager.EnumRegByWho;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

public class MainPageActivity extends BaseActivity implements OnFocusChangeListener, View.OnClickListener {

	private static final int MENU_REGISTER = 0;
	private static final int MENU_MINI = 1;
	private static final int MENU_EXIT = 2;
	private static final int MENU_RETURN_PTT = 3;
	private static final int MENU_TEST = 4;

	private ProgressDialog initProgressDialog;
	private ProgressDialog regProgressDialog;
	private ProgressDialog deInitProgressDialog;
	private AlertDialog alertDialog;
	private MainPageReceiver registerReceiver;
	// private SharedPreferences prefs;

	private static final String LOG_TAG = "MainPageActivity";
	private static final String MSG_KEY = "handler_msg";

	private static final int MSG_INIT_SUCCESS = 0;
	private static final int MSG_INIT_FAIL = 1;
	private static final int MSG_DEINIT_START = 2;
	private static final int MSG_DEINIT_OVER = 3;

//	private PTTManager pttManager = PTTManager.getInstance();

	private Button btnqz, btnbh, btnjl, btntxl, btnxx, btnsz;
	private TextView statusTV;

	public static MainPageActivity instance = null;

	private SharedPreferences sp;
	private GroupReceiver groupReceiver;

	public Handler mainPageHandler = new Handler() {

		
		public void handleMessage(Message msg) {

			Bundle bundle = null;
			int errorCode = -1;
			AlertDialogManager.getInstance().dismissProgressDialog(initProgressDialog);
			switch (msg.what) {
			case MSG_DEINIT_START:
				stopService(new Intent(MainPageActivity.this, PTTService.class));
				break;
			case MSG_DEINIT_OVER:
				if (deInitProgressDialog != null && deInitProgressDialog.isShowing()) {
					deInitProgressDialog.dismiss();
				}
				StateManager.reset();
				MainPageActivity.this.finish();
				break;
			case MSG_INIT_SUCCESS:
				// need a service here to update the status
				doRegister();
				break;
			case MSG_INIT_FAIL:
				bundle = msg.getData();
				errorCode = bundle.getInt(MSG_KEY);
				AlertDialog.Builder builder = new AlertDialog.Builder(MainPageActivity.this);
				builder.setTitle(MainPageActivity.this.getString(R.string.alert_title_init));
				builder.setMessage(MainPageActivity.this.getString(R.string.alert_msg_init_fail) + " : " + errorCode);
				builder.setNeutralButton(R.string.alert_btn_ok, new OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						// MainPageActivity.this.finish();
					}
				});

				AlertDialog alert = builder.create();
				alert.setCancelable(false);
				alert.setOnDismissListener(new OnDismissListener() {
					
					public void onDismiss(DialogInterface dialog) {
						MainPageActivity.this.finish();
					}
				});
				alert.show();
				break;
			default:
				break;
			}
		}
	};

	public class MainPageReceiver extends BroadcastReceiver {
		private Context context;

		// construct
		public MainPageReceiver(Context c) {
			context = c;
		}

		public void registerAction(String action) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(action);

			context.registerReceiver(this, filter);
		}

		
		public void onReceive(Context context, Intent intent) {
			int state = -1;
			String action = intent.getAction();

			Log.d(LOG_TAG, "<<<<<<<<<<<Receive action : " + action);
			if (PTTConstant.ACTION_REGISTER.equals(action)) {

				if (regProgressDialog != null) {
					AlertDialogManager.getInstance().dismissProgressDialog(regProgressDialog);
				}
				// register state
				state = intent.getIntExtra(PTTConstant.KEY_REGISTER_STATE, 0);
				Log.d(LOG_TAG, "<<<<<<<<<<<Receive register state : " + state);
				// show title info
				if(!StateManager.exitFlag)
					showTitle(state);
				
				if(PTTConstant.DUMMY) {
					GroupInfo[] groupInfos = new GroupInfo[1];
					groupInfos[0] = new GroupInfo("dummy", "9000");
					PTTService.instance.JNIAddGroupInfo(groupInfos);
				}

			} else if (PTTConstant.ACTION_INIT.equals(action)) {
				// init state
				int initState = intent.getIntExtra(PTTConstant.KEY_INIT_STATE, 0);
				Log.d(LOG_TAG, "<<<<<<<<<<<Receive init state : " + initState);
				if (initState == PTTConstant.INIT_SUCCESS) {
					mainPageHandler.sendEmptyMessage(MSG_INIT_SUCCESS);
				} else {
					int errorCode = intent.getIntExtra(PTTConstant.KEY_RETURN_VALUE, 0);
					Message msg = new Message();
					msg.what = MSG_INIT_FAIL;
					Bundle tempBundle = new Bundle();
					tempBundle.putInt(MSG_KEY, errorCode);
					mainPageHandler.sendMessage(msg);
				}
			} else if (PTTConstant.ACTION_NUMBER_KEY2.equals(action)) {
				// init state
				int keyCode = intent.getIntExtra(PTTConstant.KEY_NUMBER, 0);
				Log.d(LOG_TAG, "()()()()()()()(keyCode : " + keyCode);
				if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
					Intent intent2 = new Intent(MainPageActivity.this, DialActivity.class);
					intent2.putExtra("OK", 123);
					intent2.putExtra("keyCode", (keyCode - 7) + "");
					startActivity(intent2);
				}
			} else if (PTTConstant.ACTION_DEINIT.equals(action)) {
				// deinit state
				mainPageHandler.sendEmptyMessage(MSG_DEINIT_OVER);
				Log.d(LOG_TAG, "action : " + action + " ACTION_DEINIT!!!");
			} else {
				Log.d(LOG_TAG, "action : " + action + " unknown!!!");
			}
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.mainpage);

		sp = PreferenceManager.getDefaultSharedPreferences(this);

		initButtons();
		statusTV = (TextView) findViewById(R.id.maintextView1);
		// show menu items
		PTTUtil.getInstance().initOnCreat(this);
		// prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// show init dialog
		if (StateManager.getInitState() != PTTConstant.INIT_SUCCESS) {
			initProgressDialog = AlertDialogManager.getInstance().showProgressDialog(this,
					getString(R.string.alert_title_init), getString(R.string.alert_msg_initing));
		}
		Intent intent = new Intent(MainPageActivity.this, PTTService.class);
		//intent.setAction(PTTConstant.ACTION_INIT);
		startService(intent);

		instance = this;

		Log.d(LOG_TAG, "MainPageActivity onCreate Over");

		groupReceiver = new GroupReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(PTTConstant.ACTION_DYNAMIC_REGRP);
		this.registerReceiver(groupReceiver, filter);
		
		if (StateManager.getCurrentRegState() == EnumLoginState.REGISTERE_SUCCESS) {
			Intent intentTOHO = new Intent("com.zzy.action.start");
			sendBroadcast(intentTOHO);
		}
		
	}

	private class GroupReceiver extends BroadcastReceiver {
		
		public void onReceive(Context context, Intent intent) {
			updateReisterInfo();
		}
	}

	private void initButtons() {
		btnqz = (Button) findViewById(R.id.mainbutton1);
		btnbh = (Button) findViewById(R.id.mainbutton2);
		btnjl = (Button) findViewById(R.id.mainbutton3);
		btntxl = (Button) findViewById(R.id.mainbutton4);
		btnxx = (Button) findViewById(R.id.mainbutton5);
		btnsz = (Button) findViewById(R.id.mainbutton6);

		btnqz.setOnFocusChangeListener(this);
		btnqz.setOnClickListener(this);

		btnbh.setOnFocusChangeListener(this);
		btnbh.setOnClickListener(this);

		btnjl.setOnFocusChangeListener(this);
		btnjl.setOnClickListener(this);

		btntxl.setOnFocusChangeListener(this);
		btntxl.setOnClickListener(this);

		btnxx.setOnFocusChangeListener(this);
		btnxx.setOnClickListener(this);

		btnsz.setOnFocusChangeListener(this);
		btnsz.setOnClickListener(this);

		btnqz.setBackgroundResource(R.drawable.qunzu5);
		btnbh.setBackgroundResource(R.drawable.bohao05);
		btnjl.setBackgroundResource(R.drawable.jilu5);
		btntxl.setBackgroundResource(R.drawable.tongxunlu5);
		btnxx.setBackgroundResource(R.drawable.xx5);
		btnsz.setBackgroundResource(R.drawable.shezhi5);
	}

	private void showTitle(int state) {
		String titleInfo = "";
		String curGrp = sp.getString(PTTConstant.SP_CURR_GRP_NUM, "");
		List<GroupInfo> grpData = GroupManager.getInstance().getGroupData();
		if (curGrp == null || curGrp.trim().length() == 0) {
			if (grpData == null || grpData.size() == 0) {
				curGrp = "";
			} else {
				curGrp = grpData.get(0).getNumber();
			}
		} else {
			if (grpData == null || grpData.size() == 0) {
				PTTUtil.getInstance().setCurrentGrp("");
				curGrp = "";
			}
		}
		if (state == PTTConstant.REG_ED) {
			titleInfo = PTTUtil.getInstance().getCurrentUserName(this);
			statusTV.setText(getApplicationContext().getString(R.string.current_group) + curGrp + "  "
					+ getApplicationContext().getString(R.string.current_num) + titleInfo);
		} else {
			titleInfo = getString(PTTUtil.getInstance().getTitleId(state));
			statusTV.setText(titleInfo);
		}
	}

	private void showTitle(EnumLoginState currentState) {
		String titleInfo = "";
		String curGrp = sp.getString(PTTConstant.SP_CURR_GRP_NUM, "");
		List<GroupInfo> grpData = GroupManager.getInstance().getGroupData();
		if (curGrp == null || curGrp.trim().length() == 0) {
			if (grpData == null || grpData.size() == 0) {
				curGrp = "";
			} else {
				curGrp = grpData.get(0).getNumber();
			}
		} else {
			if (grpData == null || grpData.size() == 0) {
				PTTUtil.getInstance().setCurrentGrp("");
				curGrp = "";
			}
		}

		if (currentState == EnumLoginState.REGISTERE_SUCCESS) {
			titleInfo = PTTUtil.getInstance().getCurrentUserName(this);
			statusTV.setText(getApplicationContext().getString(R.string.current_group) + curGrp + "  "
					+ getApplicationContext().getString(R.string.current_num) + titleInfo);
		} else {
			titleInfo = getString(PTTUtil.getInstance().getTitleId(currentState));
			statusTV.setText(titleInfo);
		}
	}

	private void registerReceiverAction() {
		registerReceiver = new MainPageReceiver(this);
		registerReceiver.registerAction(PTTConstant.ACTION_INIT);
		registerReceiver.registerAction(PTTConstant.ACTION_REGISTER);
		registerReceiver.registerAction(PTTConstant.ACTION_NUMBER_KEY2);
		registerReceiver.registerAction(PTTConstant.ACTION_DEINIT);
	}

	private void doRegister() {
		// start to register and show progress dialog
		Log.d(LOG_TAG,
				"doRegister regstate : " + PTTUtil.getInstance().getRegStateString(StateManager.getCurrentRegState()));
		if (StateManager.getCurrentRegState() != EnumLoginState.REGISTERE_SUCCESS) {

			regProgressDialog = AlertDialogManager.getInstance().showProgressDialog(this,
					getString(R.string.alert_title_register), getString(R.string.alert_msg_registering));
			regProgressDialog.setOnCancelListener(new OnCancelListener() {
				
				public void onCancel(DialogInterface dialog) {
					if (StateManager.getCurrentRegState() != EnumLoginState.REGISTERE_SUCCESS) {
						StateManager.setCurrentRegState(EnumLoginState.UNREGISTERED);
						showTitle(EnumLoginState.UNREGISTERED);
					}
				}
			});
			StateManager.setRegStarter(EnumRegByWho.REG_BY_USER);
			Intent intent2 = new Intent(PTTConstant.ACTION_REGISTER);
			intent2.setClass(this, PTTService.class);
			intent2.putExtra(PTTConstant.KEY_REREGISTER_FORCE, true);
			startService(intent2);

			showTitle(EnumLoginState.REGISTERING);
		}
	}

	
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
		unregisterReceiver(groupReceiver);
		if (alertDialog != null) {
			alertDialog.dismiss();
		}
		if (deInitProgressDialog != null && deInitProgressDialog.isShowing()) {
			deInitProgressDialog.dismiss();
		}
		if (AlertActivity.instance != null) {
			AlertActivity.instance.finish();
		}
		StateManager.exitFlag = false;
		Log.d(LOG_TAG, "MainPageActivity.onDestory()");
	}

	private void unregisterReceiverAction() {
		if (registerReceiver != null)
			this.unregisterReceiver(registerReceiver);
	}

	
	public boolean onCreateOptionsMenu(Menu menu) {
		int order = 0;

		menu.clear();

		if (StateManager.getCurrentRegState() == EnumLoginState.ERROR_CODE_NUMBER
				|| StateManager.getCurrentRegState() == EnumLoginState.UNREGISTERED) {
			menu.add(0, MENU_REGISTER, order++, R.string.menu_register);
		}
		//if (pttManager.getCurrentPttState().getStatus() != PTTConstant.PTT_IDLE) {
		//}
		menu.add(0, MENU_RETURN_PTT, order++, R.string.menu_return_ptt);
		menu.add(0, MENU_MINI, order++, R.string.menu_minialmize);
		menu.add(0, MENU_EXIT, order++, R.string.menu_exit);
		menu.add(0, MENU_TEST, order++, R.string.aboutpyy);
		return super.onCreateOptionsMenu(menu);
	}

	
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case MENU_TEST:
			startActivity(new Intent(getApplicationContext(), AboutActivity.class));
			break;
		case MENU_EXIT:
			StateManager.stopNotification();
			destorySipStack();
			break;
		case MENU_MINI:
			this.finish();
			break;
		case MENU_RETURN_PTT:
//			if (pttManager.getCurrentPttState().getStatus() == PTTConstant.PTT_IDLE) {
//				AlertDialogManager.getInstance().toastNoPtt();
//				return true;
//			}
			Intent intent = new Intent(this, PTTActivity.class);
			intent.setAction(PTTConstant.ACTION_PTT);
			this.startActivity(intent);
			break;
		case MENU_REGISTER:
			doRegister();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	
	protected void onResume() {
		super.onResume();
		
		instance = this;
		
		registerReceiverAction();
		Log.d(LOG_TAG, "onResume");

		// update register info
		updateReisterInfo();

		if (StateManager.getInitState() == PTTConstant.INIT_SUCCESS && initProgressDialog != null
				&& initProgressDialog.isShowing()) {
			initProgressDialog.dismiss();
			if (StateManager.getCurrentRegState() == EnumLoginState.UNREGISTERED) {
				doRegister();
			}
		}

		if (StateManager.getCurrentRegState() == EnumLoginState.REGISTERE_SUCCESS && regProgressDialog != null
				&& regProgressDialog.isShowing()) {
			regProgressDialog.dismiss();
		}
		
		int iCallState = CallStateManager.getInstance().getCallState();
		if (iCallState >= PTTConstant.CALL_DIALING && iCallState <= PTTConstant.CALL_TALKING) {
			Intent intent = new Intent(this,InCallScreenActivity.class);
			startActivity(intent);
		}
	}

	
	protected void onPause() {
		super.onPause();
		// instance = null;
		unregisterReceiverAction();
		Log.d(LOG_TAG, "onPause instance " + instance);
	}
	
	@Override
	protected void onStop() {
		Log.d(LOG_TAG, "onPause");
		super.onStop();
		instance = null;
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			askIfExit();
			return true;
		case PTTConstant.KEYCODE_W_CALL:
		case KeyEvent.KEYCODE_CALL:
			// go to call log
			startActivity(new Intent(this, CallLogTabActivity.class));
			return true;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void updateReisterInfo() {
		showTitle(StateManager.getCurrentRegState());
	}

	private void destorySipStack() {
		Log.d(LOG_TAG, "<<<<<<<<<<<deInitProgressDialog to show: ");
		deInitProgressDialog = ProgressDialog.show(MainPageActivity.this, getString(R.string.alert_title_exit),
				getString(R.string.alert_msg_exiting));
		deInitProgressDialog.setCancelable(false);
		Log.d(LOG_TAG, "<<<<<<<<<<<deInitProgressDialog to show: ");
		mainPageHandler.sendEmptyMessage(MSG_DEINIT_START);
	}

	private void askIfExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.alert_title));
		builder.setMessage(getString(R.string.alert_title_ifexit));
		builder.setPositiveButton(getString(R.string.menu_minialmize), new OnClickListener() {

			
			public void onClick(DialogInterface dialog, int which) {
				MainPageActivity.this.finish();
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(getString(R.string.menu_exit), new OnClickListener() {

			
			public void onClick(DialogInterface dialog, int which) {
				StateManager.exitFlag = true;
				// MainPageActivity.this.finish();
				dialog.dismiss();
				// -----add by wangjunhui
				StateManager.stopNotification();
				destorySipStack();
			}
		});
		builder.create().show();

	}

	
	public void onClick(View v) {
		@SuppressWarnings("rawtypes")
		Class clazz = null;
		switch (v.getId()) {
		case R.id.mainbutton1:
			clazz = GroupActivity.class;
			break;
		case R.id.mainbutton2:
			clazz = DialActivity.class;
			break;
		case R.id.mainbutton3:
			clazz = CallLogTabActivity.class;
			break;
		case R.id.mainbutton4:
			clazz = PBKActivity.class;
			break;
		case R.id.mainbutton5:
			clazz = MessageActivity.class;
			break;
		case R.id.mainbutton6:
			clazz = SettingActivity.class;
			break;

		default:
			break;
		}
		Log.d(LOG_TAG, "MainPageActivity -------> " + clazz.getName());
		Intent intent = new Intent();
		intent.setClass(getApplicationContext(), clazz);
		startActivity(intent);
	}

	
	public void onFocusChange(View v, boolean hasFocus) {
		if (hasFocus) {
			switch (v.getId()) {
			case R.id.mainbutton1:
				btnqz.setBackgroundResource(R.drawable.qunzu6);
				break;
			case R.id.mainbutton2:
				btnbh.setBackgroundResource(R.drawable.bohao06);
				break;
			case R.id.mainbutton3:
				btnjl.setBackgroundResource(R.drawable.jilu6);
				break;
			case R.id.mainbutton4:
				btntxl.setBackgroundResource(R.drawable.tongxunlu6);
				break;
			case R.id.mainbutton5:
				btnxx.setBackgroundResource(R.drawable.xx6);
				break;
			case R.id.mainbutton6:
				btnsz.setBackgroundResource(R.drawable.shezhi6);
				break;

			default:
				break;
			}
		} else {
			switch (v.getId()) {
			case R.id.mainbutton1:
				btnqz.setBackgroundResource(R.drawable.qunzu5);
				break;
			case R.id.mainbutton2:
				btnbh.setBackgroundResource(R.drawable.bohao05);
				break;
			case R.id.mainbutton3:
				btnjl.setBackgroundResource(R.drawable.jilu5);
				break;
			case R.id.mainbutton4:
				btntxl.setBackgroundResource(R.drawable.tongxunlu5);
				break;
			case R.id.mainbutton5:
				btnxx.setBackgroundResource(R.drawable.xx5);
				break;
			case R.id.mainbutton6:
				btnsz.setBackgroundResource(R.drawable.shezhi5);
				break;

			default:
				break;
			}
		}
	}

	
	public boolean onPrepareOptionsMenu(Menu menu) {
		int order = 0;

		menu.clear();

		if (StateManager.getCurrentRegState() == EnumLoginState.ERROR_CODE_NUMBER
				|| StateManager.getCurrentRegState() == EnumLoginState.UNREGISTERED) {
			menu.add(0, MENU_REGISTER, order++, R.string.menu_register);
		}
		//if (pttManager.getCurrentPttState().getStatus() != PTTConstant.PTT_IDLE) {
		//}
		menu.add(0, MENU_RETURN_PTT, order++, R.string.menu_return_ptt);
		menu.add(0, MENU_MINI, order++, R.string.menu_minialmize);
		menu.add(0, MENU_EXIT, order++, R.string.menu_exit);
		menu.add(0, MENU_TEST, order++, R.string.aboutpyy);
		return super.onPrepareOptionsMenu(menu);
	}

}
