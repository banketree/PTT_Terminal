/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:SipProxy.java
 * Description:PTTService 
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-6
 */
package com.zzy.ptt.proxy;

import java.util.List;

import android.content.Context;
import android.util.Log;

import com.zzy.ptt.R;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.jni.CallJni;
import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.model.NumberInfo;
import com.zzy.ptt.model.SipServer;
import com.zzy.ptt.model.SipUser;
import com.zzy.ptt.service.CallStateManager;
import com.zzy.ptt.service.GroupManager;
import com.zzy.ptt.service.PTTManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.ui.AlertActivity;
import com.zzy.ptt.ui.PTTActivity;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class SipProxy implements InterfaceSip {

	private static final String LOG_TAG = "SipProxy";

	private static SipProxy instance = new SipProxy();

	private PTTUtil pttUtil = PTTUtil.getInstance();
	private static final boolean bDebug = true;

	private SipProxy() {
	}

	public static SipProxy getInstance() {
		return instance;
	}

	@Override
	public int RequestMakeCall(NumberInfo numberInfo) throws PTTException {

		if (!checkValidation(false)) {
			return -1;
		}

		// in case pttactivtiy is still open
		if (PTTActivity.instance != null) {
			PTTActivity.instance.finish();
		}

		CallStateManager.getInstance().setbForceInSC(true);
		pttUtil.printLog(bDebug, LOG_TAG, "call State isbForceInSC: "
				+ CallStateManager.getInstance().isbForceInSC());

		CallStateManager.getInstance().setAR();
//		PTTService.instance.wakeAudio();
		int result = CallJni.makeCall(numberInfo);
		pttUtil.printLog(bDebug, LOG_TAG, "RequestMakeCall: " + numberInfo.getNumber() + ",result:"
				+ result);
		checkResult(result, PTTService.instance.getString(R.string.alert_msg_outingcall_fail));
		return result;
	}

	private void checkResult(int result, String errorMsg) {
		String msg = errorMsg;
		Context context = PTTService.instance;
		PTTManager pttManager = PTTManager.getInstance();
		if (result != PTTConstant.RETURN_SUCCESS) {
			pttManager.getCurrentPttState().setSpeakerNum(null);
			pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
			switch (result) {
			case PTTConstant.ERROR_CODE_MEDIA_FAIL:
				msg = context.getString(R.string.alert_msg_media_fail);
				break;
			case PTTConstant.ERROR_CODE_70010:
				pttManager.setbPttErrorShowing(true);
				msg = context.getString(R.string.alert_msg_fail_ptt);
				break;
			case PTTConstant.ERROR_CODE_120128:
				pttManager.setbPttErrorShowing(true);
				msg = context.getString(R.string.alert_msg_503);
				break;
			default:
				break;
			}
			pttUtil.errorAlert(PTTService.instance, result, msg, true);
		}
	}

	@Override
	public int RequestAnswer(int callId) throws PTTException {
		int result = CallJni.answer(callId);
		Log.d(LOG_TAG, "SipProxy.RequestAnswer has been revoked, callid : " + callId);
		return result;

	}

	@Override
	public int RequestHangup(int callId) throws PTTException {
		if (callId < 0) {
			return 0;
		}
		int result = CallJni.hangup(callId);
		Log.d(LOG_TAG, "SipProxy.RequestHangup has been revoked, callid : " + callId);
		return result;
	}

	@Override
	public int RequestRegister(SipServer sipServer, SipUser sipUser) throws PTTException {

		Log.d(LOG_TAG, "server : " + sipServer.getServerIp() + " port : " + sipServer.getPort());
		Log.d(LOG_TAG, "user : " + sipUser.getUsername() + " password : " + sipUser.getPasswd());

		int result = CallJni.login(sipServer, sipUser);
		Log.d(LOG_TAG, "SipProxy.RequestRegister has been revoked");
		return result;

	}

	@Override
	public int RequestInit() throws PTTException {
		int result = CallJni.init();
		Log.d(LOG_TAG, "SipProxy.RequestInit has been revoked");
		return result;
	}

	@Override
	public int RequestDestory() throws PTTException {
		int result = CallJni.destroy();
		Log.d(LOG_TAG, "SipProxy.RequestDestory has been revoked");
		return result;
	}

	@Override
	public int RequestSendMessage(String number, String message) throws PTTException {
		if (!checkValidation(false)) {
			return -1;
		}
		Log.d(LOG_TAG, "SipProxy.RequestSendMessage number : " + number + ", message : " + message);
		int result = CallJni.sendMessage(number, message);
		return result;
	}

	@Override
	public int RequestGetMember(String groupNumber) throws PTTException {
		if (!checkValidation(false)) {
			return -1;
		}
		Log.d(LOG_TAG, "SipProxy.RequestGetMember groupNumber : " + groupNumber);
		if (groupNumber == null || groupNumber.trim().length() == 0) {
			Log.e(LOG_TAG, "groupNumber must not be NULL");
			return -1;
		}
		int result = CallJni.getMember(groupNumber);
		return result;
	}

	@Override
	public int RequestApplyPttRight(String groupNumber) throws PTTException {
		if (!checkValidation(true)) {
			return -1;
		}

		if (groupNumber == null || groupNumber.trim().length() == 0) {
			PTTManager.getInstance().setbPttErrorShowing(true);
			pttUtil.errorAlert(PTTService.instance, -1,
					PTTService.instance.getString(R.string.alert_msg_no_group), true);
		}

		Log.d(LOG_TAG, "SipProxy.RequestApplyPttRight");
		int result = CallJni.applyPttRight(groupNumber);
		checkResult(result, PTTService.instance.getString(R.string.alert_msg_fail_ptt));
		return result;
	}

	//
	// private void sendErrorBroadcast(int errorCode, String error_msg) {
	// Intent intent = new Intent(PTTConstant.ACTION_ALERT);
	// intent.putExtra("error_code", errorCode);
	// intent.putExtra("error_msg", error_msg);
	// PTTService.instance.sendBroadcast(intent);
	// }

	@Override
	public int RequestCancelPttRight(String groupNumber) throws PTTException {
		Log.d(LOG_TAG, "SipProxy.RequestCancelPttRight");
		int result = CallJni.cancelPttRight(groupNumber);
		// checkResult(result, "RequestCancelPttRight");
		/*
		 * if (result != PTTConstant.RETURN_SUCCESS) { Log.e(LOG_TAG,
		 * "RequestCancelPttRight , result : " + result); throw new
		 * PTTException("RequestCancelPttRight , result : " + result); }
		 */
		checkResult(result, PTTService.instance.getString(R.string.alert_msg_fail_ptt));
		return result;
	}

	@Override
	public int RequestExitPttUIManually(String groupNumber) throws PTTException {
		Log.d(LOG_TAG, "SipProxy.RequestExitPttUIManually");
		int result = CallJni.exitPttUIManually(groupNumber);
		return result;
	}

	@Override
	public int RequestOpenPtt() throws PTTException {
		Log.d(LOG_TAG, "SipProxy.RequestOpenPtt");
		int result = CallJni.openPtt();
		Log.d(LOG_TAG, "SipProxy.RequestOpenPtt, reslut : " + result);
		if (result != PTTConstant.RETURN_SUCCESS) {
			Log.e(LOG_TAG, "RequestOpenPtt , result : " + result);
			throw new PTTException("RequestOpenPtt , result : " + result);
		}
		return result;
	}

	public int RequestMakePttCall(NumberInfo numberInfo) {
		if (!checkValidation(true)) {
			return -1;
		}

		int result = CallJni.makeCall(numberInfo);
		checkResult(result, PTTService.instance.getString(R.string.alert_msg_fail_ptt));
		return result;
	}

	private boolean checkValidation(boolean bCheckGroup) {
		boolean bRegister = PTTUtil.getInstance().isRegistered();
		// boolean bCmConn = PTTUtil.getInstance().isCmConnected();

		Context context = PTTService.instance;

		if (context == null) {
			return false;
		}

		pttUtil.printLog(bDebug, LOG_TAG, "checkValidation, cm : " + /* bCmConn + */", register : "
				+ bRegister + ",AlertActivity instance " + AlertActivity.instance);
		/*
		 * if (!bCmConn) { pttUtil.errorAlert(context, 0,
		 * context.getString(R.string.alert_msg_cm_disconnected), true); return
		 * false; }
		 */
		if (!bRegister) {
			pttUtil.errorAlert(context, 0, context.getString(R.string.register_not), true);
			return false;
		}

		if (bCheckGroup) {
			if (!checkGroupExist()) {
				pttUtil.errorAlert(context, 0, context.getString(R.string.alert_msg_no_group), true);
				return false;
			}
		}
		return true;
	}

	private boolean checkGroupExist() {
		List<GroupInfo> groupData = GroupManager.getInstance().getGroupData();
		if (groupData == null || groupData.size() == 0) {
			return false;
		}
		return true;
	}
	
	@Override
	public int RequestHoInd(String ip) throws PTTException {
		Log.d(LOG_TAG, "SipProxy.RequestHoInd" );
		int result = CallJni.hoInd(ip);
		return result;
	}

	@Override
	public int RequestGroupNo(String anyGroupNo) {
		Log.d(LOG_TAG, "SipProxy.RequestGroupNo  " + anyGroupNo);
		return CallJni.getGroupNo(anyGroupNo);
	}

	@Override
	public int RequestSendDTMF(String digit) {
		Log.d(LOG_TAG, "SipProxy.ReuqestSendDTMF  " + digit);
		return CallJni.sendDTMF(digit);
	}
}
