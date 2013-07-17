/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : PTTManager.java
 * Author : LiXiaodong
 * Version : 1.0
 * Date : 2012-4-17
 */
package com.zzy.ptt.service;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.zzy.ptt.R;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.model.PttGroupStatus;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.ui.PTTActivity;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author lxd
 * 
 */
public class PTTManager {

	private static final String LOG_TAG = "PTTManager";

	private static PTTManager instance = new PTTManager();

	private int callState;

	private PTTUtil pttUtil = PTTUtil.getInstance();
	private static final boolean bDebug = true;

	public boolean bPttAlertPlayed = false;

	private boolean bPttErrorShowing = false;
	
	private static final String DEFAULT_DST = "123465";

	private AudioManager audioManager;

	public enum PTT_KEY {
		PTT_KEY_DOWN, PTT_KEY_UP
	};

	public PTT_KEY ePttKeyStatus = PTT_KEY.PTT_KEY_UP;

	private PTTManager() {
		pttGroupStatus = new PttGroupStatus();
	}

	public static PTTManager getInstance() {
		if (instance == null) {
			instance = new PTTManager();
		}
		return instance;
	}

	private PttGroupStatus pttGroupStatus;

	private boolean debug = true;

	public void setCurrentPttState(PttGroupStatus srcPttGroupStatus) {
		if (srcPttGroupStatus == null) {
			return;
		}
		pttGroupStatus.setSeqCallId(srcPttGroupStatus.getSeqCallId());
		pttGroupStatus.setGroupNum(srcPttGroupStatus.getGroupNum());
		pttGroupStatus.setSpeakerNum(srcPttGroupStatus.getSpeakerNum());
		pttGroupStatus.setStatus(srcPttGroupStatus.getStatus());
		pttGroupStatus.setStartTime(srcPttGroupStatus.getStartTime());
		pttGroupStatus.setSpeakerStartTime(srcPttGroupStatus.getSpeakerStartTime());
		pttGroupStatus.setCurrentWaitingPos(srcPttGroupStatus.getCurrentWaitingPos());
		pttGroupStatus.setTotalWaitingCount(srcPttGroupStatus.getTotalWaitingCount());
		pttGroupStatus.setRejectReason(srcPttGroupStatus.getRejectReason());
		Log.d(LOG_TAG, "setCurrentPttState : " + pttGroupStatus.toString());
	}

	public PttGroupStatus getCurrentPttState() {
		return pttGroupStatus;
	}

	// when exit the group activity
	public void clear() {
		pttUtil.printLog(debug, LOG_TAG, "---------------------PttManager.clear()");
		pttGroupStatus = new PttGroupStatus();
		ePttKeyStatus = PTT_KEY.PTT_KEY_UP;
		//bPttAlertPlayed = false;
		//check audio state
		Log.d(LOG_TAG, "clear : " + pttGroupStatus.toString());
	}

	/*
	 * private void printAllGrpNumMembers() {
	 * 
	 * Set<String> keys = groupNumMebersMap.keySet(); Iterator<String> iterator
	 * = keys.iterator(); while (iterator.hasNext()) { String groupNum =
	 * (String) iterator.next(); Log.d(LOG_TAG, "****** get group : " +
	 * groupNum); MemberInfo[] members = groupNumMebersMap.get(groupNum); for
	 * (int k = 0; k < members.length; k++) { Log.d(LOG_TAG,
	 * "----- get member : " + k + " " + members[k]); } } }
	 */

	public int applyPttRight() {
		int result = -1;
		String grpNum = pttGroupStatus.getGroupNum();
		if (grpNum == null || grpNum.trim().length() == 0) {
			GroupInfo groupInfo = pttUtil.getCurrentGroupInfo();
			if (groupInfo == null) {
				return -1;
			} else {
				grpNum = groupInfo.getNumber();
			}
		}
		
		pttGroupStatus.setGroupNum(grpNum);
		try {
			result = SipProxy.getInstance().RequestApplyPttRight(grpNum);
			pttUtil.printLog(bDebug, LOG_TAG, "applyPttRight " + grpNum + ", result : " + result);
		} catch (PTTException e) {
			pttUtil.printLog(bDebug, LOG_TAG, "error to applyPttRight , result : " + result);
		}
		return result;
	}

	public int cancelPttRight() {
		int result = -1;
		GroupInfo groupInfo = pttUtil.getPttGroup();
		if (groupInfo == null) {
			return result;
		}
		try {
			result = SipProxy.getInstance().RequestCancelPttRight(groupInfo.getNumber());
			pttUtil.printLog(bDebug, LOG_TAG, "cancelPttRight " + groupInfo.getNumber()
					+ ", result : " + result);
		} catch (PTTException e) {
			Log.e(LOG_TAG, "error to cancelPttRight , result : " + result);
		}
		return result;
	}

	public int hangupPttCall() {
		int result = -1;
		int callId = pttGroupStatus.getCallId();
		pttUtil.printLog(bDebug, LOG_TAG, "hangupPttCall : " + callId);
		if (callId < 0) {
			return 0;
		}
		try {
			result = SipProxy.getInstance().RequestHangup(callId);
			pttUtil.printLog(bDebug, LOG_TAG, "hangupPttCall result: " + result);
		} catch (PTTException e) {
			Log.e(LOG_TAG, "error to hangup ptt , result : " + result);
		}
		return result;
	}
	
	public int getGroupNo(){
		return SipProxy.getInstance().RequestGroupNo(DEFAULT_DST);
	}

	public boolean isPttUIOnTop() {
		pttUtil.printLog(bDebug, LOG_TAG, "isPttUIOnTop : " + (PTTActivity.instance != null));
		return (PTTActivity.instance != null && "com.zzy.ptt.ui.PTTActivity".equals(PTTService.instance.getTopActivity()));
	}

	public int getCallState() {
		return callState;
	}

	public void setCallState(int callState) {
		this.callState = callState;
	}

	public void speakerCtrl(boolean on) {
		if (audioManager == null) {
			audioManager = (AudioManager) PTTService.instance
					.getSystemService(Context.AUDIO_SERVICE);
		}
		pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>>>speakerCtrl : " + on);
		audioManager.setSpeakerphoneOn(on);
	}

	public void adjustPttVolume(boolean speakerOn) {
		if (audioManager == null) {
			audioManager = (AudioManager) PTTService.instance
					.getSystemService(Context.AUDIO_SERVICE);
		}

		if (StateManager.isbEarphone()) {
			audioManager.setSpeakerphoneOn(false);
		} else {
			audioManager.setSpeakerphoneOn(speakerOn);
		}
        pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>>>adjustPttVolume, isbEarphone " + StateManager.isbEarphone()+",speakeron : " + audioManager.isSpeakerphoneOn());
		// set ptt volume
		int iCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
		int savedVolume = PTTService.instance.prefs.getInt(
				PTTService.instance.getString(R.string.sp_call_ptt_volume),
				audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL));
		if (iCurrentVolume != savedVolume) {
			audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, savedVolume, 0);
		}

		pttUtil.printLog(bDebug, LOG_TAG,
				"adjustPttVolume  speakerOn : " + audioManager.isSpeakerphoneOn() + ", volume : "
						+ audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));

	}

	public boolean isbPttErrorShowing() {
		if(bPttErrorShowing){
			if(!"com.zzy.ptt.ui.AlertActivity".equals(PTTService.instance.getTopActivity()))
			{
				this.bPttErrorShowing = false;
				return false;
			}
			return true;
		}
		return bPttErrorShowing;
	}

	public void setbPttErrorShowing(boolean bPttErrorShowing) {
		this.bPttErrorShowing = bPttErrorShowing;
	}
}
