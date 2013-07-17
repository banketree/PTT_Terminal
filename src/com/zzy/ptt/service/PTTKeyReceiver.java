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
import android.view.KeyEvent;

import com.zzy.ptt.service.PTTManager.PTT_KEY;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class PTTKeyReceiver extends BroadcastReceiver {

	private static final String LOG_TAG = "PTTKeyReceiver";

	private Context context;

	private static boolean bDebug = true;

	private PTTManager pttManager = PTTManager.getInstance();
	private CallStateManager callStateManager = CallStateManager.getInstance();
	private PTTUtil pttUtil = PTTUtil.getInstance();
	
//	private static boolean isKeyDown = false;

	@Override
	public void onReceive(Context context, Intent intent) {

		this.context = context;

		String action = null;
		if (intent != null)
			action = intent.getAction();

		if (action == null || action.length() == 0) {
			return;
		}
		
		pttUtil.printLog(bDebug, LOG_TAG, "---------------->>onReceive : " + action);

		handlePttDown(action);

		handlePttUp(action);

		handleNumberKey(context, action, intent);
	}

	private void handleNumberKey(Context context, String action, Intent intent) {
		if (!action.equals(PTTConstant.ACTION_NUMBER_KEY1)) {
			return;
		}
		int keyCode = intent.getIntExtra("number", -1);
		pttUtil.printLog(bDebug, LOG_TAG, "<><><><><><><><> keycode : " + keyCode);
		if (keyCode < KeyEvent.KEYCODE_0 && keyCode > KeyEvent.KEYCODE_9) {
			return;
		}
		Intent intent2 = new Intent(PTTConstant.ACTION_NUMBER_KEY2);
		intent2.putExtra(PTTConstant.KEY_NUMBER, keyCode);
		context.sendBroadcast(intent2);
	}

	private void handlePttUp(String action) {
		
//		pttUtil.printLog(bDebug, LOG_TAG, "handlePttUp---------------->>isKeyDown : " + isKeyDown);
		
		if (PTTService.instance == null) {
			return;
		}
		if (!action.equals(PTTConstant.ACTION_PTT_UP)) {
			return;
		}
		pttUtil.printLog(
				bDebug,
				LOG_TAG,
				"handlePttUp " + action + ", ptt key state : " + pttManager.ePttKeyStatus.name() + ", "
						+ pttManager.getCurrentPttState());

		pttManager.ePttKeyStatus = PTT_KEY.PTT_KEY_UP;
               //add by yjp  
                try{
                   Thread.sleep(300);
                }
                catch(Exception ex){
                   pttUtil.printLog(bDebug,
                                    LOG_TAG,
                                    "handelPTTUp sleep error "+ex.toString()); 
                }
		int status = pttManager.getCurrentPttState().getStatus();

		switch (status) {
		case PTTConstant.PTT_APPLYING:
		case PTTConstant.PTT_WAITING:
			// cancel ptt right apply
			sendPttCommand(context, PTTConstant.COMM_CANCEL_PTT_RIGHT);
			break;

		case PTTConstant.PTT_SPEAKING:
			// hangup ptt call
			sendPttCommand(context, PTTConstant.COMM_HANGUP_PTT_CALL);
			break;
		case PTTConstant.PTT_REJECTED:
		case PTTConstant.PTT_CANCELED:
			pttManager.getCurrentPttState().setRejectReason(-1);
			if(pttUtil.isStrBlank(pttManager.getCurrentPttState().getSpeakerNum()))
				pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_FREE);
			else 
				pttManager.getCurrentPttState().setStatus(PTTConstant.PTT_LISTENING);
			sendPttCommand(context, PTTConstant.COMM_UPDATE_PTT_UI);
			break;
		case PTTConstant.PTT_ACCEPTED:
			// update ptt ui
			sendPttCommand(context, PTTConstant.COMM_HANGUP_PTT_CALL);
			break;
		default:
			break;
		}
//		isKeyDown = false;
//		pttUtil.printLog(bDebug, LOG_TAG, "handlePttUp---------------->>isKeyDown : set false" );
	}

	private void handlePttDown(String action) {
		
//		pttUtil.printLog(bDebug, LOG_TAG, "handlePttDown---------------->>isKeyDown : " + isKeyDown);

		if (!action.equals(PTTConstant.ACTION_PTT_DOWN)) {
			return;
		}
		if (PTTService.instance == null) {
			return;
		}
		
		PTTService.instance.setPttUIClosed(true);

		// ignore ptt events
		pttUtil.printLog(bDebug, LOG_TAG,
				"handlePttDown,>>>>>>>>>>iCallState: " + pttUtil.getCallStateString(callStateManager.getCallState()));
		// check if any single call exist, if so , ignore and if not , do ptt
		if (callStateManager.getCallState() != PTTConstant.CALL_IDLE
				&& callStateManager.getCallState() != PTTConstant.CALL_ENDED) {
			return;
		}

		int iStatus = pttManager.getCurrentPttState().getStatus();
		if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_DOWN) {
			switch (iStatus) {
			case PTTConstant.PTT_INIT:
			case PTTConstant.PTT_FREE:
			case PTTConstant.PTT_REJECTED:
			case PTTConstant.PTT_CANCELED:
			case PTTConstant.PTT_LISTENING: {
				String user = pttUtil.getSipUserFromPrefs(PTTService.instance.prefs).getUsername();
				if (user.equals(pttManager.getCurrentPttState().getSpeakerNum())) {
					break;
				}
			}
			case PTTConstant.PTT_CANCELING:
				// case PTTConstant.PTT_ACCEPTED:
				// apply ptt right
				pttUtil.printLog(bDebug, LOG_TAG,
						"handlePttDown,------------->iStatus: " + pttUtil.getPttStatusString(iStatus));
				sendPttCommand(context, PTTConstant.COMM_APPLY_PTT_RIGHT);
				break;
			}
//			isKeyDown = true;
			return;
		}

		if (pttManager.ePttKeyStatus == PTT_KEY.PTT_KEY_UP) {

			pttUtil.printLog(bDebug, LOG_TAG, "handlePttDown,iStatus: " + pttUtil.getPttStatusString(iStatus));

			pttManager.ePttKeyStatus = PTT_KEY.PTT_KEY_DOWN;
//			isKeyDown = true;
			switch (iStatus) {
			case PTTConstant.PTT_LISTENING:
			case PTTConstant.PTT_FREE:
				// case PTTConstant.PTT_APPLYING:
			case PTTConstant.PTT_REJECTED:
			case PTTConstant.PTT_CANCELED:
			case PTTConstant.PTT_IDLE:
			case PTTConstant.PTT_INIT:
				// apply ptt right
				sendPttCommand(context, PTTConstant.COMM_APPLY_PTT_RIGHT);
				break;
			default:
				break;
			}
		}
	}

	private void sendPttCommand(Context context, int pttCommand) {
		Intent intent = new Intent(PTTConstant.ACTION_COMMAND);
		// intent.setClass(context, PTTService.class);
		intent.putExtra(PTTConstant.KEY_SERVICE_COMMAND, pttCommand);
		PTTService.instance.sendPttCommand(intent);
	}
}
