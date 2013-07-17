/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:PTTKeyReceiver.java
 * DescriptionTTReceiver
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-17
 */

package com.zzy.ptt.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.zzy.ptt.R;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.service.StateManager.EnumRegByWho;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class PTTCmReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = "PTTCmReceiver";

	private Context context;

	private static boolean bAutoStartFlag = false;
	private static boolean bDebug = true;

	private PTTUtil pttUtil = PTTUtil.getInstance();

	@Override
	public void onReceive(Context context, Intent intent) {

		this.context = context;

		String action = null;
		if (intent != null)
			action = intent.getAction();

		if (action == null || action.length() == 0) {
			return;
		}
		
		handleBootCompletedEvent(action);

	}

	private void handleBootCompletedEvent(String action) {
		// auto start
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			bAutoStartFlag = true;
		}
	}
}
