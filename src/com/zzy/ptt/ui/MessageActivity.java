/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : MessageActivity
 * Author : wangjunhui
 * Version : 1.0
 * Date : 2012-04-26
 */
package com.zzy.ptt.ui;

import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.db.MessageCl;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.Message;
import com.zzy.ptt.model.NumberInfo;
import com.zzy.ptt.service.MessageReceiver;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author wangjunhui
 * 
 */
public class MessageActivity extends BaseActivity {
	/** Called when the activity is first created. */
	private ListView listView;
	private LayoutInflater inflater;
	private SessionAdapter adapter;
	private TextView addressTV, msgnumsTV, msgbodyTV, newchattime;
	private MessageCl cl;
	private List<Message> lstSession;
	private boolean havaUnRead;
	private MyMsgReceicer receicer;
	public static final int READ = 1;
	public static final int UNREAD = 2;

	private static final int MENUM_NEWSESSION = 0;
	private static final int MENUM_CALL = 1;
	private static final int MENUM_ADDCONTACT = 2;
	private static final int MENUM_DELETESESSION = 3;
	
	private int selectedPosition;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.messagemain);

		cl = new MessageCl(getApplicationContext());
		lstSession = getSessionData();
		havaUnRead = cl.checkIfAnyUnreadSession(-1);

		inflater = LayoutInflater.from(this);
		adapter = new SessionAdapter();
		listView = (ListView) findViewById(R.id.listView1);
		listView.setAdapter(adapter);

		receicer = new MyMsgReceicer();
		IntentFilter filter = new IntentFilter();
		filter.addAction(MessageReceiver.FRESHVIEW);
		this.registerReceiver(receicer, filter);

		updateTitle();
		
		registerForContextMenu(listView);
		
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				selectedPosition = arg2;
				return false;
			}
		});

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
				Message message = adapter.getItem(position);

				Intent intent = new Intent(MessageActivity.this, ChatActivity.class);
				intent.putExtra("address", message.getNumber());
				startActivity(intent);
				if (havaUnRead) {
					if (cl.checkIfAnyUnreadSession(message.getThreadId())) {
						// set all chat in this session to READ
						cl.updateMsgReadStatus(message.getThreadId(), 1);
						// check if There are other UNREAD sessions
						if (cl.checkIfAnyUnreadSession(-1)) {
							// do nothing
						} else {
							havaUnRead = false;
							if (MessageReceiver.nm != null) {
								MessageReceiver.nm.cancel(UNREAD);
							}
						}
					}
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENUM_NEWSESSION, 0, getApplicationContext().getString(R.string.msg_menu0));
		//menu.add(0, MENUM_CALL, 0, getApplicationContext().getString(R.string.contact_call1));
		//menu.add(0, MENUM_ADDCONTACT, 0, getApplicationContext().getString(R.string.msg_menu1));
		//menu.add(0, MENUM_DELETESESSION, 0, getApplicationContext().getString(R.string.msg_menu2));
		return true;
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		freshListView();
	}

	public void freshListView() {
		if (cl == null) {
			cl = new MessageCl(getApplicationContext());
		}

		lstSession = getSessionData();

		adapter = new SessionAdapter();
		adapter.notifyDataSetChanged();
		listView.setAdapter(adapter);
		updateTitle();
	}

	public void deleteSession() {
		int position = selectedPosition;
		Message message = (Message) listView.getAdapter().getItem(position);
		cl.deleteSession(message.getThreadId());
		lstSession.remove(position);
		adapter = new SessionAdapter();
		listView.setAdapter(adapter);
		updateTitle();

        if (cl.checkIfAnyUnreadSession(-1)) {
			// do nothing
		} else {
			havaUnRead = false;
			if (MessageReceiver.nm != null) {
				MessageReceiver.nm.cancel(UNREAD);
			}
		}
	}

	class MyMsgReceicer extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int fresh = intent.getIntExtra("fresh", 0);
			if (fresh == 200) {
				havaUnRead = true;
				freshListView();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int sessionCount = adapter.getCount();

		switch (item.getItemId()) {
		case MENUM_NEWSESSION:
			Intent intent0 = new Intent(MessageActivity.this, NewMsgActivity.class);
			startActivity(intent0);
			break;
		case MENUM_CALL:
			if (sessionCount > 0) {
				makeCall();
			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.msg_nochat),
						Toast.LENGTH_SHORT).show();
			}
			break;
		case MENUM_ADDCONTACT:
			if (sessionCount > 0) {
				Intent intent1 = new Intent(MessageActivity.this, AddContactActivity.class);
				Message message = (Message) listView.getAdapter().getItem(selectedPosition);
				intent1.putExtra("address", message.getNumber());
				startActivity(intent1);
			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.msg_nochat),
						Toast.LENGTH_SHORT).show();
			}
			break;
		case MENUM_DELETESESSION:
			if (sessionCount > 0) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(getApplicationContext().getString(R.string.chat_careful));
				builder.setMessage(getApplicationContext().getString(R.string.chat_oper));
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setPositiveButton(getApplicationContext().getString(R.string.chat_oper_ok),
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								deleteSession();
							}
						});
				builder.setNegativeButton(getApplicationContext().getString(R.string.chat_oper_cancle),
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						});
				builder.show();

			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.msg_nochat),
						Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}
		return true;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(getApplicationContext().getString(R.string.tview_main_sms));
		menu.add(0, MENUM_CALL, 0, getApplicationContext().getString(R.string.contact_call1));
		menu.add(0, MENUM_ADDCONTACT, 0, getApplicationContext().getString(R.string.msg_menu1));
		menu.add(0, MENUM_DELETESESSION, 0, getApplicationContext().getString(R.string.msg_menu2));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		int sessionCount = adapter.getCount();

		switch (item.getItemId()) {
		case MENUM_CALL:
			if (sessionCount > 0) {
				if (StateManager.getCurrentRegState() == EnumLoginState.REGISTERE_SUCCESS) {
					makeCall();
				}else{
					Toast.makeText(MessageActivity.this, getString(R.string.register_not), Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.msg_nochat),
						Toast.LENGTH_SHORT).show();
			}
			break;
		case MENUM_ADDCONTACT:
			if (sessionCount > 0) {
				Intent intent1 = new Intent(MessageActivity.this, AddContactActivity.class);
				Message message = (Message) listView.getAdapter().getItem(selectedPosition);
				intent1.putExtra("address", message.getNumber());
				startActivity(intent1);
			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.msg_nochat),
						Toast.LENGTH_SHORT).show();
			}
			break;
		case MENUM_DELETESESSION:
			if (sessionCount > 0) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(getApplicationContext().getString(R.string.chat_careful));
				builder.setMessage(getApplicationContext().getString(R.string.chat_oper));
				builder.setIcon(android.R.drawable.ic_dialog_alert);
				builder.setPositiveButton(getApplicationContext().getString(R.string.chat_oper_ok),
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								deleteSession();
							}
						});
				builder.setNegativeButton(getApplicationContext().getString(R.string.chat_oper_cancle),
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						});
				builder.show();

			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.msg_nochat),
						Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	class SessionAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			if (lstSession != null) {
				return lstSession.size();
			} else {
				return 0;
			}
		}

		@Override
		public Message getItem(int position) {
			return lstSession.get(position);
		}

		@Override
		public long getItemId(int position) {
			return lstSession.get(position).getThreadId();
		}

		@Override
		public View getView(int position, View v, ViewGroup parent) {
			v = (View) inflater.inflate(R.layout.chatlist, null);
			addressTV = (TextView) v.findViewById(R.id.chat_to_num);
			msgnumsTV = (TextView) v.findViewById(R.id.chat_nums);
			msgbodyTV = (TextView) v.findViewById(R.id.chat_new_text);
			newchattime = (TextView) v.findViewById(R.id.chat_latest_time);

			Message message = lstSession.get(position);
			int msgNums = cl.getMsgsNumInChat(message.getNumber());
			addressTV.setText(message.getNumber());
			msgbodyTV.setText(message.getBody());
			newchattime.setText(message.getDate());
			msgnumsTV.setText("(" + msgNums + ")");
			if (cl.checkIfAnyUnreadSession(message.getThreadId())) {
				v.setBackgroundColor(Color.BLUE);
			}
			return v;
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receicer);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case 93: // KEYCODE_W_CALL
		case KeyEvent.KEYCODE_CALL:
			makeCall();
			return true;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void makeCall() {

		int sessionCount = adapter.getCount();
		if (sessionCount <= 0) {
			AlertDialogManager.getInstance().toastNumberNull();
			return;
		}

		String strNum = adapter.getItem(selectedPosition).getNumber();

		// String strNum = etNumber.getText().toString();
		// check number
		if (!PTTUtil.getInstance().checkNumber(strNum)) {
			AlertDialogManager.getInstance().toastNumberNull();
			return;
		}

		// place call
		NumberInfo numberInfo = new NumberInfo();
		numberInfo.setNumber(strNum);
		numberInfo.setNumberType(PTTConstant.NUMBER_DIGIT);
		numberInfo.setPttType(PTTConstant.PTT_SINGLE);

		Intent intent = new Intent(PTTConstant.ACTION_COMMAND);
		// intent.setClass(MessageActivity.this, PTTService.class);
		intent.putExtra(PTTConstant.KEY_SERVICE_COMMAND, PTTConstant.COMM_MAKE_CALL);
		intent.putExtra("number_info", numberInfo);
		// startService(intent);
		PTTService.instance.sendPttCommand(intent);

	}

	private List<Message> getSessionData() {
		if (cl == null) {
			cl = new MessageCl(this);
		}
		return cl.getSessionList();
	}

	private void updateTitle() {
		int count = 0;
		if (lstSession != null) {
			count = lstSession.size();
		}
		MessageActivity.this.setTitle(getApplicationContext().getString(R.string.msg_title1) + count
				+ getApplicationContext().getString(R.string.msg_title2));
	}

}