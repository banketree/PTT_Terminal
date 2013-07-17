/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : ChatActivity
 * Author : wangjunhui
 * Version : 1.0
 * Date : 2012-04-26
 */
package com.zzy.ptt.ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteFullException;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.db.MessageCl;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.Message;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.service.MessageReceiver;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author wangjunhui
 * 
 */
public class ChatActivity extends BaseActivity {
	private Message msg;
	private MessageCl cl;
	private LayoutInflater inflater;
	private MyAdapter adapter;
	private Button sendButton;
	private EditText msgBody;
	private String caddress;
	private int listId = -1, sendstatus;
	private MyChatFreshReceicer receicer;
	private MyChatStatusReceicer receicer2;

	public static final int TYPEME = 1;// me
	public static final int TYPEHE = 2;// others
	public static final int READ = 1;
	public static final int UNREAD = 2;
	public static final int SENDOK = 1;
	public static final int INSEND = 2;
	public static final int UNSEND = 3;
	private ListView talkView;
	private List<Message> list = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chatmain1);
		inflater = LayoutInflater.from(this);
		adapter = new MyAdapter();
		talkView = (ListView) findViewById(R.id.list);
		sendButton = (Button) findViewById(R.id.MessageButton);
		msgBody = (EditText) findViewById(R.id.MessageText);
		cl = new MessageCl(getApplicationContext());
		Intent intent = getIntent();
		caddress = intent.getStringExtra("address");
		list = cl.getMsgAtrrayfromAddress(caddress);
		setTitle(getApplicationContext().getString(R.string.title1) + caddress
				+ getApplicationContext().getString(R.string.title2));

		/*
		 * for (int i = 0; i < list.size(); i++) { msg = list.get(i);
		 * msg.setReadstatus(READ); cl.updateMsgReadStatus(msg); }
		 */
		
		msgBody.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				if (s.toString().length() >= 120) {
					Toast.makeText(getApplicationContext(),
							getApplicationContext().getString(R.string.msg_text_length), Toast.LENGTH_LONG).show();
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

		receicer = new MyChatFreshReceicer();
		receicer2 = new MyChatStatusReceicer();
		IntentFilter filter = new IntentFilter();
		IntentFilter filter2 = new IntentFilter();
		filter.addAction(MessageReceiver.FRESHVIEW);
		filter2.addAction(MessageReceiver.SENDSTATUS);
		this.registerReceiver(receicer, filter);
		this.registerReceiver(receicer2, filter2);

		talkView.setAdapter(adapter);
		talkView.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				listId = arg2;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});
		talkView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				registerForContextMenu(arg0);
				return false;
			}
		});
		sendButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String address = caddress;
				String date = getDate();
				String body = msgBody.getText().toString();
				int type = 1;
				int conn = -1;
				int readstatus = READ;
				int sendstatus = INSEND;
				int thread_id = cl.getThreadidFromAddress(address);
				msg = new Message(thread_id, address, date, body, type, readstatus, sendstatus);
				try {
					cl.addNew(msg);
				} catch (SQLiteFullException e) {
					// TODO: handle exception
					PTTUtil.getInstance().errorAlert(getApplicationContext(), -1,
							getApplicationContext().getString(R.string.alert_msg_no_enough_space_my), false);
				}
				try {
					conn = SipProxy.getInstance().RequestSendMessage(address, body);
				} catch (PTTException e) {
					e.printStackTrace();
				}
				if (conn == 0) {
					msgBody.setText("");
				} else {
					Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.no_connect),
							Toast.LENGTH_LONG).show();
				}

			}
		});
	}

	private String getDate() {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
		Date d = new Date();
		return sdf.format(d);
	}

	public void freshListView() {
		cl = new MessageCl(getApplicationContext());
		list = cl.getMsgAtrrayfromAddress(caddress);
		// for (int i = 0; i < list.size(); i++) {
		// msg = list.get(i);
		// msg.setReadstatus(READ);
		// cl.updateMsgReadStatus(msg);
		// }
		adapter.notifyDataSetChanged();
		talkView.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, getApplicationContext().getString(R.string.chat_menu0));
		menu.add(0, 1, 0, getApplicationContext().getString(R.string.chat_menu1));
		menu.add(0, 2, 0, getApplicationContext().getString(R.string.chat_menu2));
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Message msg = list.get(listId);
		int conn = -1;
		switch (item.getItemId()) {
		case 0:
			if (msg.getType() == 1) {
				try {
					cl.addNew(msg);
				} catch (SQLiteFullException e1) {
					// TODO Auto-generated catch block
					PTTUtil.getInstance().errorAlert(getApplicationContext(), -1,
							getApplicationContext().getString(R.string.alert_msg_no_enough_space_my), false);
					e1.printStackTrace();
				}
				try {
					conn = SipProxy.getInstance().RequestSendMessage(msg.getNumber(), msg.getBody());
				} catch (PTTException e) {
					e.printStackTrace();
				}
				if (conn == 0) {
					msgBody.setText("");
				} else {
					Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.no_connect),
							Toast.LENGTH_LONG).show();
				}
			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.chat_resend),
						Toast.LENGTH_LONG).show();
			}
			break;
		case 1:
			int id = cl.getIdFromMsg(msg);
			cl.deleteMsg(id);
			freshListView();
			break;
		case 2:
			final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getApplicationContext().getString(R.string.chat_careful));
			builder.setMessage(getApplicationContext().getString(R.string.chat_oper));
			builder.setIcon(android.R.drawable.ic_dialog_alert);
			builder.setPositiveButton(getApplicationContext().getString(R.string.chat_oper_ok),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							String address = msg.getNumber();
							int threadid = cl.getThreadidFromAddress(address);
							cl.deleteSession(threadid);
							ChatActivity.this.finish();
						}
					});
			builder.setNegativeButton(getApplicationContext().getString(R.string.chat_oper_cancle),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
			builder.show();

			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	class MyChatFreshReceicer extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int fresh = intent.getIntExtra("fresh", 0);
			Message msg = (Message) intent.getSerializableExtra("newMsg");
			if (fresh == 200) {
				MessageReceiver.nm.cancel(UNREAD);
				msg.setReadstatus(READ);
				cl.updateMsgReadStatus(msg);
				freshListView();
			}
		}
	}

	class MyChatStatusReceicer extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			sendstatus = intent.getIntExtra("sendstatus", 888);
			if (sendstatus == 200) {
				msg.setSendstatus(SENDOK);
				cl.updateMsgSendStatus(msg);
				freshListView();
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.chat_send_success),
						Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(
						getApplicationContext(),
						getApplicationContext().getString(R.string.chat_send_fault1) + caddress
								+ getApplicationContext().getString(R.string.chat_send_fault2), Toast.LENGTH_LONG)
						.show();
				msg.setSendstatus(UNSEND);
				cl.updateMsgSendStatus(msg);
				freshListView();
			}
		}

	}

	class MyAdapter extends BaseAdapter {
		TextView addressTV, msgtimeTV, msgbodyTV;

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View v, ViewGroup parent) {
			msg = list.get(position);
			int type = msg.getType();
			int sendstatus = msg.getSendstatus();
			if (type == TYPEME) {
				v = inflater.inflate(R.layout.list_say_me_itemnew, null);
			} else {
				v = inflater.inflate(R.layout.list_say_me_item, null);
			}
			addressTV = (TextView) v.findViewById(R.id.messagedetail_row_name);
			msgtimeTV = (TextView) v.findViewById(R.id.messagedetail_row_date);
			msgbodyTV = (TextView) v.findViewById(R.id.messagedetail_row_text);
			ImageView sendstatusimg = (ImageView) v.findViewById(R.id.send_status);
			String address;
			if (type == TYPEME) {
				address = getApplicationContext().getString(R.string.chat_name);
			} else {
				address = msg.getNumber();
			}
			if (sendstatus == UNSEND) {
				sendstatusimg.setBackgroundResource(R.drawable.unsend);
			}
			String date = msg.getDate();
			String body = msg.getBody();
			addressTV.setText(address);
			msgtimeTV.setText(date);
			msgbodyTV.setText(body);
			return v;
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receicer);
		unregisterReceiver(receicer2);
	}
}
