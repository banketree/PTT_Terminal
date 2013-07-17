/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:JniCallback.java
 * Description:JniCallback, declares the methods invoked by C
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-2
 */

package com.zzy.ptt.jni;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.CallState;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.model.MemberInfo;
import com.zzy.ptt.model.PttGroupStatus;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.service.CallStateManager;
import com.zzy.ptt.service.PTTManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.service.PTTManager.PTT_KEY;
import com.zzy.ptt.ui.PTTActivity;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class JniCallback {

	private static PTTService service;

	private static final String LOG_TAG = "JniCallback";

	private static JniCallback instance = new JniCallback();

	private PTTManager pttManager = PTTManager.getInstance();
	private PTTUtil pttUtil = PTTUtil.getInstance();
	private CallStateManager callManager = CallStateManager.getInstance();

	private static boolean bDebug = true;

	public JniCallback() {
	}

	public static JniCallback getInstance() {
		return instance;
	}

	public void showLoginState(int state, int errorCode) {

		if (PTTService.instance == null) {
			return;// ignore
		}

		EnumLoginState currentState = StateManager.getCurrentRegState();

		pttUtil.printLog(bDebug, LOG_TAG, "-------showLoginState---state : " + pttUtil.getRegStateString(state)
				+ ", currentState : " + pttUtil.getRegStateString(currentState) + ",code :" + errorCode);
		if (currentState == EnumLoginState.UNREGISTERED && errorCode != PTTConstant.RESPONSE_OK) {
			// ignore
			return;
		}

		try {
			EnumLoginState newState = null;
			if (state == PTTConstant.REG_NOT)
				newState = EnumLoginState.UNREGISTERED;
			else if (state == PTTConstant.REG_ED)
				newState = EnumLoginState.REGISTERE_SUCCESS;
			else if (state == PTTConstant.REG_ING)
				newState = EnumLoginState.REGISTERING;
			else if (state == PTTConstant.REG_ERROR) {
				newState = EnumLoginState.ERROR_CODE_NUMBER;
			}

			//if (StateManager.getCurrentRegState() != newState || (newState == null)) {
				StateManager.setCurrentRegState(newState);
				service.JNIUpdateRegisterState(newState, state, errorCode);
			//}
		} catch (Exception e) {
			Log.e(LOG_TAG, "showLoginState error, seems the pjsip has crashed");
		}
	}

	public void showCallState(CallState callState) {
		// check ptt if exist
		int iPttStatus = pttManager.getCurrentPttState().getStatus();
		int iCallState = callState.getCallState();
		pttUtil.printLog(bDebug, LOG_TAG,
				"-----------showCallState, ptt status : " + pttUtil.getPttStatusString(iPttStatus));
		pttUtil.printLog(bDebug, LOG_TAG, "-----------showCallState, call state : " + callState);
		pttUtil.printLog(bDebug, LOG_TAG, "-----------showCallState, callManager : " + CallStateManager.getInstance());

		// check error code
		int errorCode = callState.getErrorCode();
		if (callManager.checkErrorCode(errorCode)) {
			// if (pttManager.isPttUIOnTop()) {
			if (callManager.isSingleCallOnTop() || callManager.isbForceInSC()
					|| !pttUtil.checkIsGroupNum(callState.getNumber())) {
				if (callManager.getCallState() == PTTConstant.CALL_RINGING
						|| callManager.getCallState() == PTTConstant.CALL_REJECTING) {
					if (errorCode == PTTConstant.ERROR_CODE_486 && iCallState == PTTConstant.CALL_ENDED) {
						service.JNIUpdateCallState(callState);
					}
				} else {
					pttUtil.printLog(bDebug, LOG_TAG,
							"<<<<<<<<<<<<<<<<<>>>>>>>>>showCallState  We got a timeout message, clear call state!!!");
					pttUtil.errorAlert(PTTService.instance, errorCode, "", false);
					service.JNIUpdateCallState(callState);
				}
			} else if ((pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) || pttManager.isPttUIOnTop()) {
				switch (errorCode) {
				case PTTConstant.ERROR_CODE_486:
				case PTTConstant.ERROR_CODE_504:

					pttUtil.printLog(bDebug, LOG_TAG,
							"<<<<<<<<<<<<<<<<<>>>>>>>>>showCallState  We got a timeout message, clear ptt state!!!");

					pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
					pttManager.getCurrentPttState().setSpeakerNum(null);
					if (errorCode == PTTConstant.ERROR_CODE_504) {
						pttManager.setbPttErrorShowing(true);
						pttUtil.errorAlert(PTTService.instance, errorCode, "", false);
					}
					break;
				default:
					break;
				}
				pttManager.setCallState(callState.getCallState());
			}

			if (errorCode == PTTConstant.ERROR_CODE_504) {
				pttUtil.printLog(bDebug, LOG_TAG,
						"<<<<<<<<<<<<<<<<<>>>>>>>>>showCallState  Receive 504, hangup call mannually");
				try {
					SipProxy.getInstance().RequestHangup(callState.getCallId());
				} catch (PTTException e) {
					e.printStackTrace();
				}
			}
			return;
		}

		if (pttUtil.checkIsGroupNum(callState.getNumber())) {

			if (callManager.isbForceInSC()) {
				// single call dial group number
				pttUtil.printLog(bDebug, LOG_TAG, "-----------go to single call");
				PTTService.instance.JNIUpdateCallState(callState);
				return;
			}

			int iCallId = callState.getCallId();
			pttUtil.printLog(bDebug, LOG_TAG, "PTT_KEY_DOWN-----------go to ptt,icallid : " + iCallId);
			if (iCallId >= 0) {
				pttManager.getCurrentPttState().setCallId(iCallId);
			}
			pttManager.setCallState(callState.getCallState());

			switch (iCallState) {
			case PTTConstant.CALL_RINGING:
				// pttActivity is on top
				service.broadcastDebugInfo("invite is coming");
				pttUtil.printLog(bDebug, LOG_TAG, "-----------pttActivity is on top");
				// in ptt mo
				switch (iPttStatus) {
				case PTTConstant.PTT_IDLE:// in case the CALL_RINGING received
											// after 403
					// in ptt apply right
				case PTTConstant.PTT_FREE:
				case PTTConstant.PTT_APPLYING:
				case PTTConstant.PTT_ACCEPTED:
				case PTTConstant.PTT_WAITING:
				case PTTConstant.PTT_CANCELING:
				case PTTConstant.PTT_LISTENING:
				case PTTConstant.PTT_REJECTED:
					pttManager.getCurrentPttState().setCallId(iCallId);
					// auto answer the s-Invite
					try {
//						if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {
							pttUtil.printLog(bDebug, LOG_TAG,
									"------showCallState, autoAnswer " + pttManager.getCurrentPttState());
							//service.playPttAlert(PTTService.instance, PTTConstant.PTT_ALERT_ACCEPT);
							service.broadcastDebugInfo("invite is answering");
							SipProxy.getInstance().RequestAnswer(iCallId);
//						} else {
//							pttUtil.printLog(bDebug, LOG_TAG, "------showCallState, autoHangup ");
//							service.broadcastDebugInfo("invite is hanging");
//							SipProxy.getInstance().RequestHangup(iCallId);
//						}
						return;
					} catch (PTTException e) {
						e.printStackTrace();
					}
					break;
				/*
				 * updatePttState(pttManager.getCurrentPttState()); break;
				 */
				default:
                                        //by yjp 2013-4-1
                                        //solve group number call in
                                        try { 
                                            if (pttManager.ePttKeyStatus != PTT_KEY.PTT_KEY_DOWN) {
                                                pttUtil.printLog(bDebug, LOG_TAG, "------showCallState, autoHangup2 (default dispose) ");
					        service.broadcastDebugInfo("invite is hanging");
					        SipProxy.getInstance().RequestHangup(iCallId); 
					    }
                                        } catch (PTTException e) {
						e.printStackTrace();
					}
					break;
				}
				break;
			case PTTConstant.CALL_TALKING:
				// hangup
				switch (iPttStatus) {
				case PTTConstant.PTT_FREE:
					if (pttManager.getCurrentPttState().getCallId() != callState.getCallId()) {
						break;
					}
					// go through
				case PTTConstant.PTT_ACCEPTED:
				case PTTConstant.PTT_CANCELING:
				case PTTConstant.PTT_CANCELED:
				case PTTConstant.PTT_WAITING:
				case PTTConstant.PTT_APPLYING:
				case PTTConstant.PTT_LISTENING:
				case PTTConstant.PTT_IDLE:// in case the CALL_RINGING received
											// after 403
					
					pttUtil.printLog(true, LOG_TAG, "sxsexe ePttKeyStatus " + pttManager.ePttKeyStatus + "current state " + pttManager.getCurrentPttState());
					
					if((pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_UP && pttManager.getCurrentPttState().getStatus() == PTTConstant.PTT_LISTENING)
							||(pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN)) {
						service.broadcastDebugInfo("invite answer OK");
						// update ptt ui
						if (null == pttManager.getCurrentPttState().getGroupNum()) {
							GroupInfo groupInfo = pttUtil.getCurrentGroupInfo();
							if (groupInfo != null) {
								pttManager.getCurrentPttState().setGroupNum(groupInfo.getNumber());
							}
						}

						String speakerNum = pttManager.getCurrentPttState().getSpeakerNum();
						String usernum = pttUtil.getSipUserFromPrefs(PTTService.instance.prefs).getUsername();
						if(usernum.equals(speakerNum) || TextUtils.isEmpty(speakerNum)) {
							pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_SPEAKING);
						}
						if(pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {
							pttManager.getCurrentPttState().setSpeakerNum(usernum);
						}
						pttManager.getCurrentPttState().setCallId(callState.getCallId());
						service.JNIUpdatePTTState(pttManager.getCurrentPttState());
					} else {
						service.broadcastDebugInfo("invite is hanging");
						pttManager.hangupPttCall();
					}
					break;
				default:
					break;
				}
				break;
			case PTTConstant.CALL_ENDED:
				service.broadcastDebugInfo("invite hangup OK");
				switch (iPttStatus) {
				case PTTConstant.PTT_FREE:
				case PTTConstant.PTT_ACCEPTED:
				case PTTConstant.PTT_WAITING:
					if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_UP) {
						String speaker = pttManager.getCurrentPttState().getSpeakerNum();
						int state = PTTConstant.PTT_FREE;
						if (speaker != null && speaker.trim().length() > 0) {
							state = PTTConstant.PTT_LISTENING;
						}
						if (PTTConstant.PTT_WAITING == iPttStatus) {
							state = PTTConstant.PTT_CANCELING;
							pttManager.cancelPttRight();
						}

						pttManager.getCurrentPttState().setStatus(state);
						service.JNIUpdatePTTState(pttManager.getCurrentPttState());
						return;
					}
					break;
				case PTTConstant.PTT_SPEAKING:
                                       //by yjp 2013-4-1
                                       //solve for when mds handup ,ptt prompt 'di~~~~' sound 
                                       if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {                                      
                                           pttUtil.printLog(bDebug, 
                                                            LOG_TAG,
                                                            "^^^^^^when ptt speaking,and cancle by mds, play 'di~~~~' sound ");
                                           service.playPttAlert(PTTService.instance,PTTConstant.PTT_ALERT_RELEASE);
                                        }
                                        //end yjp
					pttManager.getCurrentPttState().setSpeakerNum(null);
					pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
					// pttManager.bPttAlertPlayed = false;
					service.JNIUpdatePTTState(pttManager.getCurrentPttState());
					return;
				case PTTConstant.PTT_LISTENING:
					pttManager.getCurrentPttState().setSpeakerNum(null);
					pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
					service.JNIUpdatePTTState(pttManager.getCurrentPttState());
					break;
				default:
					break;
				}
				break;

			default:
				break;
			}
		}

		if (!pttManager.isPttUIOnTop()) {
			if (CallStateManager.getInstance().isSingleCallOnTop()) {
				pttUtil.printLog(bDebug, LOG_TAG, "*********----------InCallScreenActivity is on top");
				service.JNIUpdateCallState(callState);
				// pttManager.clear();
			} else {
				switch (iPttStatus) {
				case PTTConstant.PTT_FREE:
				//case PTTConstant.PTT_LISTENING:
					// case PTTConstant.PTT_ACCEPTED:
				case PTTConstant.PTT_APPLYING:
					// case PTTConstant.PTT_INIT:
				case PTTConstant.PTT_IDLE:
					if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_UP) {
						pttUtil.printLog(bDebug, LOG_TAG, "----------go to -InCallScreenActivity ");
						service.JNIUpdateCallState(callState);
						// pttManager.clear();
					} else {
						pttManager.getCurrentPttState().setCallId(callState.getCallId());
					}
					break;
				default:
					break;
				}
			}
		} else {
			switch (iPttStatus) {
			case PTTConstant.PTT_FREE:
				// case PTTConstant.PTT_INIT:
			case PTTConstant.PTT_IDLE:
				pttUtil.printLog(bDebug, LOG_TAG, "callId :" + callState.getCallId() + ",pttCallId:"
						+ pttManager.getCurrentPttState().getCallId());
				if (pttUtil.checkIsGroupNum(callState.getNumber())) {
					// do nothing
				} else {
					pttUtil.printLog(bDebug, LOG_TAG, "InCallScreenActivity----------go to -InCallScreenActivity ");
					service.JNIUpdateCallState(callState);
				}
				break;
			default:
				break;
			}
		}
	}

	public void receiveGroupInfo(GroupInfo[] groupInfos) {
		pttUtil.printLog(bDebug, LOG_TAG, "receiveGroupInfo  groupInfos : " + groupInfos);
		service.JNIAddGroupInfo(groupInfos);
	}

	public static PTTService getService() {
		return service;
	}

	public static void setService(PTTService Service) {
		service = Service;
	}

	// add by wangjunhui
	public void receiveMessage(String number, String text) {
		pttUtil.printLog(bDebug, LOG_TAG, "receiveMessage  number : " + number);
		service.JNIReceiveMessage(number, text);
	}

	// add by wangjunhui
	public void sendMessageResult(int status) {
		service.JNISendMessageResult(status);
	}

	public void receiveMemberInfo(String groupNum, MemberInfo[] members) {

		for (int i = 0; i < members.length; i++) {
			pttUtil.printLog(bDebug, LOG_TAG,
					"receiveMemberInfo " + members[i].getName() + " " + members[i].getNumber());
		}
		service.JNIReceiveMemberInfo(groupNum, members);
	}

	public void receiveMemberStateChange(MemberInfo member) {
		service.JNIReceiveMemberStateChange(member);
	}

	public void updatePttState(PttGroupStatus pttGroupStatus) {
		service.JNIUpdatePTTState(pttGroupStatus);
	}

	public void mInviteIndicate(String groupNum, String speaker) {
		pttUtil.printLog(bDebug, LOG_TAG, "mInviteIndicate");
		service.mInviteIndicate(groupNum, speaker);
	}

	public void mByeIndicate() {
		service.mByeIndicate();
	}

	public void receivePttRightResponse(int responseStatus) throws PTTException {
		pttUtil.printLog(bDebug, LOG_TAG,
				"receivePttRightResponse : " + responseStatus + "--" + pttUtil.getPttStatusString(responseStatus));
		PttGroupStatus pttGroupStatus = pttManager.getCurrentPttState();

		String speaker = pttGroupStatus.getSpeakerNum();
		// boolean bIfSpeaker = pttUtil.checkIfSpeaker(speaker);
		if (pttGroupStatus.getStatus() == PTTConstant.PTT_SPEAKING) {
			return;
		}

		service.broadcastDebugInfo("ptt state " + pttUtil.getPttStatusString(responseStatus));
		if (responseStatus == PTTConstant.PTT_BUSY) {// to cordinate with
														// protocol
			responseStatus = PTTConstant.PTT_ACCEPTED;
		} else if (responseStatus == PTTConstant.PTT_CANCELED || responseStatus == PTTConstant.PTT_REJECTED) {

			if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_UP) {
				if (pttUtil.isStrBlank(speaker)) {
					responseStatus = PTTConstant.PTT_FREE;
				} else {
					responseStatus = PTTConstant.PTT_LISTENING;
				}
			}
		} else if (responseStatus == PTTConstant.PTT_FORBIDDEN) {
			// protocol receive 403
			pttUtil.printLog(bDebug, LOG_TAG, ">>>>>>>>>>>>>>>>>>protocol receive 403");
			if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {
				pttManager.clear();
				pttManager.ePttKeyStatus = PTT_KEY.PTT_KEY_DOWN;
				sendPttCommand(PTTService.instance, PTTConstant.COMM_APPLY_PTT_RIGHT);
				return;
			} else {
				if (PTTActivity.instance != null) {
					PTTActivity.instance.finish();
				}
				pttManager.clear();
				// pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
				return;
			}
		}

		pttGroupStatus.setStatus(responseStatus);
		pttUtil.printLog(bDebug, LOG_TAG, "=========== : " + pttGroupStatus + ",keyStatus:" + pttManager.ePttKeyStatus);
		service.JNIUpdatePTTState(pttGroupStatus);
	}

	private void sendPttCommand(Context context, int pttCommand) {
		Intent intent = new Intent(PTTConstant.ACTION_COMMAND);
		// intent.setClass(context, PTTService.class);
		intent.putExtra(PTTConstant.KEY_SERVICE_COMMAND, pttCommand);
		// context.startService(intent);
		PTTService.instance.sendPttCommand(intent);
	}
}
