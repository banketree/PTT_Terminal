/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:WorkThread.java
 * Description:Called by Service
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-9
 */

package com.zzy.ptt.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.zzy.ptt.R;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.SipServer;
import com.zzy.ptt.model.SipUser;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class RegisterThread extends Thread {

	private static final String LOG_TAG = "RegisterThread";

	private SharedPreferences prefs;

	private Context context;

	private boolean bForceRedo = false;

	private PTTUtil pttUtil = PTTUtil.getInstance();

	public RegisterThread(Context context, boolean bForce) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		this.bForceRedo = bForce;
		this.context = context;
	}

	@Override
	public synchronized void run() {
		Log.d(LOG_TAG, "RegisterThread.run " + StateManager.getCurrentRegState());

		Looper.prepare();

		if (bForceRedo) {
			doRegister();
		} else {
			if (StateManager.getCurrentRegState() == EnumLoginState.ERROR_CODE_NUMBER
					|| StateManager.getCurrentRegState() == EnumLoginState.UNREGISTERED) {
				doRegister();
			}
		}
	}

	private synchronized int doRegister() {
        Log.d(LOG_TAG, "doRegister " + StateManager.getCurrentRegState());
        if(StateManager.getCurrentRegState() == EnumLoginState.REGISTERING){
            Log.d(LOG_TAG, "Register is in progress now, no need to reregister");
            return 0;
        }

		int result = PTTConstant.REG_ERROR;
		SipServer sipServer = PTTUtil.getInstance().getSipServerFromPrefs(prefs);
		SipUser sipUser = PTTUtil.getInstance().getSipUserFromPrefs(prefs);

		StateManager.setCurrentRegState(EnumLoginState.REGISTERING);
		try {
			result = SipProxy.getInstance().RequestRegister(sipServer, sipUser);
		} catch (PTTException e) {
			Log.e(LOG_TAG, "RequestRegister return : " + result);
		}
		Log.d(LOG_TAG, "RegisterThread get jni return value : " + result);
		
		if(PTTConstant.DUMMY) {
			result = PTTConstant.RETURN_SUCCESS;
			StateManager.setCurrentRegState(EnumLoginState.REGISTERE_SUCCESS);
			PTTService.instance.JNIUpdateRegisterState(EnumLoginState.REGISTERE_SUCCESS, PTTConstant.REG_ED, 200);
			return result;
		}

		// check result
		if (result != PTTConstant.RETURN_SUCCESS) {

			if ((sipServer != null) && (pttUtil.checkSipServerValid(sipServer.getServerIp()))
					&& (sipUser != null) && (pttUtil.checkSipUserValid(sipUser.getUsername()))
					&& (pttUtil.checkSipPasswordValid(sipUser.getPasswd()))) {
				pttUtil.regErrorAlert(-1, PTTService.instance.getString(R.string.alert_msg_register_fail));
			} else {
				if (!pttUtil.checkSettingIsOn()) {
					pttUtil.regErrorAlert(-1, PTTService.instance.getString(R.string.alert_msg_registerinfo_null));
				}
			}

			StateManager.setCurrentRegState(EnumLoginState.ERROR_CODE_NUMBER);
			Intent intent = new Intent(PTTConstant.ACTION_REGISTER);
			intent.putExtra(PTTConstant.KEY_REGISTER_STATE, PTTConstant.REG_ERROR);
			context.sendBroadcast(intent);
			return PTTConstant.REG_ERROR;
		}
		if(result == PTTConstant.RETURN_SUCCESS){
			Intent intent2 = new Intent("com.zzy.action.start");
			context.sendBroadcast(intent2);
		}
		return result;
	}
}
