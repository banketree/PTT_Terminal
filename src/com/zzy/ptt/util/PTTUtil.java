/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:PTTUtil.java
 * Description:provide utility function and tools
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-2
 */
package com.zzy.ptt.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.model.SipServer;
import com.zzy.ptt.model.SipUser;
import com.zzy.ptt.service.GroupManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.service.StateManager.EnumRegByWho;
import com.zzy.ptt.ui.AlertActivity;
import com.zzy.ptt.ui.AlertDialogManager;
import com.zzy.ptt.ui.MainPageActivity;
import com.zzy.ptt.ui.SettingActivity;
import com.zzy.ptt.ui.SettingDetailActivity;

/**
 * @author Administrator
 * 
 */
public class PTTUtil {

	private static final String LOG_TAG = "PTTUtil";

	private static PTTUtil instance = new PTTUtil();

	private static boolean bDebugSwitch = true;

	// private static boolean bDebug = true;

	public static PTTUtil getInstance() {
		return instance;
	}

	private PTTUtil() {

	}

	/**
	 * Get the register state
	 * 
	 * @param state
	 * @return
	 */
	public int getTitleId(int state) {
		int titleId = 0;
		switch (state) {
		case PTTConstant.REG_NOT:
			titleId = R.string.register_not;
			break;
		case PTTConstant.REG_ING:
			titleId = R.string.register_ing;
			break;
		case PTTConstant.REG_ED:
			titleId = R.string.register_ed;
			break;
		case PTTConstant.REG_ERROR:
			titleId = R.string.register_error;
			break;
		default:
			titleId = R.string.register_not;
			break;
		}
		return titleId;
	}

	/**
	 * Get the register state
	 * 
	 * @param state
	 * @return
	 */
	public int getTitleId(EnumLoginState currentState) {
		int titleId = 0;
		switch (currentState) {
		case UNREGISTERED:
			titleId = R.string.register_not;
			break;
		case REGISTERING:
			titleId = R.string.register_ing;
			break;
		case REGISTERE_SUCCESS:
			titleId = R.string.register_ed;
			break;
		case ERROR_CODE_NUMBER:
			titleId = R.string.register_error;
			break;
		default:
			titleId = R.string.register_not;
			break;
		}
		return titleId;
	}

	/**
	 * Get the SipServer info from prefs
	 * 
	 * @return check the return if null
	 * @param prefs
	 */
	public SipServer getSipServerFromPrefs(SharedPreferences prefs) {

		SipServer sipServer = null;
		if (prefs == null) {
			return null;
		}

		sipServer = new SipServer();
		sipServer.setServerIp(prefs.getString(PTTConstant.SP_SERVERIP, ""));
		sipServer.setPort(prefs.getString(PTTConstant.SP_PORT, "5060"));
		return sipServer;
	}
	
	public boolean checkIP(String ipAddr){
		if(isStrBlank(ipAddr)){
			return false;
		}
		String regular = "\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b";
		Pattern pattern = Pattern.compile(regular);
		Matcher mat = pattern.matcher(ipAddr);
		return mat.matches();
	}

	/**
	 * Get the SipUser info from prefs
	 * 
	 * @param prefs
	 *            check the return if null
	 * @return
	 */
	public SipUser getSipUserFromPrefs(SharedPreferences prefs) {

		SipUser sipUser = null;
		if (prefs == null) {
			return null;
		}

		sipUser = new SipUser();
		sipUser.setUsername(prefs.getString(PTTConstant.SP_USERNAME, ""));
		sipUser.setPasswd(prefs.getString(PTTConstant.SP_PASSWORD, ""));
		return sipUser;
	}

	/**
	 * Check sip server setting valid
	 * 
	 * @param server
	 * @return true : valid false : invalid
	 */
	public boolean checkSipServerValid(String server) {
		boolean result = true;

		if (null == server || server.length() == 0) {
			return false;
		}
		return result;
	}

	/**
	 * Check sip user setting valid
	 * 
	 * @param user
	 * @return true : valid | false : invalid
	 */
	public boolean checkSipUserValid(String user) {
		boolean result = true;

		if (null == user || user.length() == 0) {
			return false;
		}
		return result;
	}

	/**
	 * Check sip password setting valid
	 * 
	 * @param password
	 * @return true : valid | false : invalid
	 */
	public boolean checkSipPasswordValid(String password) {
		boolean result = true;

		if (null == password || password.length() == 0) {
			return false;
		}
		return result;
	}

	/**
	 * Check number if valid
	 * 
	 * @param number
	 * @return true : valid | false : invalid
	 */
	public boolean checkNumber(String number) {
		boolean result = true;

		if (null == number || number.length() == 0) {
			return false;
		}
		return result;
	}

	public String duriation2String(long duration) {
		StringBuilder sb = new StringBuilder();
		long seconds = duration % 60;
		long minutes = (duration / 60) % 60;
		long hours = duration / 3600;
		if (hours <= 9) {
			sb.append("0");
		}
		sb.append(hours).append(":");
		if (minutes <= 9) {
			sb.append("0");
		}
		sb.append(minutes).append(":");
		if (seconds <= 9) {
			sb.append("0");
		}
		sb.append(seconds);
		return sb.toString();
	}

	/**
	 * 
	 * @return true:registered; false:unregistered
	 */
	public boolean isRegistered() {
		return StateManager.getCurrentRegState() == EnumLoginState.REGISTERE_SUCCESS;
	}

	/**
	 * 
	 * @return true:connected; false:disconnected
	 */
	/*
	 * public boolean isCmConnected() { return StateManager.getCmState() ==
	 * PTTConstant.CM_CONNECTED; }
	 */

	public boolean isAutoRegister(SharedPreferences prefs) {
		return prefs.getBoolean(PTTConstant.SP_AUTOREGISTER, true);
	}

	public void initOnCreat(Context context) {
		PTTService.activityList.add((Activity) context);
		AlertDialogManager.getInstance().setContext(context);
	}

	public String getCurrentUserName(Context context) {
		if(PTTConstant.DUMMY) {
			return "10008";
		}
		return PreferenceManager.getDefaultSharedPreferences(context).getString(PTTConstant.SP_USERNAME, "");
	}

	public String getFirstRingName(Resources res, String start) {
		AssetManager assetManager = res.getAssets();
		try {
			String[] files = assetManager.list("");
			if (files == null || files.length == 0) {
				return null;
			}
			for (int i = 0; i < files.length; i++) {
				if (files[i].startsWith(start)) {
					return files[i];
				}
			}

		} catch (IOException e) {
			Log.e(LOG_TAG, "Error to load assets");
			e.printStackTrace();
		}
		return null;
	}

	public AssetFileDescriptor getFirstRingFD(Resources res, String filename) {
		AssetFileDescriptor fd = null;

		if (filename == null) {
			return null;
		}

		AssetManager assetManager = res.getAssets();
		try {
			fd = assetManager.openFd(filename);
		} catch (IOException e) {
		}
		return fd;
	}

	public List<String> listAllRings(Resources res, String start) {
		List<String> lstRings = new ArrayList<String>();
		AssetManager assetManager = res.getAssets();
		try {
			String[] files = assetManager.list("");
			if (files == null || files.length == 0) {
				return lstRings;
			}
			for (int i = 0; i < files.length; i++) {
				if (files[i].startsWith(start)) {
					lstRings.add(files[i]);
				}
			}
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error to load assets");
			e.printStackTrace();
		}

		return lstRings;
	}

	public boolean checkCallIdValidInfo(int callid) {
		return callid == PTTConstant.INVALID_CALLID;
	}

	public void printLog(boolean debug, String logTag, String msg) {

		if (!bDebugSwitch) {
			return;
		}

		if (debug) {
			Log.d(logTag, "sxsexe>>"+msg);
		}
	}

	public GroupInfo getPttGroup() {
		GroupManager groupManager = GroupManager.getInstance();
		List<GroupInfo> groupInfos = groupManager.getGroupData();
		if (groupInfos == null || groupInfos.size() == 0) {
			return null;
		}

		return groupInfos.get(0);
	}

	public String getPttStatusString(int status) {
		String strStatus = "PTT_STATUS_UNKNOWN";
		switch (status) {
		case PTTConstant.PTT_IDLE:
			strStatus = "PTT_IDLE";
			break;
		case PTTConstant.PTT_APPLYING:
			strStatus = "PTT_APPLYING";
			break;
		case PTTConstant.PTT_ACCEPTED:
			strStatus = "PTT_ACCEPTED";
			break;
		case PTTConstant.PTT_CANCELED:
			strStatus = "PTT_CANCELED";
			break;
		case PTTConstant.PTT_CANCELING:
			strStatus = "PTT_CANCELING";
			break;
		case PTTConstant.PTT_CLOSED:
			strStatus = "PTT_CLOSED";
			break;
		case PTTConstant.PTT_FREE:
			strStatus = "PTT_FREE";
			break;
		case PTTConstant.PTT_LISTENING:
			strStatus = "PTT_LISTENING";
			break;
		case PTTConstant.PTT_REJECTED:
			strStatus = "PTT_REJECTED";
			break;
		case PTTConstant.PTT_SPEAKING:
			strStatus = "PTT_SPEAKING";
			break;
		case PTTConstant.PTT_WAITING:
			strStatus = "PTT_WAITING";
			break;
		 case PTTConstant.PTT_INIT:
		 strStatus = "PTT_INIT";
		 break;
		case PTTConstant.PTT_BUSY:
			strStatus = "PTT_ACCEPTED";
			break;
		case PTTConstant.PTT_FORBIDDEN:
			strStatus = "PTT_FORBIDDEN";
			break;
		default:
			break;
		}
		return strStatus;
	}

	public String getCallStateString(int status) {
		String strStatus = "CALL_STATE_UNKNOWN";
		switch (status) {
		case PTTConstant.CALL_CANCELING:
			strStatus = "CALL_CANCELING";
			break;
		case PTTConstant.CALL_DIALING:
			strStatus = "CALL_DIALING";
			break;
		case PTTConstant.CALL_ENDED:
			strStatus = "CALL_ENDED";
			break;
		case PTTConstant.CALL_ENDING:
			strStatus = "CALL_ENDING";
			break;
		case PTTConstant.CALL_ERROR:
			strStatus = "CALL_ENDING";
			break;
		case PTTConstant.CALL_IDLE:
			strStatus = "CALL_IDLE";
			break;
		case PTTConstant.CALL_REJECTING:
			strStatus = "CALL_REJECTING";
			break;
		case PTTConstant.CALL_RINGBACK:
			strStatus = "CALL_RINGBACK";
			break;
		case PTTConstant.CALL_RINGING:
			strStatus = "CALL_RINGING";
			break;
		case PTTConstant.CALL_TALKING:
			strStatus = "CALL_TALKING";
			break;
		default:
			break;
		}
		return strStatus;
	}

	public String getRegStateString(int status) {
		String strStatus = "REG_STATUS_UNKNOWN";
		switch (status) {
		case PTTConstant.REG_ED:
			strStatus = "REG_ED";
			break;
		case PTTConstant.REG_ERROR:
			strStatus = "REG_ERROR";
			break;
		case PTTConstant.REG_ING:
			strStatus = "REG_ERROR";
			break;
		case PTTConstant.REG_NOT:
			strStatus = "REG_NOT";
			break;
		default:
			break;
		}
		return strStatus;
	}

	public String getRegStateString(EnumLoginState status) {
		String strStatus = "REG_STATUS_UNKNOWN";
		switch (status) {
		case UNREGISTERED:
			strStatus = "UNREGISTERED";
			break;
		case ERROR_CODE_NUMBER:
			strStatus = "ERROR_CODE_NUMBER";
			break;
		case REGISTERE_SUCCESS:
			strStatus = "REGISTERE_SUCCESS";
			break;
		case REGISTERING:
			strStatus = "REGISTERING";
			break;
		default:
			break;
		}
		return strStatus;
	}

	public String getCommandString(int command) {
		String strStatus = "PTT_COMM_UNKNOWN";
		switch (command) {
		case PTTConstant.COMM_APPLY_PTT_RIGHT:
			strStatus = "COMM_APPLY_PTT_RIGHT";
			break;
		case PTTConstant.COMM_CANCEL_PTT_RIGHT:
			strStatus = "COMM_CANCEL_PTT_RIGHT";
			break;
		case PTTConstant.COMM_HANGUP_PTT_CALL:
			strStatus = "COMM_HANGUP_PTT_CALL";
			break;
		// case PTTConstant.COMM_MAKE_PTT_CALL:
		// strStatus = "COMM_MAKE_PTT_CALL";
		// break;
		case PTTConstant.COMM_OPEN_PTT_UI:
			strStatus = "COMM_OPEN_PTT_UI";
			break;
		case PTTConstant.COMM_START_APP:
			strStatus = "COMM_START_APP";
			break;
		case PTTConstant.COMM_UPDATE_PTT_UI:
			strStatus = "COMM_UPDATE_PTT_UI";
			break;
		case PTTConstant.COMM_CM_DISCONNECT:
			strStatus = "COMM_CM_DISCONNECT";
			break;
		case PTTConstant.COMM_CM_CONNECT:
			strStatus = "COMM_CM_CONNECT";
			break;
		case PTTConstant.COMM_CLOSE_PTT:
			strStatus = "COMM_CLOSE_PTT";
			break;
		case PTTConstant.COMM_MAKE_CALL:
			strStatus = "COMM_MAKE_CALL";
			break;
		default:
			break;
		}
		return strStatus;
	}

	public String getARString(int arRoute) {
		String strStatus = "AR_UNKNOWN";
		switch (arRoute) {
		case PTTConstant.AR_EARPHONE:
			strStatus = "PTTConstant.AR_EARPHONE";
			break;
		case PTTConstant.AR_HANDSET:
			strStatus = "PTTConstant.AR_HANDSET";
			break;
		case PTTConstant.AR_SPEAKER:
			strStatus = "PTTConstant.AR_SPEAKER";
			break;
		default:
			strStatus = "AR_UNKNOWN";
			break;
		}
		return strStatus;
	}

	/**
	 * 
	 * @param context
	 * @param errorCode
	 * @param errroMsg
	 * @param bError
	 *            true : error , false : status
	 */
	public void errorAlert(final Context context, final int errorCode, final String errroMsg, final boolean bError) {
		boolean appStatrt = checkAppStarted();
		boolean isBackground = checkIsBackground();
		
		printLog(true, "PTTUtil", "start activity AlertActivity " + appStatrt + " isBackground " + isBackground + " exit " + StateManager.exitFlag);
		if (appStatrt) {
			return;
		}

//		if (isBackground) {
//			return;
//		}
		
		if(StateManager.exitFlag)
			return;

		String msg = getErrorMsg(errorCode);
		if (msg == null) {
			msg = errroMsg;
		}
		
		Intent intent = new Intent();
		intent.setClass(context, AlertActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(PTTConstant.KEY_ERROR_CODE, errorCode);
		intent.putExtra(PTTConstant.KEY_ERROR_MSG, msg);
		intent.putExtra(PTTConstant.KEY_ERROR_OR_STATUS, bError);
		context.startActivity(intent);

	}

	public void msgAlert(final Context context, final String errroMsg, final boolean bError) {
		if (checkAppStarted()) {
			return;
		}

		if (checkIsBackground()) {
			return;
		}

		printLog(true, "PTTUtil", "start activity AlertActivity");
		Intent intent = new Intent();
		intent.setClass(context, AlertActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(PTTConstant.KEY_ERROR_MSG, errroMsg);
		intent.putExtra(PTTConstant.KEY_ERROR_OR_STATUS, bError);
		context.startActivity(intent);

	}

	public void errorToast(Context context, int errorCode, String errorMsg) {
		Toast.makeText(context, errorCode + " " + errorMsg, Toast.LENGTH_LONG).show();
	}

	public void msgToast(Context context, String msg) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}

	public boolean checkAppStarted() {
		return PTTService.instance == null;
	}

	public boolean checkIsBackground() {
		return MainPageActivity.instance == null;
	}

	public boolean checkIfSpeaker(String speaker) {
		if (isStrBlank(speaker)) {
			return false;
		}
		String user = getSipUserFromPrefs(PTTService.instance.prefs).getUsername();
		return speaker.equals(user);
	}

	public String getErrorMsg(int errorCode) {
		String strMsg = null;

		Context context = PTTService.instance;

		switch (errorCode) {
		case PTTConstant.ERROR_CODE_401:
			strMsg = context.getString(R.string.alert_msg_401);
			break;
		case PTTConstant.ERROR_CODE_403:
			strMsg = context.getString(R.string.alert_msg_403);
			break;
		case PTTConstant.ERROR_CODE_408:
			strMsg = context.getString(R.string.alert_msg_408);
			break;
		case PTTConstant.ERROR_CODE_481:
			strMsg = context.getString(R.string.alert_msg_481);
			break;
		case PTTConstant.ERROR_CODE_500:
			strMsg = context.getString(R.string.alert_msg_500);
			break;
		case PTTConstant.ERROR_CODE_503:
			strMsg = context.getString(R.string.alert_msg_503);
			break;
		case PTTConstant.ERROR_CODE_502:
			strMsg = context.getString(R.string.alert_msg_502);
			break;
		case PTTConstant.ERROR_CODE_486:
			strMsg = context.getString(R.string.alert_msg_486);
			break;
		case PTTConstant.ERROR_CODE_504:
			strMsg = context.getString(R.string.alert_msg_504);
			break;
		default:
			break;
		}
		return strMsg;
	}

	public GroupInfo getCurrentGroupInfo() {
		GroupInfo groupInfo = null;
		String num = PTTService.instance.prefs.getString(PTTConstant.SP_CURR_GRP_NUM, null);
		if (num == null || num.trim().length() == 0) {
			return null;
		}

		List<GroupInfo> lstGroupData = GroupManager.getInstance().getGroupData();
		if (lstGroupData == null || lstGroupData.size() == 0) {
			return null;
		}
		int iSize = lstGroupData.size();
		for (int i = 0; i < iSize; i++) {
			GroupInfo groupInfo2 = lstGroupData.get(i);
			if (num.equals(groupInfo2.getNumber())) {
				groupInfo = new GroupInfo();
				groupInfo.setNumber(num);
				groupInfo.setName(groupInfo2.getName());
				groupInfo.setEmergency(groupInfo2.isEmergency());
				groupInfo.setLevel(groupInfo2.getLevel());
				return groupInfo;
			}
		}
		return groupInfo;
	}

	public void setCurrentGrp(String grpNum) {
		if (PTTService.instance != null) {
			Editor editor = PTTService.instance.prefs.edit();
			editor.putString(PTTConstant.SP_CURR_GRP_NUM, grpNum);
			editor.commit();
		}
	}

	public boolean checkIsGroupNum(String callNum) {
		if (callNum == null || callNum.trim().length() == 0) {
			return false;
		}
		List<GroupInfo> lstGroupData = GroupManager.getInstance().getGroupData();
		if (lstGroupData == null || lstGroupData.size() == 0) {
			printLog(true, LOG_TAG, "-----------Find Group Data is NULL-----------");
			return false;
		}
		int iSize = lstGroupData.size();
		printLog(true, LOG_TAG, "-----------Find Group Data is " + lstGroupData);
		for (int i = 0; i < iSize; i++) {
			GroupInfo groupInfo2 = lstGroupData.get(i);
			if (groupInfo2 != null && callNum.equals(groupInfo2.getNumber())) {
				return true;
			}
		}
		return false;
	}

	public boolean checkSettingIsOn() {
		return SettingActivity.instance != null || SettingDetailActivity.instance != null;
	}

	public void regErrorAlert(int errorCode, String errorMessage) {
		EnumRegByWho regStarted = StateManager.getRegStarter();
		boolean bShowEnable = false;

		printLog(true, LOG_TAG, "EnumRegByWho : " + regStarted);

		switch (regStarted) {
		case REG_BY_ERRORCODE:
		case REG_BY_CM:
			if (checkSettingIsOn()) {
				// no need to show error alert
				bShowEnable = false;
			} else {
				bShowEnable = true;
			}
			break;
		case REG_BY_USER:
			bShowEnable = true;
			break;
		default:
			break;
		}
		printLog(true, LOG_TAG, "bShowEnable : " + bShowEnable);
		if (bShowEnable) {
			errorAlert(PTTService.instance, errorCode, errorMessage, true);
		}
	}

	public long generateRandom(int max) {
		return (long) (Math.random() * max);
	}

	public String longToIp(long ip) {
		StringBuffer ret = new StringBuffer();
		long temp = 0;
		temp = (ip >> 24) & 0xff;
		ret.append(temp).append(".");
		temp = (ip >> 16) & 0xff;
		ret.append(temp).append(".");
		temp = (ip >> 8) & 0xff;
		ret.append(temp).append(".");
		temp = ip & 0xff;
		ret.append(temp);
		return ret.toString();
	}

	public long ipToLong(String ip) {
		if (ip == null || ip.length() == 0) {
			return 0;
		}
		String[] temp = ip.trim().split("\\.");
		if (temp.length != 4) {
			return 0;
		}
		long ret = 0;
		try {
			for (int i = 0; i < temp.length; i++) {
				ret += Long.parseLong(temp[i]) << ((3 - i) * 8);
			}
			return ret;
		} catch (Exception ex) {
			return 0;
		}
	}

	public boolean isStrBlank(String str) {
		if (str == null || str.trim().length() == 0)
			return true;
		else
			return false;
	}
}
