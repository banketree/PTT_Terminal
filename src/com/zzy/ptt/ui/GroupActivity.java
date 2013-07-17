/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : GroupActivity
 * Author : wangjunhui
 * Version : 1.0
 * Date : 2012-04-26
 */
package com.zzy.ptt.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.model.EnumLoginState;
import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.model.MemberInfo;
import com.zzy.ptt.model.NumberInfo;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.service.GroupManager;
import com.zzy.ptt.service.PTTManager;
import com.zzy.ptt.service.PTTService;
import com.zzy.ptt.service.StateManager;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author wangjunhui
 * 
 */
public class GroupActivity extends BaseActivity {

	private LinearLayout root;
	private ExpandableListView expandableListView;
	private ExpandableListAdapter mAdapter;
	private GroupReceiver groupReceiver;
	private Map<String, MemberInfo[]> groupInfoMap;
	private Map<String, Integer> groupStatusMap;
	private Map<String, String> groupNameMap;
	private List<GroupInfo> lstGroupData;
	private int freshTimeLong = 5;
	private String strNum;
	private String[] groups;
	private String[] groupString;
	private MemberInfo[][] memberInfos;
	private String[][] memberInfoString;
	private Timer timer;
	private MyTask task;

	private SharedPreferences sp;
	private Editor editor;

	public int getfreshTimeLong() {
		return freshTimeLong * 1000 * 60;
	}

	public void setfreshTimeLong(int freshTimeLong) {
		this.freshTimeLong = freshTimeLong * 1000 * 60;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.groupmain);

		groupInfoMap = new HashMap<String, MemberInfo[]>();
		groupStatusMap = new HashMap<String, Integer>();
		groupNameMap = new HashMap<String, String>();
		lstGroupData = new ArrayList<GroupInfo>();

		sp = PreferenceManager.getDefaultSharedPreferences(this);
		editor = sp.edit();
		timer = new Timer();
		task = new MyTask();

		groups = new String[] { "" };
		groupString = new String[] { "" };
		memberInfos = new MemberInfo[][] {};
		memberInfoString = new String[][] { { "" } };

		PTTUtil.getInstance().initOnCreat(this);
		expandableListView = new ExpandableListView(this);
		mAdapter = new MyExpandableListAdapter();
		expandableListView.setAdapter(mAdapter);
		registerForContextMenu(expandableListView);

		root = (LinearLayout) findViewById(R.id.root);
		LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		root.addView(expandableListView, rl);

		groupReceiver = new GroupReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(PTTConstant.ACTION_MEMBERS_INFO);
		filter.addAction(PTTConstant.ACTION_DYNAMIC_REGRP);
		this.registerReceiver(groupReceiver, filter);

		timer.schedule(task, 100, getfreshTimeLong());

		expandableListView.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				TextView tv = (TextView) arg1;
				if(tv != null){
					String a[] = tv.getText().toString().split("--");
					if (!a[0].contains("--") && a[0].length() != 0 && (a[0].getBytes().length == a[0].length())) {
						strNum = a[0];
					} else {
						strNum = null;
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
			}
		});
		
		expandableListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				TextView tv = (TextView) arg1;
				String a[] = tv.getText().toString().split("--");
				if (!a[0].contains("--") && a[0].length() != 0 && (a[0].getBytes().length == a[0].length())) {
					strNum = a[0];
				} else {
					strNum = null;
				}
				return false;
			}
		});
		Log.d("GroupActivity", "onCreate----> freshView");
		freshView();
	}

	public void requestFreshView() {
		try {
			List<GroupInfo> groupData = GroupManager.getInstance().getGroupData();
			if (groupData == null || groupData.size() == 0) {
				return;
			}
			int size = groupData.size();
			String grpNum = null;
			for (int i = 0; i < size; i++) {
				grpNum = groupData.get(i).getNumber();
				if (grpNum == null || grpNum.trim().length() == 0) {
					continue;
				}
				SipProxy.getInstance().RequestGetMember(grpNum);
			}

		} catch (PTTException e) {
			e.printStackTrace();
		}
	}

	class MyTask extends TimerTask {

		@Override
		public void run() {
			requestFreshView();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 3, 0, getApplicationContext().getString(R.string.group_menu3));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 3:
			GroupManager.getInstance().clearNumMemberMap();
			requestFreshView();
			if (GroupManager.getInstance().checkReceiveAll()) {
				freshView();
			}
			break;

		default:
			break;
		}
		return true;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) menuInfo;
		int type = ExpandableListView.getPackedPositionType(info.packedPosition);
		if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			menu.setHeaderTitle(getApplicationContext().getString(R.string.group_title_groupmember));
			menu.add(0, 0, 0, getApplicationContext().getString(R.string.group_call_groupmember));
			menu.add(0, 1, 0, getApplicationContext().getString(R.string.group_msg_groupmember));
			menu.add(0, 2, 0, getApplicationContext().getString(R.string.group_add_groupmember));
		} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			menu.setHeaderTitle(getApplicationContext().getString(R.string.group_title_group));
			menu.add(0, 3, 0, getApplicationContext().getString(R.string.group_change_to_auto));
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
		String title = ((TextView) info.targetView).getText().toString();
		String a[] = title.split("--");
		if (!a[0].contains("--") && a[0].length() != 0 && (a[0].getBytes().length == a[0].length())) {
			strNum = a[0];
		} else {
			strNum = null;
		}
		switch (item.getItemId()) {
		case 0:
			if (StateManager.getCurrentRegState() == EnumLoginState.REGISTERE_SUCCESS) {
				makeCall();
			}else{
				Toast.makeText(GroupActivity.this, getString(R.string.register_not), Toast.LENGTH_SHORT).show();
			}
			break;
		case 1:
			if (strNum != null) {
				Intent it = new Intent(GroupActivity.this, NewMsgActivity.class);
				it.putExtra("num", strNum);
				startActivity(it);
			}
			break;
		case 2:
			if (strNum != null) {
				Intent intent1 = new Intent(GroupActivity.this, AddContactActivity.class);
				intent1.putExtra("address", strNum);
				startActivity(intent1);
			}
			break;
		case 3:
			if (lstGroupData.size() != 0) {
				if (PTTManager.getInstance().getCurrentPttState().getStatus() == PTTConstant.PTT_IDLE) {
					int groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
					String groupNumString = lstGroupData.get(groupPos).getNumber();
					editor.putString(PTTConstant.SP_CURR_GRP_NUM, groupNumString);
					editor.commit();
					//PTTManager.getInstance().setDefaultGrp(groupNumString);
					GroupManager.getInstance().clearNumMemberMap();
					requestFreshView();
					if (GroupManager.getInstance().checkReceiveAll()) {
						freshView();
					}
				} else {
					Toast.makeText(
							getApplicationContext(),
							getApplicationContext().getString(R.string.setting_current_group_off1) + "\n"
									+ getApplicationContext().getString(R.string.setting_current_group_off2),
							Toast.LENGTH_SHORT).show();
				}
			}
			break;
		default:
			break;
		}

		return false;
	}

	public class MyExpandableListAdapter extends BaseExpandableListAdapter {

		public Object getChild(int groupPosition, int childPosition) {
			return memberInfoString[groupPosition][childPosition];
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			TextView textView = getGenericView();
			String mebNum = getChild(groupPosition, childPosition).toString();
			if (mebNum.length() > 0) {
				int mebStatus = groupStatusMap.get(mebNum);
				String mebName = groupNameMap.get(mebNum);
				textView.setText(mebNum + "--(" + mebName + ")");
				if (mebStatus != PTTConstant.MEMBER_OFFLINE) {
					textView.setTextColor(Color.BLUE);
					textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.on_line, 0, 0, 0);
				} else {
					textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.off_line, 0, 0, 0);
				}
			}
			return textView;
		}

		public int getChildrenCount(int groupPosition) {
			return memberInfoString[groupPosition].length;
		}

		public Object getGroup(int groupPosition) {
			return groupString[groupPosition];
		}

		public int getGroupCount() {
			return groupString.length;
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			TextView textView = getGenericView();
			String grou_num = getGroup(groupPosition).toString();
			String curr_num = sp.getString(PTTConstant.SP_CURR_GRP_NUM, "");
			if (curr_num.trim().length() != 0 && grou_num.contains(curr_num)) {
				textView.setText(grou_num + getApplicationContext().getString(R.string.setting_current_group_on));
				textView.setTextColor(Color.BLUE);
			} else {
				textView.setText(grou_num);
			}
			return textView;
		}

		public TextView getGenericView() {
			ListView.LayoutParams lp = new ListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 80);
			TextView textView = new TextView(GroupActivity.this);
			textView.setLayoutParams(lp);
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			textView.setPadding(60, 0, 0, 0);
			return textView;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		public boolean hasStableIds() {
			return true;
		}

	}

	public void freshView() {
           //by yjp 2013-4-9
           try{
		groupInfoMap = GroupManager.getInstance().getGroupNumMebersMap();
		lstGroupData = GroupManager.getInstance().getGroupData();

		if (lstGroupData.size() > 0 && groupInfoMap.size() > 0) {

			groups = new String[lstGroupData.size()];
			groupString = new String[lstGroupData.size()];
			memberInfos = new MemberInfo[groupInfoMap.size()][];
			memberInfoString = new String[groupInfoMap.size()][];

			for (int i = 0; i < lstGroupData.size(); i++) {
				groupString[i] = lstGroupData.get(i).getNumber() + "--(" + lstGroupData.get(i).getName() + ")";
				groups[i] = lstGroupData.get(i).getNumber();
				memberInfos[i] = groupInfoMap.get(groups[i]);
				Arrays.sort(memberInfos[i]);
			}

			for (int i = 0; i < groupInfoMap.size(); i++) {
				memberInfoString[i] = new String[memberInfos[i].length];
				for (int j = 0; j < memberInfos[i].length; j++) {
					String mebNum = memberInfos[i][j].getNumber();
					String mebName = memberInfos[i][j].getName();
					int mebStatus = memberInfos[i][j].getStatus();
					groupStatusMap.put(mebNum, mebStatus);
					groupNameMap.put(mebNum, mebName);
					memberInfoString[i][j] = mebNum;
				}
			}
		} else {
			groupString = new String[] { "" };
			memberInfoString = new String[][] { { "" } };
		}

		mAdapter = new MyExpandableListAdapter();
		expandableListView.setAdapter(mAdapter);
		if (lstGroupData.size() == 1) {
			expandableListView.expandGroup(0);
		}
//		GroupManager.getInstance().clearNumMemberMap();
//		Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.group_fresh),
//				Toast.LENGTH_SHORT).show();
          }
          catch(Exception ex)
            {
              ex.printStackTrace();
            }
	}

	class GroupReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			freshView();
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		timer.cancel();
		this.unregisterReceiver(groupReceiver);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
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
		//intent.setClass(GroupActivity.this, PTTService.class);
		intent.putExtra(PTTConstant.KEY_SERVICE_COMMAND,
				PTTConstant.COMM_MAKE_CALL);
		intent.putExtra("number_info", numberInfo);
		//startService(intent);
		PTTService.instance.sendPttCommand(intent);
	}

}
