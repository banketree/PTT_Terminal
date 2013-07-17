/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:SettingDetailActivity.java
 * Description:SettingDetailActivity
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-6
 */

package com.zzy.ptt.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.model.SipServer;
import com.zzy.ptt.model.SipUser;
import com.zzy.ptt.service.GroupManager;
import com.zzy.ptt.service.PTTManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.service.StateManager.EnumRegByWho;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class SettingDetailActivity extends BaseActivity implements OnClickListener,
		OnSeekBarChangeListener, OnCheckedChangeListener {

	private Intent intent;
	private int currentItemId;

	// for register setting
	private EditText etServerIP, etPort, etUsername,
			etPassword;
	private Button btnrergi;
	private CheckBox cbAutoRegister;
	private SharedPreferences prefs;

	private Spinner groupChoseSpinner;
	private TextView groupChosetv;

	// for call setting
	private RadioButton radioHandset, radioSpeaker;
	// private CheckBox cbAutoAnswer;
	// for ringtone setting
	private SeekBar callVolumeBar, pttVolumeBar, ringVolumeBar;
	private CheckBox cbRing, cbVirabate;
	private TextView tvCurrentRingtone;
	private Button btnCallRing;
	private MediaPlayer player;
	// for system setting
	private CheckBox cbAutoStart;

	private BroadcastReceiver registerReceiver;
	private BroadcastReceiver pttReceiver;

	private static final String LOG_TAG = "SettingDetailActivity";
	private static boolean bDebug = true;

	private static final int MENU_RETURN = 0;
	private static final int MENU_REREGISTER = 1;

	private AudioManager audioManager = null;
	private PTTUtil pttUtil = PTTUtil.getInstance();

	// add by wangjunhui
	private boolean bDirty = false;
	private ProgressDialog progressDialog;
	private String[] currentGroups;
	private List<GroupInfo> lstGroupData;
	private List<String> lstGroupNum;
	private ArrayAdapter<String> adapter;
	private SharedPreferences sp;
	private Editor editor;
	private String currGrpNum = null;

	public static SettingDetailActivity instance = null;

	public String getCurrGrpNum() {
		return this.currGrpNum;
	}

	public void setCurrGrpNum(String currGrpNum) {
		this.currGrpNum = currGrpNum;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PTTUtil.getInstance().initOnCreat(this);

		intent = getIntent();
		currentItemId = intent.getIntExtra(PTTConstant.SETTING_DISPATCH_KEY, 0);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		showView(currentItemId);

	}

	private void showView(int itemId) {

		switch (itemId) {
		case PTTConstant.SETTING_ITEM_REGISTGER:
			setContentView(R.layout.register_setting_layout);
			this.setTitle(getApplicationContext().getString(R.string.setting_register));
			loadRegisterSettingData();
			IntentFilter filter = new IntentFilter(PTTConstant.ACTION_REGISTER);
			addRegReceiver();
			registerReceiver(registerReceiver, filter);
			break;
		case PTTConstant.SETTING_ITEM_TALKING:
			setContentView(R.layout.call_setting_layout);
			this.setTitle(getApplicationContext().getString(R.string.setting_talking));
			loadCallSettingData();
			addPttReceiver();
			filter = new IntentFilter(PTTConstant.ACTION_PTT);
			registerReceiver(pttReceiver, filter);
			filter = new IntentFilter(PTTConstant.ACTION_DYNAMIC_REGRP);
			registerReceiver(pttReceiver, filter);
			break;
		case PTTConstant.SETTING_ITEM_ALERTRING:
			setContentView(R.layout.ringtone_setting_layout);
			this.setTitle(getApplicationContext().getString(R.string.setting_alertring));
			loadRingData();
			break;
		case PTTConstant.SETTING_ITEM_SYSTEM:
			setContentView(R.layout.system_setting_layout);
			this.setTitle(getApplicationContext().getString(R.string.setting_system));
			loadSystemData();
		default:
			break;
		}
	}

	private void loadSystemData() {

		cbAutoStart = (CheckBox) findViewById(R.id.cb_auto_start);
		cbAutoStart.setChecked(prefs.getBoolean(getString(R.string.sp_auto_start), true));
		cbAutoStart.setOnCheckedChangeListener(this);
	}

	private void loadRingData() {

		player = new MediaPlayer();

		callVolumeBar = (SeekBar) findViewById(R.id.seekbar_call_single_volume);
		pttVolumeBar = (SeekBar) findViewById(R.id.seekbar_call_ptt_volume);
		ringVolumeBar = (SeekBar) findViewById(R.id.seekbar_call_ring_volume);

		cbRing = (CheckBox) findViewById(R.id.cb_cond_ring);
		cbVirabate = (CheckBox) findViewById(R.id.cb_cond_virabate);
		tvCurrentRingtone = (TextView) findViewById(R.id.tv_current_ring);
		btnCallRing = (Button) findViewById(R.id.btn_choose_ringtone);

		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		ringVolumeBar.setMax(maxVolume);
		int volumeLevle = prefs.getInt(getString(R.string.sp_call_ring_volume), -1);
		if (volumeLevle == -1) {
			Editor editor = prefs.edit();
			editor.putInt(getString(R.string.sp_call_ring_volume), maxVolume);
			editor.commit();
			volumeLevle = maxVolume;
		}
		ringVolumeBar.setProgress(volumeLevle);

		maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
		callVolumeBar.setMax(maxVolume);
		volumeLevle = prefs.getInt(getString(R.string.sp_call_single_volume), -1);
		if (volumeLevle == -1) {
			Editor editor = prefs.edit();
			editor.putInt(getString(R.string.sp_call_single_volume), maxVolume);
			editor.commit();
			volumeLevle = maxVolume;
		}
		callVolumeBar.setProgress(volumeLevle);

		maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
		volumeLevle = prefs.getInt(getString(R.string.sp_call_ptt_volume), -1);
		pttVolumeBar.setMax(maxVolume);
		if (volumeLevle == -1) {
			Editor editor = prefs.edit();
			editor.putInt(getString(R.string.sp_call_ptt_volume), maxVolume);
			editor.commit();
			volumeLevle = maxVolume;
		}
		pttVolumeBar.setProgress(volumeLevle);

		cbRing.setChecked(prefs.getBoolean(getString(R.string.sp_ring_enable), true));
		cbVirabate.setChecked(prefs.getBoolean(getString(R.string.sp_virabate_enable), true));
		btnCallRing.setEnabled(cbRing.isChecked());

		String currentRing = prefs.getString(getString(R.string.sp_current_ringtone), null);
		if (currentRing == null || currentRing.length() == 0) {
			currentRing = PTTUtil.getInstance().getFirstRingName(getResources(), "call_");
		}
		tvCurrentRingtone.setText(currentRing);

		callVolumeBar.setOnSeekBarChangeListener(this);
		pttVolumeBar.setOnSeekBarChangeListener(this);
		ringVolumeBar.setOnSeekBarChangeListener(this);
		cbRing.setOnCheckedChangeListener(this);
		cbVirabate.setOnCheckedChangeListener(this);
		btnCallRing.setOnClickListener(this);
	}

	private void checkIfDirty(int itemId) {
		switch (itemId) {
		case PTTConstant.SETTING_ITEM_REGISTGER:
			bDirty = checkIfRegisterDirty();
			break;

		default:
			break;
		}
	}

	private boolean checkIfRegisterDirty() {
		String oldServerip = prefs.getString(PTTConstant.SP_SERVERIP, "");
		if (!oldServerip.equals(etServerIP.getText().toString())) {
			return true;
		}

		String oldServerport = prefs.getString(PTTConstant.SP_PORT, "");
		if (!oldServerport.equals(etPort.getText().toString())) {
			return true;
		}

		String oldUsername = prefs.getString(PTTConstant.SP_USERNAME, "");
		if (!oldUsername.equals(etUsername.getText().toString())) {
			return true;
		}

		String oldPassword = prefs.getString(PTTConstant.SP_PASSWORD, "");
		if (!oldPassword.equals(etPassword.getText().toString())) {
			return true;
		}
		return false;
	}

	private void loadRegisterSettingData() {
		etServerIP = (EditText) findViewById(R.id.et_serverip1);
		etUsername = (EditText) findViewById(R.id.et_username);
		etPassword = (EditText) findViewById(R.id.et_password);
		etPort = (EditText) findViewById(R.id.et_port);
		cbAutoRegister = (CheckBox) findViewById(R.id.cb_autoregister);
		btnrergi = (Button) findViewById(R.id.btn_regi);

		etServerIP.setFilters(new InputFilter[] { new InputFilter.LengthFilter(15) });
		etUsername.setFilters(new InputFilter[] { new InputFilter.LengthFilter(10) });
		etPassword.setFilters(new InputFilter[] { new InputFilter.LengthFilter(10) });
		etPort.setFilters(new InputFilter[] { new InputFilter.LengthFilter(10) });

		SipServer sipServer = PTTUtil.getInstance().getSipServerFromPrefs(prefs);
		SipUser sipUser = PTTUtil.getInstance().getSipUserFromPrefs(prefs);
		boolean autoRegister = prefs.getBoolean(PTTConstant.SP_AUTOREGISTER, true);

		cbAutoRegister.setOnCheckedChangeListener(this);
		cbAutoRegister.setVisibility(View.GONE);

		
		etServerIP.setText(sipServer.getServerIp());
		etPort.setText(sipServer.getPort());
		etUsername.setText(sipUser.getUsername());
		etPassword.setText(sipUser.getPasswd());
		cbAutoRegister.setChecked(autoRegister);

		
		btnrergi.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(doSave()){
					doReregister();
				}
			}
		});
	}

	private void loadCallSettingData() {

		// cbAutoAnswer = (CheckBox) findViewById(R.id.cb_auto_answer);
		radioHandset = (RadioButton) findViewById(R.id.id_ar_handset);
		radioSpeaker = (RadioButton) findViewById(R.id.id_ar_speaker);

		// cbAutoAnswer.setChecked(prefs.getBoolean(getString(R.string.sp_auto_answer),
		// false));
		radioHandset.setChecked(prefs.getBoolean(getString(R.string.sp_ar_handset), true));
		radioSpeaker.setChecked(prefs.getBoolean(getString(R.string.sp_ar_speaker), false));

		radioHandset.setOnClickListener(this);
		radioSpeaker.setOnClickListener(this);
		// cbAutoAnswer.setOnClickListener(this);
		// add by wangjunhui
		groupChoseSpinner = (Spinner) findViewById(R.id.group_chose_Spinner);
		groupChosetv = (TextView) findViewById(R.id.group_chose_tv);
		sp = PTTService.instance.prefs;
		editor = sp.edit();
		lstGroupData = GroupManager.getInstance().getGroupData();
		lstGroupNum = new ArrayList<String>();
		currentGroups = new String[lstGroupData.size()];
		for (int i = 0; i < lstGroupData.size(); i++) {
			currentGroups[i] = lstGroupData.get(i).getName() + "--("
					+ lstGroupData.get(i).getNumber() + ")";
			lstGroupNum.add(lstGroupData.get(i).getNumber());
		}
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
				currentGroups);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		groupChoseSpinner.setAdapter(adapter);
		if (sp.getString(PTTConstant.SP_CURR_GRP_NUM, "").trim().length() != 0
				&& lstGroupNum.indexOf(sp.getString(PTTConstant.SP_CURR_GRP_NUM, "")) != -1) {
			groupChoseSpinner.setSelection(
					lstGroupNum.indexOf(sp.getString(PTTConstant.SP_CURR_GRP_NUM, "")), true);
			groupChosetv.setText(getApplicationContext().getString(R.string.setting_current_group));
		} else if (lstGroupData.size() != 0) {
			groupChoseSpinner.setSelection(0, true);
			editor.putString(PTTConstant.SP_CURR_GRP_NUM, lstGroupData.get(0).getNumber());
			editor.commit();
			setCurrGrpNum(lstGroupData.get(0).getNumber());
			groupChosetv.setText(getApplicationContext().getString(R.string.setting_current_group));
		} else {
			adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
					new String[] { "" });
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			groupChoseSpinner.setAdapter(adapter);
			groupChoseSpinner.setEnabled(false);
		}
		groupChoseSpinner.setOnItemSelectedListener(spinnerListener);
		groupChoseSpinner.setVisibility(View.VISIBLE);
	}

	// add by wangjunhui
	private Spinner.OnItemSelectedListener spinnerListener = new Spinner.OnItemSelectedListener() {
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
			if (lstGroupData.size() != 0) {
				groupChosetv.setText(getApplicationContext().getString(
						R.string.setting_current_group));
				editor.putString(PTTConstant.SP_CURR_GRP_NUM, lstGroupData.get(arg2).getNumber());
				editor.commit();
				setCurrGrpNum(lstGroupData.get(arg2).getNumber());
			} else {
				groupChosetv.setText(getApplicationContext().getString(
						R.string.setting_current_group));
				groupChoseSpinner.setEnabled(false);
			}
		}

		public void onNothingSelected(AdapterView<?> arg0) {

		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		int index = 0;
		switch (currentItemId) {
		case PTTConstant.SETTING_ITEM_REGISTGER:
			//menu.add(0, MENU_REREGISTER, index++, R.string.menu_reregister);
			menu.add(0, MENU_RETURN, index++, R.string.menu_return);
			break;
		default:
			break;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case MENU_RETURN:
			checkIfDirty(currentItemId);
			if (bDirty) {
				switch (currentItemId) {
				case PTTConstant.SETTING_ITEM_REGISTGER:
					askIfReregister();
					break;
				default:
					break;
				}
			}else{
				SettingDetailActivity.this.finish();
			}
			return true;
		case MENU_REREGISTER:
			if(doSave()){
				doReregister();
			}
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private boolean doSave() {
		String ip = etServerIP.getText().toString();
		//check IP addr if correct
		if(!pttUtil.checkIP(ip)){
			Toast.makeText(getApplicationContext(), "wrong ip addr", Toast.LENGTH_SHORT).show();
			return false;
		}
		
		Editor editor = prefs.edit();
		switch (currentItemId) {
		case PTTConstant.SETTING_ITEM_REGISTGER:
			editor.putBoolean(PTTConstant.SP_AUTOREGISTER, cbAutoRegister.isChecked());
			editor.putString(PTTConstant.SP_SERVERIP, ip);
			editor.putString(PTTConstant.SP_PORT, etPort.getText().toString());
			editor.putString(PTTConstant.SP_USERNAME, etUsername.getText().toString());
			editor.putString(PTTConstant.SP_PASSWORD, etPassword.getText().toString());
			bDirty = false;
			break;
		default:
			break;
		}
		editor.commit();
		
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
		bDirty = false;
		if (registerReceiver != null) {
			unregisterReceiver(registerReceiver);
		}
		if (pttReceiver != null) {
			unregisterReceiver(pttReceiver);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		super.onKeyDown(keyCode, event);

		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			checkIfDirty(currentItemId);
			if (bDirty && (currentItemId == PTTConstant.SETTING_ITEM_REGISTGER)) {
				askIfReregister();
			} else {
				this.finish();
			}
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void askIfReregister() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.alert_title_register));
		builder.setMessage(getString(R.string.alert_msg_ifreregister));
		builder.setPositiveButton(getString(R.string.alert_btn_yes),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						// new thread to register, show progress dialog, update
						// notification and currentState
						dialog.dismiss();
						bDirty = false;
						if(doSave()){
							doReregister();
						}
					}
				});
		builder.setNegativeButton(getString(R.string.alert_btn_no),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						SettingDetailActivity.this.finish();
					}
				});
		builder.create().show();
	}

	private void doReregister() {
		progressDialog = AlertDialogManager.getInstance().showProgressDialog(
				SettingDetailActivity.this, getString(R.string.alert_title_register),
				getString(R.string.alert_msg_registering));
		progressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (StateManager.getCurrentRegState() != EnumLoginState.REGISTERE_SUCCESS) {
					StateManager.setCurrentRegState(EnumLoginState.UNREGISTERED);
				}
			}
		});
		StateManager.setRegStarter(EnumRegByWho.REG_BY_USER);
		Intent intent = new Intent(PTTConstant.ACTION_REGISTER);
		intent.putExtra(PTTConstant.KEY_REREGISTER_FORCE, true);
		intent.setClass(this, PTTService.class);
		startService(intent);
	}

	private void addRegReceiver() {
		registerReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();

				if (PTTConstant.ACTION_REGISTER.equals(action)) {
					// register state
					if (progressDialog != null && progressDialog.isShowing()) {
						AlertDialogManager.getInstance().dismissProgressDialog(progressDialog);
					}
				}
			}
		};
	}

	private void addPttReceiver() {
		pttReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action == null || action.trim().length() == 0) {
					return;
				}

				if (action.equals(PTTConstant.ACTION_PTT)) {
					if (PTTManager.getInstance().getCurrentPttState().getStatus() == PTTConstant.PTT_IDLE) {
						groupChoseSpinner.setEnabled(true);
					}
				}
				if (action.equals(PTTConstant.ACTION_DYNAMIC_REGRP)) {
					loadCallSettingData();
				}
			}
		};
	}

	@Override
	public void onClick(View v) {
		Editor editor = prefs.edit();
		if (v == radioHandset || v == radioSpeaker) {
			editor.putBoolean(getString(R.string.sp_ar_handset), radioHandset.isChecked());
			editor.putBoolean(getString(R.string.sp_ar_speaker), radioSpeaker.isChecked());
		} /*
		 * else if (v == cbAutoAnswer) {
		 * editor.putBoolean(getString(R.string.sp_auto_answer),
		 * cbAutoAnswer.isChecked()); }
		 */
		editor.commit();

		if (v == btnCallRing) {
			chooseCallRing();
		}

		// if (v == groupChoseSpinner) {
		// if (groupChoseSpinner.isEnabled()) {
		// // do nothing
		// } else {
		// AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// builder.setTitle(getString(R.string.alert_title));
		// builder.setMessage(getString(R.string.alert_msg_ptt_not_closed));
		// builder.setNeutralButton(R.string.alert_btn_ok, new
		// DialogInterface.OnClickListener() {
		//
		// @Override
		// public void onClick(DialogInterface dialog, int which) {
		// dialog.dismiss();
		// }
		// });
		// builder.create().show();
		// }
		// }
	}

	private class RingDialogListener implements DialogInterface.OnClickListener, OnDismissListener,
			OnShowListener {

		private String[] items;

		private int which;

		private int currentWhich = -1;

		public RingDialogListener(String[] items) {
			this.items = items;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {

			this.which = which;

			if (player == null) {
				player = new MediaPlayer();
			}

			pttUtil.printLog(bDebug, LOG_TAG, "onClick :, player " + player.isPlaying());
			if (player.isPlaying()) {
				if (currentWhich == this.which) {
					// do nothing
					return;
				} else {
					player.stop();
					player.release();
				}
			}

			pttUtil.printLog(bDebug, LOG_TAG, "onClick :, which " + which);

			AssetFileDescriptor fd = PTTUtil.getInstance().getFirstRingFD(getResources(),
					items[which]);
			if (fd != null) {
				try {
					player = new MediaPlayer();
					player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(),
							fd.getLength());
					player.prepare();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
				if (volume <= 0)
					volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
				pttUtil.printLog(bDebug, LOG_TAG, "onClick :, volume " + volume);
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
				audioManager.setSpeakerphoneOn(true);
				audioManager.setMode(AudioManager.MODE_NORMAL);
				pttUtil.printLog(bDebug, LOG_TAG, "ringFile : " + items[which] + " volumn : "
						+ volume);
				player.setLooping(false);
				currentWhich = which;
				player.start();
			}
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>onDismiss");
			if (player != null && player.isPlaying()) {
				player.stop();
				player.release();
			}
			player = null;
			audioManager.setSpeakerphoneOn(false);
			audioManager.setMode(AudioManager.MODE_NORMAL);
			currentWhich = -1;
		}

		public int getWhich() {
			return which;
		}

		@Override
		public void onShow(DialogInterface dialog) {
			pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>onShow");

		}
	}

	private void chooseCallRing() {
		List<String> lstRings = PTTUtil.getInstance().listAllRings(getResources(), "call_");
		int currentRingIndex = 0;
		String currentRingName = prefs.getString(getString(R.string.sp_current_ringtone), "");

		if (lstRings == null || lstRings.size() == 0) {
			alertMsg(getString(R.string.alert_msg_norings));
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.alert_title));

			int size = lstRings.size();
			final String[] items = new String[size];
			for (int i = 0; i < size; i++) {
				items[i] = lstRings.get(i);
				if (items[i].equals(currentRingName)) {
					currentRingIndex = i;
				}
			}

			final RingDialogListener listener = new RingDialogListener(items);

			builder.setSingleChoiceItems(items, currentRingIndex, listener);
			builder.setNeutralButton(R.string.alert_btn_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					tvCurrentRingtone.setText(items[listener.getWhich()]);
					Editor editor = prefs.edit();
					editor.putString(getString(R.string.sp_current_ringtone),
							items[listener.getWhich()]);
					editor.commit();
					dialog.dismiss();
				}
			});
			Dialog d = builder.create();
			d.setOnDismissListener(listener);
			d.setOnShowListener(listener);
			d.show();
		}
	}

	private void alertMsg(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.alert_title));
		builder.setMessage(msg);
		builder.setNeutralButton(R.string.alert_btn_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

		Editor editor = prefs.edit();
		String key = "";
		if (buttonView == cbRing) {
			key = getString(R.string.sp_ring_enable);
			btnCallRing.setEnabled(isChecked);
		} else if (buttonView == cbVirabate) {
			key = getString(R.string.sp_virabate_enable);
		} else if (buttonView == cbAutoStart) {
			key = getString(R.string.sp_auto_start);
		} else if (buttonView == cbAutoRegister) {
			key = PTTConstant.SP_AUTOREGISTER;
		}
		editor.putBoolean(key, isChecked);
		editor.commit();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		pttUtil.printLog(bDebug, LOG_TAG, "onProgressChanged");
		seekBar.setProgress(progress);
		Editor editor = prefs.edit();
		if (seekBar == pttVolumeBar)
			editor.putInt(getString(R.string.sp_call_ptt_volume), progress);
		else if (seekBar == callVolumeBar) {
			editor.putInt(getString(R.string.sp_call_single_volume), progress);
		} else {
			editor.putInt(getString(R.string.sp_call_ring_volume), progress);
		}
		editor.commit();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		pttUtil.printLog(bDebug, LOG_TAG, "onStartTrackingTouch");
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		pttUtil.printLog(bDebug, LOG_TAG, "onStartTrackingTouch");
	}

	@Override
	protected void onResume() {
		super.onResume();
		instance = this;

		if (currentItemId == PTTConstant.SETTING_ITEM_TALKING) {
			if (PTTManager.getInstance().getCurrentPttState().getStatus() != PTTConstant.PTT_IDLE) {
				groupChoseSpinner.setEnabled(false);
				Toast.makeText(
						getApplicationContext(),
						getApplicationContext().getString(R.string.setting_current_group_off1)
								+ "\n"
								+ getApplicationContext().getString(
										R.string.setting_current_group_off2), Toast.LENGTH_LONG)
						.show();
			} else {
				groupChoseSpinner.setEnabled(true);
			}
		} else if (currentItemId == PTTConstant.SETTING_ITEM_ALERTRING) {
			int pttVolume = prefs.getInt(getString(R.string.sp_call_ptt_volume), 0);
			int ringVolume = prefs.getInt(getString(R.string.sp_call_ring_volume), 0);
			int callVolume = prefs.getInt(getString(R.string.sp_call_single_volume), 0);
			if (pttVolume != pttVolumeBar.getProgress())
				pttVolumeBar.setProgress(pttVolume);
			if (ringVolume != ringVolumeBar.getProgress())
				ringVolumeBar.setProgress(ringVolume);
			if (callVolume != callVolumeBar.getProgress())
				callVolumeBar.setProgress(callVolume);
		}
	}

}
