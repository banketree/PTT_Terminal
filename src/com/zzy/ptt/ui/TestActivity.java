package com.zzy.ptt.ui;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.db.DBHelper;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.jni.JniCallback;
import com.zzy.ptt.model.CallLog;
import com.zzy.ptt.model.CallState;
import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.service.GroupManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.util.PTTConstant;

public class TestActivity extends Activity {

	private Button btnMakecall, btnAnswer, btnHangup, btnRegister, btnIncoming;
	private Button btnQuery, btnTestPtt;
	private Button btnSendMessage, btnReceiveGrpMember;
	private Button btnApplyRight;

	private static final String LOG_TAG = "TestActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		PTTService.activityList.add(this);

		InitWidget();

	}

	private void InitWidget() {
		btnMakecall = (Button) findViewById(R.id.btn_makecall);
		btnAnswer = (Button) findViewById(R.id.btn_answer);
		btnHangup = (Button) findViewById(R.id.btn_hangup);
		btnRegister = (Button) findViewById(R.id.btn_register);
		btnIncoming = (Button) findViewById(R.id.btn_incoming);
		btnQuery = (Button) findViewById(R.id.btn_query);
		btnTestPtt = (Button) findViewById(R.id.btn_query);
		btnReceiveGrpMember = (Button) findViewById(R.id.btn_receive_members);
		btnSendMessage = (Button) findViewById(R.id.btn_sendmsg);
		btnApplyRight = (Button) findViewById(R.id.btn_apply_pttright);

		btnMakecall.setOnClickListener(mClickListener);
		btnAnswer.setOnClickListener(mClickListener);
		btnRegister.setOnClickListener(mClickListener);
		btnHangup.setOnClickListener(mClickListener);
		btnIncoming.setOnClickListener(mClickListener);
		btnQuery.setOnClickListener(mClickListener);
		btnTestPtt.setOnClickListener(mClickListener);
		btnReceiveGrpMember.setOnClickListener(mClickListener);
		btnSendMessage.setOnClickListener(mClickListener);
		btnApplyRight.setOnClickListener(mClickListener);
	}

	private View.OnClickListener mClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Check invalid info

			if (v == btnRegister) {
				// SipProxy.getInstance().RequestRegister(objMessage);
				// Log.d("TestActivity",
				// "TestCount : "+GroupManager.getInstance().getTestCount());
				// List<String> lstData =
				// GroupManager.getInstance().getLstTestData();
				// int size = lstData.size();
				// for(int i=0; i< size; i++){
				// Log.d("TestActivity", "list : "+ i + lstData.get(i));
				// }
			} else if (v == btnMakecall) {
				// SipProxy.getInstance().RequestMakeCall(objMessage);
			} else if (v == btnAnswer) {
				// SipProxy.getInstance().RequestAnswer(objMessage);
			} else if (v == btnHangup) {
				// SipProxy.getInstance().RequestHangup(objMessage);
			} else if (v == btnIncoming) {
				CallState callState = new CallState();
				callState.setCallId(2);
				callState.setCallState(PTTConstant.CALL_RINGING);
				callState.setNumber("1212");
				callState.setSeconds(100000);
				callState.setErrorCode(2);
				JniCallback.getInstance().showCallState(callState);
			} else if (v == btnQuery) {
				List<CallLog> data = new DBHelper(TestActivity.this)
						.queryCallLog(PTTConstant.LOG_ALL);
				if (data == null || data.size() == 0) {
					return;
				}

				int size = data.size();
				CallLog callLog = null;
				for (int i = 0; i < size; i++) {
					callLog = data.get(i);
					Log.d("TestActivity", callLog.toString());
				}

			} else if (v == btnTestPtt) {
				// startActivity(new Intent(TestActivity.this,
				// PTTActivity.class));
			} else if (v == btnReceiveGrpMember) {
				try {
					List<GroupInfo> groupData = GroupManager.getInstance().getGroupData();
					if (groupData == null || groupData.size() == 0) {
						return;
					}

					int size = groupData.size();
					String grpNum = null;
					for (int i = 0; i < size; i++) {
						grpNum = groupData.get(i).getNumber();
						Log.e(LOG_TAG, "grpNum : " + grpNum);
						if (grpNum == null || grpNum.trim().length() == 0) {
							Toast.makeText(TestActivity.this, "grpNum info null", Toast.LENGTH_LONG)
									.show();
							continue;
						}
						int result = SipProxy.getInstance().RequestGetMember(grpNum);
						Log.d(LOG_TAG, "RequestGetMember return : " + result);
					}
				} catch (PTTException e) {
					e.printStackTrace();
				}

			} else if (v == btnSendMessage) {
				try {
					int result = SipProxy.getInstance().RequestSendMessage("1008", "Hello PjSip");
					Log.d(LOG_TAG, "RequestSendMessage return : " + result);
				} catch (PTTException e) {
					e.printStackTrace();
				}
			} else if (v == btnApplyRight) {
				List<GroupInfo> groupData = GroupManager.getInstance().getGroupData();
				if (groupData == null || groupData.size() == 0) {
					return;
				}

				String grpNum = groupData.get(0).getNumber();
				try {
					int result = SipProxy.getInstance().RequestApplyPttRight(grpNum);
					Log.d(LOG_TAG, "RequestApplyPttRight return : " + result);
				} catch (PTTException e) {
					e.printStackTrace();
				}
			}
		}
	};

	/*
	 * private SipMessage constructSipMessage() { SipMessage objMessage = new
	 * SipMessage(); objMessage.setUsername(etUsername.getText().toString());
	 * objMessage.setPassword(etPassword.getText().toString());
	 * objMessage.setServerIp(etServerIP.getText().toString()); return
	 * objMessage; }
	 */
}
