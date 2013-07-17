/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Projectzzy PTT V1.0
 * NameCallLogActivity.java
 * AuthorLiXiaodong
 * Version1.0
 * Date2012-3-5
 */

package com.zzy.ptt.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.db.DBHelper;
import com.zzy.ptt.model.CallLog;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.NumberInfo;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class CallLogTabActivity extends TabActivity implements TabContentFactory,
		OnTabChangeListener {

	private TabHost tabHost;
	private MyAdapter adapter;
	private ListView listView;
	private List<CallLog> tempListall;
	private List<HashMap<String, Object>> lstAllData;
	private List<HashMap<String, Object>> lstMissingData;
	private List<HashMap<String, Object>> lstAnswerData;
	private List<HashMap<String, Object>> lstOutData;

	private static final String LOG_TAG = "CallLogTabActivity";

	private static final int MENU_DELETE_ALL = 2;

	private static final String TAB_CALLOUT = "1";
	private static final String TAB_CALLIN = "2";
	private static final String TAB_CALLMISS = "3";
	private static final String TAB_CALLALL = "4";

	private LayoutInflater inflater;
	private View loadMoreView;
	//private TextView loadmoretv;
	private Button loadmoreBtn;
	private int listPosition = -1;
	private Handler handler = new Handler();
	private int visibleLastIndex = 0;
	private List<HashMap<String, Object>> lstAllDataAdd;
	private List<HashMap<String, Object>> lstMissingDataAdd;
	private List<HashMap<String, Object>> lstAnswerDataAdd;
	private List<HashMap<String, Object>> lstOutDataAdd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PTTUtil.getInstance().initOnCreat(this);

		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		listView = new ListView(this);
		lstAllData = new ArrayList<HashMap<String, Object>>();
		lstMissingData = new ArrayList<HashMap<String, Object>>();
		lstAnswerData = new ArrayList<HashMap<String, Object>>();
		lstOutData = new ArrayList<HashMap<String, Object>>();

		lstAllDataAdd = new ArrayList<HashMap<String, Object>>();
		lstMissingDataAdd = new ArrayList<HashMap<String, Object>>();
		lstAnswerDataAdd = new ArrayList<HashMap<String, Object>>();
		lstOutDataAdd = new ArrayList<HashMap<String, Object>>();

		inflater = LayoutInflater.from(getApplicationContext());
		loadMoreView = getLayoutInflater().inflate(R.layout.load_more, null);
		//loadmoretv = (TextView) loadMoreView.findViewById(R.id.loadmoretv);
		loadmoreBtn = (Button) loadMoreView.findViewById(R.id.loadmorebtn);
		listView.addFooterView(loadMoreView);
		initWidgets();
		
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
		
		registerForContextMenu(listView);

		listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
				if (arg2 >= adapter.getCount()) {
					listPosition = -1;
				} else {
					listPosition = arg2;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub

			}
		});
		
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (arg2 >= adapter.getCount()) {
					listPosition = -1;
				} else {
					listPosition = arg2;
				}
				return false;
			}
		});

		listView.setOnScrollListener(new AbsListView.OnScrollListener() {

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				int itemsLastIndex = adapter.getCount() - 1;
				int lastIndex = itemsLastIndex + 1;
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE
						&& visibleLastIndex == lastIndex) {
					// loadMore();
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
					int totalItemCount) {
				// TODO Auto-generated method stub
				visibleLastIndex = firstVisibleItem + visibleItemCount - 1;
			}
		});

	}

	public void loadMore() {
		loadmoreBtn.setText(getApplicationContext().getString(R.string.loading1));
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {

				loadData();

				adapter.notifyDataSetChanged();
				listView.setSelection(visibleLastIndex);

				loadmoreBtn.setText(getApplicationContext().getString(R.string.loading3));
			}
		}, 1000);
	}

	private void loadData() {
		List<HashMap<String, Object>> temp = new ArrayList<HashMap<String, Object>>();
		String tagId = tabHost.getCurrentTabTag();
		getDataAdd(tagId);
		if (tagId.equals(TAB_CALLIN)) {
			temp = lstAnswerDataAdd;
		}
		if (tagId.equals(TAB_CALLOUT)) {
			temp = lstOutDataAdd;
		}
		if (tagId.equals(TAB_CALLMISS)) {
			temp = lstMissingDataAdd;
		}
		if (tagId.equals(TAB_CALLALL)) {
			temp = lstAllDataAdd;
		}
		if (temp.size() > 0) {
			for (int i = 0; i < temp.size(); i++) {
				adapter.addItem(temp.get(i));
			}
		} else {
			Toast.makeText(getApplicationContext(),
					getApplicationContext().getString(R.string.loading4), Toast.LENGTH_LONG).show();
		}
	}

	private void initWidgets() {
		tabHost = getTabHost();
		tabHost.addTab(tabHost.newTabSpec(TAB_CALLOUT)
				.setIndicator("", getResources().getDrawable(R.drawable.ic_call_log_outgoing_call))
				.setContent(this));
		tabHost.addTab(tabHost.newTabSpec(TAB_CALLIN)
				.setIndicator("", getResources().getDrawable(R.drawable.ic_call_log_incoming_call))
				.setContent(this));
		tabHost.addTab(tabHost.newTabSpec(TAB_CALLMISS)
				.setIndicator("", getResources().getDrawable(R.drawable.ic_call_log_missed_call))
				.setContent(this));
		tabHost.addTab(tabHost.newTabSpec(TAB_CALLALL)
				.setIndicator("", getResources().getDrawable(R.drawable.ic_call_log_all))
				.setContent(this));
		tabHost.setCurrentTab(PTTConstant.LOG_OUT);
		TabWidget tabWidget = tabHost.getTabWidget();
		for (int i = 0; i < tabWidget.getChildCount(); i++) {
			tabWidget.getChildAt(i).getLayoutParams().height = 80;
		}
		tabHost.setOnTabChangedListener(this);

	}

	private void getDataAdd(String tagId) {

		if (tagId.equals(TAB_CALLIN)) {
			lstAnswerDataAdd.clear();
			tempListall = new DBHelper(this).queryCallLogAdd(PTTConstant.LOG_ANSWERED);
			if (tempListall == null) {
				return;
			}
			int size = tempListall.size();
			HashMap<String, Object> objMap = null;
			CallLog callLog = null;
			for (int i = 0; i < size; i++) {
				objMap = new HashMap<String, Object>();
				callLog = tempListall.get(i);
				objMap.put(PTTConstant.TABLE_CALLLOG_NUM, callLog.getNum());
				objMap.put(PTTConstant.TABLE_CALLLOG_ID, callLog.getId());
				objMap.put(PTTConstant.TABLE_CALLLOG_TIME, callLog.getTime().toLocaleString());
				objMap.put(PTTConstant.TABLE_CALLLOG_DURATION,
						generateDuration(callLog.getDuration()));
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE, callLog.getLogType());
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE_ICON,
						getCorrectIcon(callLog.getLogType()));
				lstAnswerDataAdd.add(objMap);
			}
		}
		if (tagId.equals(TAB_CALLOUT)) {
			lstOutDataAdd.clear();
			tempListall = new DBHelper(this).queryCallLogAdd(PTTConstant.LOG_OUT);
			if (tempListall == null) {
				return;
			}

			int size = tempListall.size();
			HashMap<String, Object> objMap = null;
			CallLog callLog = null;
			for (int i = 0; i < size; i++) {
				objMap = new HashMap<String, Object>();
				callLog = tempListall.get(i);
				objMap.put(PTTConstant.TABLE_CALLLOG_NUM, callLog.getNum());
				objMap.put(PTTConstant.TABLE_CALLLOG_ID, callLog.getId());
				objMap.put(PTTConstant.TABLE_CALLLOG_TIME, callLog.getTime().toLocaleString());
				objMap.put(PTTConstant.TABLE_CALLLOG_DURATION,
						generateDuration(callLog.getDuration()));
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE, callLog.getLogType());
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE_ICON,
						getCorrectIcon(callLog.getLogType()));
				lstOutDataAdd.add(objMap);
			}
		}
		if (tagId.equals(TAB_CALLMISS)) {
			lstMissingDataAdd.clear();
			tempListall = new DBHelper(this).queryCallLogAdd(PTTConstant.LOG_MISSING);
			if (tempListall == null) {
				return;
			}

			int size = tempListall.size();
			HashMap<String, Object> objMap = null;
			CallLog callLog = null;
			for (int i = 0; i < size; i++) {
				objMap = new HashMap<String, Object>();
				callLog = tempListall.get(i);
				objMap.put(PTTConstant.TABLE_CALLLOG_NUM, callLog.getNum());
				objMap.put(PTTConstant.TABLE_CALLLOG_ID, callLog.getId());
				objMap.put(PTTConstant.TABLE_CALLLOG_TIME, callLog.getTime().toLocaleString());
				objMap.put(PTTConstant.TABLE_CALLLOG_DURATION,
						generateDuration(callLog.getDuration()));
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE, callLog.getLogType());
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE_ICON,
						getCorrectIcon(callLog.getLogType()));
				lstMissingDataAdd.add(objMap);
			}
		}
		if (tagId.equals(TAB_CALLALL)) {
			lstAllDataAdd.clear();
			tempListall = new DBHelper(this).queryCallLogAdd(PTTConstant.LOG_ALL);
			if (tempListall == null) {
				return;
			}

			int size = tempListall.size();
			HashMap<String, Object> objMap = null;
			CallLog callLog = null;
			for (int i = 0; i < size; i++) {
				objMap = new HashMap<String, Object>();
				callLog = tempListall.get(i);
				objMap.put(PTTConstant.TABLE_CALLLOG_NUM, callLog.getNum());
				objMap.put(PTTConstant.TABLE_CALLLOG_ID, callLog.getId());
				objMap.put(PTTConstant.TABLE_CALLLOG_TIME, callLog.getTime().toLocaleString());
				objMap.put(PTTConstant.TABLE_CALLLOG_DURATION,
						generateDuration(callLog.getDuration()));
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE, callLog.getLogType());
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE_ICON,
						getCorrectIcon(callLog.getLogType()));
				lstAllDataAdd.add(objMap);
			}
		}
	}

	private void getData(String tagId) {

		if (tagId.equals(TAB_CALLIN)) {
			tempListall = new DBHelper(this).queryCallLog(PTTConstant.LOG_ANSWERED);
			if (tempListall == null) {
				return;
			}
			lstAnswerData.clear();
			int size = tempListall.size();
			HashMap<String, Object> objMap = null;
			CallLog callLog = null;
			for (int i = 0; i < size; i++) {
				objMap = new HashMap<String, Object>();
				callLog = tempListall.get(i);
				objMap.put(PTTConstant.TABLE_CALLLOG_NUM, callLog.getNum());
				objMap.put(PTTConstant.TABLE_CALLLOG_ID, callLog.getId());
				objMap.put(PTTConstant.TABLE_CALLLOG_TIME, callLog.getTime().toLocaleString());
				objMap.put(PTTConstant.TABLE_CALLLOG_DURATION,
						generateDuration(callLog.getDuration()));
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE, callLog.getLogType());
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE_ICON,
						getCorrectIcon(callLog.getLogType()));
				lstAnswerData.add(objMap);
			}
		}
		if (tagId.equals(TAB_CALLOUT)) {
			tempListall = new DBHelper(this).queryCallLog(PTTConstant.LOG_OUT);
			if (tempListall == null) {
				return;
			}
			lstOutData.clear();
			int size = tempListall.size();
			HashMap<String, Object> objMap = null;
			CallLog callLog = null;
			for (int i = 0; i < size; i++) {
				objMap = new HashMap<String, Object>();
				callLog = tempListall.get(i);
				objMap.put(PTTConstant.TABLE_CALLLOG_NUM, callLog.getNum());
				objMap.put(PTTConstant.TABLE_CALLLOG_ID, callLog.getId());
				objMap.put(PTTConstant.TABLE_CALLLOG_TIME, callLog.getTime().toLocaleString());
				objMap.put(PTTConstant.TABLE_CALLLOG_DURATION,
						generateDuration(callLog.getDuration()));
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE, callLog.getLogType());
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE_ICON,
						getCorrectIcon(callLog.getLogType()));
				lstOutData.add(objMap);
			}
		}
		if (tagId.equals(TAB_CALLMISS)) {
			tempListall = new DBHelper(this).queryCallLog(PTTConstant.LOG_MISSING);
			if (tempListall == null) {
				return;
			}
			lstMissingData.clear();
			int size = tempListall.size();
			HashMap<String, Object> objMap = null;
			CallLog callLog = null;
			for (int i = 0; i < size; i++) {
				objMap = new HashMap<String, Object>();
				callLog = tempListall.get(i);
				objMap.put(PTTConstant.TABLE_CALLLOG_NUM, callLog.getNum());
				objMap.put(PTTConstant.TABLE_CALLLOG_ID, callLog.getId());
				objMap.put(PTTConstant.TABLE_CALLLOG_TIME, callLog.getTime().toLocaleString());
				objMap.put(PTTConstant.TABLE_CALLLOG_DURATION,
						generateDuration(callLog.getDuration()));
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE, callLog.getLogType());
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE_ICON,
						getCorrectIcon(callLog.getLogType()));
				lstMissingData.add(objMap);
			}
		}
		if (tagId.equals(TAB_CALLALL)) {
			tempListall = new DBHelper(this).queryCallLog(PTTConstant.LOG_ALL);
			if (tempListall == null) {
				return;
			}
			lstAllData.clear();
			int size = tempListall.size();
			HashMap<String, Object> objMap = null;
			CallLog callLog = null;
			for (int i = 0; i < size; i++) {
				objMap = new HashMap<String, Object>();
				callLog = tempListall.get(i);
				objMap.put(PTTConstant.TABLE_CALLLOG_NUM, callLog.getNum());
				objMap.put(PTTConstant.TABLE_CALLLOG_ID, callLog.getId());
				objMap.put(PTTConstant.TABLE_CALLLOG_TIME, callLog.getTime().toLocaleString());
				objMap.put(PTTConstant.TABLE_CALLLOG_DURATION,
						generateDuration(callLog.getDuration()));
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE, callLog.getLogType());
				objMap.put(PTTConstant.TABLE_CALLLOG_LOGTYPE_ICON,
						getCorrectIcon(callLog.getLogType()));
				lstAllData.add(objMap);
			}
		}
	}

	private List<HashMap<String, Object>> getCurrentListData() {
		String tagId = tabHost.getCurrentTabTag();
		if (tagId.equals(TAB_CALLIN)) {
			return lstAnswerData;
		}
		if (tagId.equals(TAB_CALLOUT)) {
			return lstOutData;
		}
		if (tagId.equals(TAB_CALLMISS)) {
			return lstMissingData;
		}
		if (tagId.equals(TAB_CALLALL)) {
			return lstAllData;
		}
		return lstAllData;
	}

	private String generateDuration(long duration) {

		if (duration == 0) {
			return "";
		}
		return PTTUtil.getInstance().duriation2String(duration);
	}

	private Object getCorrectIcon(int logType) {
		int iconId = 0;
		switch (logType) {
		case PTTConstant.LOG_ANSWERED:
			iconId = R.drawable.ic_call_log_incoming_call1;
			break;
		case PTTConstant.LOG_MISSING:
			iconId = R.drawable.ic_call_log_missed_call1;
			break;
		case PTTConstant.LOG_OUT:
			iconId = R.drawable.ic_call_log_outgoing_call1;
			break;
		default:
			iconId = R.drawable.ic_call_log_all1;
			break;
		}
		return iconId;
	}

	@Override
	public View createTabContent(String tagId) {
		adapter = new MyAdapter(tagId);
		listView.setAdapter(adapter);
		if (adapter.getCount() < 20) {
			loadMoreView.setVisibility(View.GONE);
		} else {
			loadMoreView.setVisibility(View.VISIBLE);
		}
		return listView;
	}

	@Override
	public void onTabChanged(String tabId) {

		Log.d(LOG_TAG, "go to tab : " + tabId);
		if (tabId == null || tabId.trim().length() == 0) {
			return;
		}
		UpdateView(tabId);
		new DBHelper(this).resetIndex();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		UpdateView(tabHost.getCurrentTabTag());
		new DBHelper(this).resetIndex();
	}

	private void UpdateView(String tagId) {
		adapter = new MyAdapter(tagId);
		adapter.notifyDataSetChanged();
		listView.setAdapter(adapter);
		if (adapter.getCount() < 20) {
			loadMoreView.setVisibility(View.GONE);
		} else {
			loadMoreView.setVisibility(View.VISIBLE);
		}
	}

	class MyAdapter extends BaseAdapter {
		List<HashMap<String, Object>> temp = new ArrayList<HashMap<String, Object>>();

		public MyAdapter(String tagId) {
			getData(tagId);
			if (tagId.equals(TAB_CALLIN)) {
				temp = lstAnswerData;
			}
			if (tagId.equals(TAB_CALLOUT)) {
				temp = lstOutData;
			}
			if (tagId.equals(TAB_CALLMISS)) {
				temp = lstMissingData;
			}
			if (tagId.equals(TAB_CALLALL)) {
				temp = lstAllData;
			}
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return temp.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return temp.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View v, ViewGroup parent) {
			// TODO Auto-generated method stub
			v = inflater.inflate(R.layout.calllog_item_layout, null);
			ImageView logIcon = (ImageView) v.findViewById(R.id.img_log_icon);
			TextView numTV = (TextView) v.findViewById(R.id.log_num);
			TextView timeTV = (TextView) v.findViewById(R.id.log_time);
			TextView durTV = (TextView) v.findViewById(R.id.log_duration);
			Map<String, Object> objMap = new HashMap<String, Object>();
			objMap = temp.get(position);
			String num = (String) objMap.get(PTTConstant.TABLE_CALLLOG_NUM);
			String duration = (String) objMap.get(PTTConstant.TABLE_CALLLOG_DURATION);
			String time = (String) objMap.get(PTTConstant.TABLE_CALLLOG_TIME);
			int iconId = (Integer) objMap.get(PTTConstant.TABLE_CALLLOG_LOGTYPE_ICON);
			logIcon.setBackgroundResource(iconId);
			numTV.setText(num);
			timeTV.setText(time);
			durTV.setText(duration);
			return v;
		}

		public void addItem(HashMap<String, Object> map) {
			temp.add(map);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemIndex = item.getItemId();
		HashMap<String, Object> map;
		if (itemIndex != MENU_DELETE_ALL) {
			if (listPosition < 0) {
//				alertErrorMsg("None selected");
				return true;
			}
			map = (HashMap<String, Object>) adapter.getItem(listPosition);
		} else {
			if (listPosition < 0 && adapter.getCount() == 0) {
				return true;
			}
			map = (HashMap<String, Object>) adapter.getItem(0);
		}
		if (adapter.getCount() <= 0) {
//			alertErrorMsg("None selected");
			return true;
		}

		if (map == null) {
			return true;
		}

		switch (itemIndex) {
//		case MENU_CALL:
//			String num = (String) map.get(PTTConstant.TABLE_CALLLOG_NUM);
//			if (num == null || num.trim().length() == 0) {
//				alertErrorMsg("num is null");
//			} else {
//				makeCall(num);
//			}
//			break;
//		case MENU_SENDSMS:
//			String strNum = (String) map.get(PTTConstant.TABLE_CALLLOG_NUM);
//			if (strNum == null || strNum.trim().length() == 0) {
//				alertErrorMsg("num is null");
//			} else {
//				Intent it = new Intent(CallLogTabActivity.this, NewMsgActivity.class);
//				it.putExtra("num", strNum);
//				startActivity(it);
//			}
//			break;
//		case MENU_DELETE:
//			deleteLog(map);
//			new DBHelper(this).resetIndex();
//			break;
		case MENU_DELETE_ALL:
			if (tabHost.getCurrentTab() == 3) {
				deleteAllLog();
				new DBHelper(this).resetIndex();
			} else {
				deleteTabAllLog(map);
				new DBHelper(this).resetIndex();
			}
			break;
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int order = 0;
		menu.add(0, MENU_DELETE_ALL, order++, R.string.log_delete_all);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		// TODO Auto-generated method stub
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(getString(R.string.tview_main_calllog));
		menu.add(0, 0, 0, getString(R.string.call_dialing));
		menu.add(0, 1, 0, getString(R.string.sms_send));
		menu.add(0, 2, 0, getString(R.string.log_delete));
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		int itemIndex = item.getItemId();
		HashMap<String, Object> map;
		//Toast.makeText(CallLogTabActivity.this, listPosition+"->listPosition", Toast.LENGTH_SHORT).show();
		if (listPosition < 0) {
			// alertErrorMsg("None selected");
			return true;
		}
		map = (HashMap<String, Object>) adapter.getItem(listPosition);
		if (adapter.getCount() <= 0) {
			// alertErrorMsg("None selected");
			return true;
		}

		if (map == null) {
			return true;
		}

		switch (itemIndex) {
		case 0:
			String num = (String) map.get(PTTConstant.TABLE_CALLLOG_NUM);
			if (num == null || num.trim().length() == 0) {
				alertErrorMsg("num is null");
			} else {
				if (StateManager.getCurrentRegState() == EnumLoginState.REGISTERE_SUCCESS) {
					makeCall(num);
				}else{
					Toast.makeText(CallLogTabActivity.this, getString(R.string.register_not), Toast.LENGTH_SHORT).show();
				}
			}
			break;
		case 1:
			String strNum = (String) map.get(PTTConstant.TABLE_CALLLOG_NUM);
			//Toast.makeText(CallLogTabActivity.this, strNum+"->strNum", Toast.LENGTH_SHORT).show();
			if (strNum == null || strNum.trim().length() == 0) {
				alertErrorMsg("num is null");
			} else {
				Intent it = new Intent(CallLogTabActivity.this, NewMsgActivity.class);
				it.putExtra("num", strNum);
				startActivity(it);
			}
			break;
		case 2:
			deleteLog(map);
			new DBHelper(this).resetIndex();
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
	}

	private void deleteLog(Map<String, Object> map) {

		Integer id = (Integer) map.get(PTTConstant.TABLE_CALLLOG_ID);
		boolean result = new DBHelper(this).deleteCallLog(id, false);
		if (!result) {
			alertErrorMsg("delete fail");
		} else {
			List<HashMap<String, Object>> tempList = getCurrentListData();
			tempList.remove(map);
			lstAllData.remove(map);
		}
		UpdateView(tabHost.getCurrentTabTag());
	}

	private void deleteTabAllLog(Map<String, Object> map) {
		int logType = (Integer) map.get(PTTConstant.TABLE_CALLLOG_LOGTYPE);
		boolean result = new DBHelper(this).deleteTabCallLog(logType);
		if (!result) {
			alertErrorMsg("delete fail");
		}
		UpdateView(tabHost.getCurrentTabTag());
	}

	private void deleteAllLog() {
		boolean result = new DBHelper(this).deleteCallLog(-1, true);
		if (!result) {
			alertErrorMsg("delete fail");
		}
		UpdateView(tabHost.getCurrentTabTag());
	}

	private void makeCall(String strNum) {
		if (strNum == null || strNum.trim().length() == 0) {
			alertErrorMsg("num is null");
			return;
		}
		// place call
		NumberInfo numberInfo = new NumberInfo();
		numberInfo.setNumber(strNum);
		numberInfo.setNumberType(PTTConstant.NUMBER_DIGIT);
		numberInfo.setPttType(PTTConstant.PTT_SINGLE);

		Intent intent = new Intent(PTTConstant.ACTION_COMMAND);
		//intent.setClass(CallLogTabActivity.this, PTTService.class);
		intent.putExtra(PTTConstant.KEY_SERVICE_COMMAND, PTTConstant.COMM_MAKE_CALL);
		intent.putExtra("number_info", numberInfo);
		//startService(intent);
		PTTService.instance.sendPttCommand(intent);
	}

	

	private void alertErrorMsg(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.alert_title));
		builder.setMessage(msg);
		builder.setNeutralButton(R.string.alert_btn_ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN
				&& loadMoreView.isShown()
				&& !loadmoreBtn.getText().toString()
						.contains(getApplicationContext().getString(R.string.loading2))) {
			loadMore();
		}
		switch (keyCode) {
		case KeyEvent.KEYCODE_CALL:
		case PTTConstant.KEYCODE_W_CALL:
			if (listPosition < 0) {
				//alertErrorMsg("none selected");
				return true;
			} else {
				@SuppressWarnings("unchecked")
				HashMap<String, Object> map = ((HashMap<String, Object>) listView
						.getItemAtPosition(listPosition));
				String num = (String) map.get(PTTConstant.TABLE_CALLLOG_NUM);
				if (num == null || num.trim().length() == 0) {
					alertErrorMsg("num is null");
				} else {
					makeCall(num);
				}
				return true;
			}

		default:
			break;
		}

		return super.onKeyDown(keyCode, event);
	}

}
