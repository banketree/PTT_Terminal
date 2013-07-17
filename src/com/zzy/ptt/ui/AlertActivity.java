/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : AlertActivity.java
 * Author : LiXiaodong
 * Version : 1.0
 * Date : 2012-4-18
 */
package com.zzy.ptt.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.zzy.ptt.R;
import com.zzy.ptt.service.PTTManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.service.StateManager.EnumRegByWho;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author lxd
 * 
 */
public class AlertActivity extends Activity implements OnClickListener {

	private Button btnPositive;
	private TextView tvMsg, tvErrorcode;

	public static AlertActivity instance = null;

	private static int count = 1;
	private int currentError = -1;

	private static final String LOG_TAG = "AlertActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PTTUtil.getInstance().printLog(true, LOG_TAG, ">>>>>>>>> onCreate : ");
		instance = this;
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.alert_activity_layout);
		initWidgets();
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.alert_title_layout);
		setTitle(getString(R.string.alert_title));
	}

	private void initWidgets() {
		btnPositive = (Button) findViewById(R.id.btn_alert_positive);
		// btnNegative = (Button) findViewById(R.id.btn_alert_negative);
		tvMsg = (TextView) findViewById(R.id.tv_alert_msg);
		tvErrorcode = (TextView) findViewById(R.id.tv_alert_errorcode);

		btnPositive.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {

		/*
		 * switch (currentError) { case PTTConstant.ERROR_CODE_503: case
		 * PTTConstant.ERROR_CODE_120128: case PTTConstant.ERROR_CODE_408: if
		 * (currentError % 2 == 1) count++; break; default: break; }
		 */
		this.finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		PTTUtil.getInstance().printLog(true, LOG_TAG, ">>>>>>>>> onResume : ");
		PTTManager.getInstance().setbPttErrorShowing(true);
		Intent intent = getIntent();
		currentError = intent.getIntExtra(PTTConstant.KEY_ERROR_CODE, -1);
		updateMsg();
		checkSpecialError(currentError);
	}

	private void updateMsg() {
		Intent intent = getIntent();
		String errorMsg = intent.getStringExtra(PTTConstant.KEY_ERROR_MSG);
		boolean bError = intent.getBooleanExtra(PTTConstant.KEY_ERROR_OR_STATUS, false);
		if (errorMsg == null || errorMsg.trim().length() == 0) {
			errorMsg = String.valueOf(currentError);
		}
		String errorCodeLabel = (bError == true ? getString(R.string.alert_msg_error_code)
				: getString(R.string.alert_msg_status_code));
		String errorMsgLabel = (bError == true ? getString(R.string.alert_msg_error_msg)
				: getString(R.string.alert_msg_status_msg));

		tvMsg.setText(errorMsgLabel + " : " + errorMsg);
		tvErrorcode.setText(errorCodeLabel + " : " + currentError);

		PTTUtil.getInstance().printLog(true, LOG_TAG, currentError + ", " + errorMsg);
		if (currentError == -1) {
			tvErrorcode.setVisibility(View.GONE);
		}
		tvMsg.invalidate();
		tvErrorcode.invalidate();

	}

	private void checkSpecialError(int iErrorCode) {

		PTTUtil.getInstance().printLog(true, LOG_TAG,
				">>>>>>>>> checkSpecialError : " + iErrorCode + ", count : " + count);

		switch (iErrorCode) {
		case PTTConstant.ERROR_CODE_503:
		case PTTConstant.ERROR_CODE_120128:
		case PTTConstant.ERROR_CODE_408:
		case PTTConstant.ERROR_CODE_504:
			if (count % 2 == 1) {
				if (PTTUtil.getInstance().checkSettingIsOn() || StateManager.getRegStarter() == EnumRegByWho.REG_BY_CM) {
					count++;
					break;
				} else {
					StateManager.setRegStarter(EnumRegByWho.REG_BY_ERRORCODE);
					Intent intent = new Intent(PTTConstant.ACTION_REGISTER);
					intent.putExtra(PTTConstant.KEY_REREGISTER_FORCE, true);
					intent.setClass(AlertActivity.this, PTTService.class);
					startService(intent);
				}
			}
			count++;
			break;
		default:
			break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
		PTTManager.getInstance().setbPttErrorShowing(false);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			PTTManager.getInstance().setbPttErrorShowing(false);
			this.finish();
			return true;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	public static void restartCount() {
		if (count % 2 == 0)
			count++;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		setIntent(intent);
	}
}
