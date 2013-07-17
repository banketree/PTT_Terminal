/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : NewMsgActivity
 * Author : wangjunhui
 * Version : 1.0
 * Date : 2012-04-26
 */
package com.zzy.ptt.ui;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteFullException;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.db.MessageCl;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.Message;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.service.MessageReceiver;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author wangjunhui
 * 
 */
public class NewMsgActivity extends BaseActivity {
	private Button send, exit, select;
	private EditText num, text;
	private Message message;
	private MessageCl cl;
	private MySendStatusReveiver reveiver;
	public static final int TYPEME = 1;// me
	public static final int TYPEHE = 2;// others
	public static final int READ = 1;
	public static final int UNREAD = 2;
	public static final int SENDOK = 1;
	public static final int INSEND = 2;
	public static final int UNSEND = 3;
	private int thread_id, type, readstatus, sendstatus;
	private String address, date, body;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.newmsg);
		NewMsgActivity.this.setTitle(getApplicationContext().getString(R.string.titlename));
		send = (Button) findViewById(R.id.sendnewmsg);
		exit = (Button) findViewById(R.id.exitnewmsg);
		select = (Button) findViewById(R.id.selectcontact);
		num = (EditText) findViewById(R.id.newmsg_name);
		text = (EditText) findViewById(R.id.newmsg_text);

		cl = new MessageCl(getApplicationContext());

		Intent intent = getIntent();
		address = intent.getStringExtra("num");
		num.setText(address);

		reveiver = new MySendStatusReveiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(MessageReceiver.SENDSTATUS);
		this.registerReceiver(reveiver, filter);

		text.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				if (s.toString().length() >= 120) {
					Toast.makeText(getApplicationContext(),
							getApplicationContext().getString(R.string.msg_text_length),
							Toast.LENGTH_LONG).show();
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub

			}
		});
		num.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				if (s.toString().length() >= 20) {
					Toast.makeText(getApplicationContext(),
							getApplicationContext().getString(R.string.dail_num_length),
							Toast.LENGTH_LONG).show();
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub

			}
		});

		send.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (num.getText().length() != 0) {
					thread_id = cl.getThreadidFromAddress(num.getText().toString());
					address = num.getText().toString();
					date = getDate();
					body = text.getText().toString().trim();
					type = TYPEME;
					readstatus = READ;
					sendstatus = INSEND;
					message = new Message(thread_id, address, date, body, type, readstatus,
							sendstatus);
					int conn = -1;
					try {
						conn = SipProxy.getInstance().RequestSendMessage(address, body);
					} catch (PTTException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (conn == 0) {
						try {
							cl.addNew(message);
						} catch (SQLiteFullException e) {
							// TODO Auto-generated catch block
							PTTUtil.getInstance().errorAlert(
									getApplicationContext(),
									-1,
									getApplicationContext().getString(
											R.string.alert_msg_no_enough_space_my), false);
							e.printStackTrace();
						}
					} else {
						Toast.makeText(getApplicationContext(),
								getApplicationContext().getString(R.string.no_connect),
								Toast.LENGTH_LONG).show();
					}

				} else {
					body = text.getText().toString().trim();
					if ("debug on".equals(body)) {
						Intent intent = new Intent(PTTConstant.ACTION_DEBUG);
						intent.putExtra("debug_on", 1);
						sendBroadcast(intent);
					} else if ("debug off".equals(body)) {
						Intent intent = new Intent(PTTConstant.ACTION_DEBUG);
						intent.putExtra("debug_on", 0);
						sendBroadcast(intent);
					} else {
						Toast.makeText(getApplicationContext(),
								getApplicationContext().getString(R.string.numisnull),
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		});

		exit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				NewMsgActivity.this.finish();
			}
		});

		select.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(NewMsgActivity.this, PBKActivity.class);
				intent.putExtra("getnum", 852);
				startActivityForResult(intent, 200);
			}
		});
	}

	class MySendStatusReveiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			int sendstatus = intent.getIntExtra("sendstatus", 777);
			if (sendstatus == 200) {
				message.setSendstatus(SENDOK);
				Toast.makeText(getApplicationContext(),
						getApplicationContext().getString(R.string.chat_send_success),
						Toast.LENGTH_LONG).show();
				cl.updateMsgSendStatus(message);
				NewMsgActivity.this.finish();
			} else {
				Toast.makeText(
						getApplicationContext(),
						getApplicationContext().getString(R.string.chat_send_fault1) + address
								+ getApplicationContext().getString(R.string.chat_send_fault2),
						Toast.LENGTH_LONG).show();
				message.setSendstatus(UNSEND);
				cl.updateMsgSendStatus(message);
				NewMsgActivity.this.finish();
			}

		}

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		this.unregisterReceiver(reveiver);
	}

	private String getDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
		Date d = new Date();
		return sdf.format(d);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == 200) {
			String phonenum = data.getStringExtra("phonenum");
			num.setText(phonenum);
		}
	}
}
