/**
 * Copyright 1012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : PBKActivity
 * Author : wangjunhui
 * Version : 1.0
 * Date : 1012-04-26
 */
package com.zzy.ptt.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.db.ContactUserCl;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.NumberInfo;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author wangjunhui
 * 
 */
public class PBKActivity extends BaseActivity {
	private ListView user_lv = null;
	private ContactUserCl ubc = null;
	private ArrayList<HashMap<String, String>> userlistinit;
	private int totalNum;// contact num
	private int listID = -1;
	// private SimpleAdapter adapter;
	private MyAdapter adapter;
	private static final String LOG_TAG = "PBKActivity";
	private String strNum;

	private View loadMoreView;
	//private TextView loadmoretv;
	private Button loadmoreBtn;
	private int visibleLastIndex = 0;
	private Handler handler = new Handler();
	private LayoutInflater inflater;
	private ArrayList<HashMap<String, String>> userlistadd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PTTUtil.getInstance().initOnCreat(this);
		this.setContentView(R.layout.pkbmain);
		PTTUtil.getInstance().initOnCreat(this);
		user_lv = (ListView) findViewById(R.id.lv_userlist);
		userlistinit = new ArrayList<HashMap<String, String>>();
		userlistadd = new ArrayList<HashMap<String, String>>();
		inflater = LayoutInflater.from(getApplicationContext());

		loadMoreView = getLayoutInflater().inflate(R.layout.load_more, null);
		//loadmoretv = (TextView) loadMoreView.findViewById(R.id.loadmoretv);
		loadmoreBtn = (Button) loadMoreView.findViewById(R.id.loadmorebtn);
		
		loadmoreBtn.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (loadMoreView.isShown()
							&& !loadmoreBtn.getText().toString()
									.contains(getApplicationContext().getString(R.string.loading2))) {
						loadMore();
					}
				}
				return false;
			}
		});

		user_lv.addFooterView(loadMoreView);
		user_lv.setOnScrollListener(new AbsListView.OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				int itemsLastIndex = adapter.getCount() - 1;
				int lastIndex = itemsLastIndex + 1;
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE && visibleLastIndex == lastIndex) {
					// loadMore();
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				// TODO Auto-generated method stub
				visibleLastIndex = firstVisibleItem + visibleItemCount - 1;
			}
		});

		user_lv.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
				listID = arg2;
				if (listID <= (adapter.getCount()-1)) {
					HashMap<String, String> map0 = (HashMap<String, String>) userlistinit.get(listID);
					String phonenum = (String) map0.get("cellphone");
					strNum = phonenum;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
		
		user_lv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				listID = arg2;
				if (listID <= (adapter.getCount()-1)) {
					HashMap<String, String> map0 = (HashMap<String, String>) userlistinit.get(listID);
					String phonenum = (String) map0.get("cellphone");
					strNum = phonenum;
				}
				return false;
			}
		});
		
		registerForContextMenu(user_lv);

		ubc = new ContactUserCl(this);
		userlistinit = ubc.getUserListInit();
		this.loadListView(userlistinit);
		if (adapter.getCount() < 20) {
			loadMoreView.setVisibility(View.GONE);
		}else{
			loadMoreView.setVisibility(View.VISIBLE);
		}
	}

	public void loadMore() {
		loadmoreBtn.setText(getApplicationContext().getString(R.string.loading1));
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {

				loadData();

				adapter.notifyDataSetChanged();
				user_lv.setSelection(visibleLastIndex);

				loadmoreBtn.setText(getApplicationContext().getString(R.string.loading3));
			}
		}, 1000);
	}
	
	private void loadData() {
		userlistadd = ubc.getUserListAdd();
		if (userlistadd.size() > 0) {
			for (int i = 0; i < userlistadd.size(); i++) {
				adapter.addItem(userlistadd.get(i));
			}
		}else{
			Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.loading4),
					Toast.LENGTH_LONG).show();
		}
	}

	// @Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		userlistinit = ubc.getUserListInit();
		ubc.resetIndex();
		this.loadListView(userlistinit);
		if (adapter.getCount() < 20) {
			loadMoreView.setVisibility(View.GONE);
		}else{
			loadMoreView.setVisibility(View.VISIBLE);
		}
	}

	public void loadListView(final ArrayList<HashMap<String, String>> userlist) {
		totalNum = ubc.getTotalUserNum("");
		this.setTitle(getApplicationContext().getString(R.string.contact_total1) + totalNum
				+ getApplicationContext().getString(R.string.contact_total2) + "");

		// ArrayList<HashMap<String,String>> userlist=ubc.getUserList();
		// adapter = new SimpleAdapter(this, userlist, R.layout.contactuserlist,
		// new String[] { "imageId", "userName",
		// "cellphone" }, new int[] { R.id.list_item_image, R.id.first_ll_name,
		// R.id.second_ll_phone });
		adapter = new MyAdapter(this, userlist);
		user_lv.setAdapter(adapter);
		user_lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				// TODO Auto-generated method stub
				Intent intent = getIntent();
				if (intent.getIntExtra("getnum", 0) > 0 && position <= (userlist.size()-1)) {
					HashMap<String, String> mapa = (HashMap<String, String>) userlist.get(position);
					String phonenum = (String) mapa.get("cellphone");
					intent.putExtra("phonenum", phonenum);
					setResult(200, intent);
					PBKActivity.this.finish();
				}
			}
		});
	}

	class MyAdapter extends BaseAdapter {
		ArrayList<HashMap<String, String>> userlist;

		public MyAdapter(Context context, ArrayList<HashMap<String, String>> userlist) {
			// TODO Auto-generated constructor stub
			this.userlist = userlist;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return userlist.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View v, ViewGroup parent) {
			// TODO Auto-generated method stub
			v = inflater.inflate(R.layout.contactuserlist, null);
			TextView nameTV = (TextView) v.findViewById(R.id.first_ll_name);
			TextView phoneTV = (TextView) v.findViewById(R.id.second_ll_phone);
			HashMap<String, String> map = userlist.get(position);
			String phonenum = (String) map.get("cellphone");
			String userName = (String) map.get("userName");
			nameTV.setText(userName);
			phoneTV.setText(phonenum);
			return v;
		}

		public void addItem(HashMap<String, String> map) {
			userlist.add(map);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		//menu.add(0, 0, 0, getApplicationContext().getString(R.string.contact_call) + "");
		//menu.add(0, 1, 0, getApplicationContext().getString(R.string.contact_msg) + "");
		menu.add(0, 2, 0, getApplicationContext().getString(R.string.contact_add) + "");
		//menu.add(0, 3, 0, getApplicationContext().getString(R.string.contact_edit) + "");
		//menu.add(0, 4, 0, getApplicationContext().getString(R.string.contact_delete) + "");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case 0:
			if (listID >= 0) {
				if (listID <= (adapter.getCount()-1)) {
					HashMap<String, String> map0 = (HashMap<String, String>) userlistinit.get(listID);
					String phonenum = (String) map0.get("cellphone");
					strNum = phonenum;
					makeCall();
				}
			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.nopeople) + "",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case 1:
			if (listID >= 0) {
				if (listID <= (adapter.getCount()-1)) {
					HashMap<String, String> map0 = (HashMap<String, String>) userlistinit.get(listID);
					String phonenum = (String) map0.get("cellphone");
					Intent it = new Intent(PBKActivity.this, NewMsgActivity.class);
					it.putExtra("num", phonenum);
					startActivity(it);
				}

			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.nopeople) + "",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case 2:
			Intent it2 = new Intent();
			it2.setClass(PBKActivity.this, AddContactActivity.class);
			startActivityForResult(it2, 0);
			break;
		case 3:
			if (listID >= 0) {
				if (listID <= (adapter.getCount()-1)) {
					HashMap<String, String> map3 = (HashMap<String, String>) userlistinit.get(listID);
					Intent it = new Intent();
					it.putExtra("userInfo", map3);
					it.setClass(PBKActivity.this, EditContactActivity.class);
					startActivity(it);
				}
			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.nopeople) + "",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case 4:
			if (listID >= 0) {
				if (listID <= (adapter.getCount()-1)) {
					final HashMap<String, String> map4 = (HashMap<String, String>) userlistinit.get(listID);
					final AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(getApplicationContext().getString(R.string.alert) + "");
					builder.setMessage(getApplicationContext().getString(R.string.sure) + "");
					builder.setIcon(android.R.drawable.ic_dialog_alert);
					builder.setPositiveButton(getApplicationContext().getString(R.string.ok) + "",
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (ubc.delete(Integer.parseInt(map4.get("_id").toString()))) {
										Toast.makeText(PBKActivity.this,
												getApplicationContext().getString(R.string.contact_del_success) + "",
												Toast.LENGTH_SHORT).show();
										setResult(1);
										/*
										 * PBKActivity.this.finish(); Intent
										 * intent = new Intent(
										 * PBKActivity.this, PBKActivity.class);
										 * startActivity(intent);
										 */
										userlistinit = ubc.getUserListInit();
										ubc.resetIndex();
										adapter.notifyDataSetChanged();
										user_lv.setAdapter(adapter);
										PBKActivity.this.loadListView(userlistinit);
										listID = listID - 1;
										if (adapter.getCount() < 20) {
											loadMoreView.setVisibility(View.GONE);
										}else{
											loadMoreView.setVisibility(View.VISIBLE);
										}
									} else {
										Toast.makeText(PBKActivity.this,
												getApplicationContext().getString(R.string.contact_del_faulte) + "",
												Toast.LENGTH_SHORT).show();
									}
								}
							});
					builder.setNegativeButton(getApplicationContext().getString(R.string.cancle) + "",
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
								}
							});
					builder.show();
				}
			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.nopeople) + "",
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
		menu.setHeaderTitle(getApplicationContext().getString(R.string.tview_main_pbk) + "");
		menu.add(0, 0, 0, getApplicationContext().getString(R.string.contact_call) + "");
		menu.add(0, 1, 0, getApplicationContext().getString(R.string.contact_msg) + "");
		menu.add(0, 2, 0, getApplicationContext().getString(R.string.contact_edit) + "");
		menu.add(0, 3, 0, getApplicationContext().getString(R.string.contact_delete) + "");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case 0:
			if (listID >= 0) {
				if (listID <= (adapter.getCount() - 1)) {
					HashMap<String, String> map0 = (HashMap<String, String>) userlistinit.get(listID);
					String phonenum = (String) map0.get("cellphone");
					strNum = phonenum;
					if (StateManager.getCurrentRegState() == EnumLoginState.REGISTERE_SUCCESS) {
						makeCall();
					}else{
						Toast.makeText(PBKActivity.this, getString(R.string.register_not), Toast.LENGTH_SHORT).show();
					}
				}
			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.nopeople) + "",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case 1:
			if (listID >= 0) {
				if (listID <= (adapter.getCount() - 1)) {
					HashMap<String, String> map0 = (HashMap<String, String>) userlistinit.get(listID);
					String phonenum = (String) map0.get("cellphone");
					Intent it = new Intent(PBKActivity.this, NewMsgActivity.class);
					it.putExtra("num", phonenum);
					startActivity(it);
				}

			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.nopeople) + "",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case 2:
			if (listID >= 0) {
				if (listID <= (adapter.getCount() - 1)) {
					HashMap<String, String> map3 = (HashMap<String, String>) userlistinit.get(listID);
					Intent it = new Intent();
					it.putExtra("userInfo", map3);
					it.setClass(PBKActivity.this, EditContactActivity.class);
					startActivity(it);
				}
			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.nopeople) + "",
						Toast.LENGTH_SHORT).show();
			}
			break;
		case 3:
			if (listID >= 0) {
				if (listID <= (adapter.getCount() - 1)) {
					final HashMap<String, String> map4 = (HashMap<String, String>) userlistinit.get(listID);
					final AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(getApplicationContext().getString(R.string.alert) + "");
					builder.setMessage(getApplicationContext().getString(R.string.sure) + "");
					builder.setIcon(android.R.drawable.ic_dialog_alert);
					builder.setPositiveButton(getApplicationContext().getString(R.string.ok) + "",
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (ubc.delete(Integer.parseInt(map4.get("_id").toString()))) {
										Toast.makeText(PBKActivity.this,
												getApplicationContext().getString(R.string.contact_del_success) + "",
												Toast.LENGTH_SHORT).show();
										setResult(1);

										PBKActivity.this.finish();
										Intent intent = new Intent(PBKActivity.this, PBKActivity.class);
										startActivity(intent);

										userlistinit = ubc.getUserListInit();
										ubc.resetIndex();
										adapter.notifyDataSetChanged();
										user_lv.setAdapter(adapter);
										PBKActivity.this.loadListView(userlistinit);
										listID = listID - 1;
										if (adapter.getCount() < 20) {
											loadMoreView.setVisibility(View.GONE);
										} else {
											loadMoreView.setVisibility(View.VISIBLE);
										}
									} else {
										Toast.makeText(PBKActivity.this,
												getApplicationContext().getString(R.string.contact_del_faulte) + "",
												Toast.LENGTH_SHORT).show();
									}
								}
							});
					builder.setNegativeButton(getApplicationContext().getString(R.string.cancle) + "",
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
								}
							});
					builder.show();
				}
			} else {
				Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.nopeople) + "",
						Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == 0 || requestCode == 3) {
			if (resultCode == 1) {
				userlistinit = ubc.getUserListInit();
				this.loadListView(userlistinit);
			} else if (resultCode == 2) {
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && loadMoreView.isShown()
				&& !loadmoreBtn.getText().toString().contains(getApplicationContext().getString(R.string.loading2))) {
			loadMore();
		}
		switch (keyCode) {
		case 93: // KEYCODE_W_CALL //FIXME
		case KeyEvent.KEYCODE_CALL:
			makeCall();
			return true;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void makeCall() {

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

		Log.d(LOG_TAG, "Dial num : " + strNum);
		Intent intent = new Intent(PTTConstant.ACTION_COMMAND);
//		intent.setClass(PBKActivity.this, PTTService.class);
		intent.putExtra(PTTConstant.KEY_SERVICE_COMMAND,
				PTTConstant.COMM_MAKE_CALL);
		intent.putExtra("number_info", numberInfo);
//		startService(intent);
		PTTService.instance.sendPttCommand(intent);
	}

}
