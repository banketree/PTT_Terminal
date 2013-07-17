/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:DialActivity.java
 * Description:TODO
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-5
 */

package com.zzy.ptt.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.CallState;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.NumberInfo;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.service.CallStateManager;
import com.zzy.ptt.service.DailService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class DialActivity extends BaseActivity implements OnClickListener {

	private static final String LOG_TAG = "DialActivity";

	private static DialActivity instance;

	private ImageView btnNum1, btnNum2, btnNum3, btnNum4, btnNum5, btnNum6,
			btnNum7, btnNum8, btnNum9, btnNum0, btnNumStart, btnNumPound,
			btnNumDelete, btnSoundCall, BtnSearch;
	private EditText dailNumEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_calling_main);

		PTTUtil.getInstance().initOnCreat(this);

		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		instance = this;
		initWeigets();
	}

	public void initWeigets() {
		btnNum1 = (ImageView) findViewById(R.id.pone);
		btnNum2 = (ImageView) findViewById(R.id.ptwo);
		btnNum3 = (ImageView) findViewById(R.id.pthree);
		btnNum4 = (ImageView) findViewById(R.id.pfour);
		btnNum5 = (ImageView) findViewById(R.id.pfive);
		btnNum6 = (ImageView) findViewById(R.id.psix);
		btnNum7 = (ImageView) findViewById(R.id.pseven);
		btnNum8 = (ImageView) findViewById(R.id.penight);
		btnNum9 = (ImageView) findViewById(R.id.pnine);
		btnNumStart = (ImageView) findViewById(R.id.pmi);
		btnNum0 = (ImageView) findViewById(R.id.p0);
		btnNumPound = (ImageView) findViewById(R.id.pjing);
		btnNumDelete = (ImageView) findViewById(R.id.pdel);
		btnSoundCall = (ImageView) findViewById(R.id.pphone);
		BtnSearch = (ImageView) findViewById(R.id.psearch);

		dailNumEditText = (EditText) findViewById(R.id.p_digits);
		int oldnum = DailService.getInstance().strNumber;
		if (oldnum != -1) {
			String str = Integer.toString(oldnum);
			dailNumEditText.setText(str);
			dailNumEditText.setSelection(str.length());
			DailService.getInstance().strNumber = -1;
		}

		btnNum1.setOnClickListener(this);
		btnNum2.setOnClickListener(this);
		btnNum3.setOnClickListener(this);
		btnNum4.setOnClickListener(this);
		btnNum5.setOnClickListener(this);
		btnNum6.setOnClickListener(this);
		btnNum7.setOnClickListener(this);
		btnNum8.setOnClickListener(this);
		btnNum9.setOnClickListener(this);
		btnNum0.setOnClickListener(this);
		btnNumStart.setOnClickListener(this);
		btnNumPound.setOnClickListener(this);
		btnNumDelete.setOnClickListener(this);
		btnSoundCall.setOnClickListener(this);
		BtnSearch.setOnClickListener(this);
	}

	private void makeCall() {

		final String strNum = dailNumEditText.getText().toString();
		Log.d(LOG_TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>Dial num : " + strNum);
		// check number
		if (!PTTUtil.getInstance().checkNumber(strNum)) {
			AlertDialogManager.getInstance().toastNumberNull();
			return;
		}

		CallState myCallState = new CallState();
		myCallState.setNumber(strNum);
		myCallState.setCallState(PTTConstant.CALL_DIALING);
		CallStateManager.getInstance().setCurrentCallState(myCallState);

		Intent intent = new Intent();
		intent.setClass(this, InCallScreenActivity.class);
		startActivity(intent);

		NumberInfo numberInfo = new NumberInfo();
		numberInfo.setNumber(strNum);
		numberInfo.setNumberType(PTTConstant.NUMBER_DIGIT);
		numberInfo.setPttType(PTTConstant.PTT_SINGLE);

		try {
			SipProxy.getInstance().RequestMakeCall(numberInfo);
		} catch (PTTException e) {
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			Log.d(LOG_TAG, "onKeyDown  KEYCODE_BACK");
			super.onKeyDown(keyCode, event);
			this.finish();
			return true;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {

		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			if (event.getRepeatCount() == 0) {
				Log.d(LOG_TAG,
						"onKeyDown  KEYCODE_BACK keyCode" + event.getKeyCode());
				// 具体的操作代码
				StringBuilder sb = new StringBuilder();
				sb.append(dailNumEditText.getText().toString());
				switch (event.getKeyCode()) {
				case 8:
					sb.append("1");
					break;
				case 9:
					sb.append("2");
					break;
				case 10:
					sb.append("3");
					break;
				case 11:
					sb.append("4");
					break;
				case 12:
					sb.append("5");
					break;
				case 13:
					sb.append("6");
					break;
				case 14:
					sb.append("7");
					break;
				case 15:
					sb.append("8");
					break;
				case 16:
					sb.append("9");
					break;
				case 7:
					sb.append("0");
					break;
				case 17:
					sb.append("*");
					break;
				case 18:
					sb.append("#");
					break;
				case 5:
					makeCall();
					break;
				default:
					return super.dispatchKeyEvent(event);
				}
				dailNumEditText.setText(sb);
				dailNumEditText.setSelection(sb.length());
				return true;
			}
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (instance == null) {
			instance = this;
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder();
		sb.append(dailNumEditText.getText().toString());
		switch (v.getId()) {
		case R.id.pone:
			sb.append("1");
			dailNumEditText.setText(sb);
			break;
		case R.id.ptwo:
			sb.append("2");
			dailNumEditText.setText(sb);
			break;
		case R.id.pthree:
			sb.append("3");
			dailNumEditText.setText(sb);
			break;
		case R.id.pfour:
			sb.append("4");
			dailNumEditText.setText(sb);
			break;
		case R.id.pfive:
			sb.append("5");
			dailNumEditText.setText(sb);
			break;
		case R.id.psix:
			sb.append("6");
			dailNumEditText.setText(sb);
			break;
		case R.id.pseven:
			sb.append("7");
			dailNumEditText.setText(sb);
			break;
		case R.id.penight:
			sb.append("8");
			dailNumEditText.setText(sb);
			break;
		case R.id.pnine:
			sb.append("9");
			dailNumEditText.setText(sb);
			break;
		case R.id.p0:
			sb.append("0");
			dailNumEditText.setText(sb);
			break;
		case R.id.pmi:
			sb.append("*");
			dailNumEditText.setText(sb);
			break;
		case R.id.pjing:
			sb.append("#");
			dailNumEditText.setText(sb);
			break;
		case R.id.pphone:
			if (StateManager.getCurrentRegState() == EnumLoginState.REGISTERE_SUCCESS) {
				makeCall();
			} else {
				Toast.makeText(instance, getString(R.string.register_not),
						Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.psearch:
			Intent intent = new Intent(DialActivity.this, PBKActivity.class);
			intent.putExtra("getnum", 852);
			startActivityForResult(intent, 200);
			break;
		case R.id.pdel:
			if (sb.length() != 0) {
				sb.deleteCharAt(sb.length() - 1);
				dailNumEditText.setText(sb);
			}
			break;

		default:
			break;
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == 200) {
			String phonenum = data.getStringExtra("phonenum");
			dailNumEditText.setText(phonenum);
		}
	}
}
