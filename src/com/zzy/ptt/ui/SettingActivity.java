/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:SettingActivity.java
 * Description:SettingPage, store information to file
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-5
 */

package com.zzy.ptt.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.model.VersionInfo;
import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author Administrator
 * 
 */
public class SettingActivity extends BaseActivity implements OnItemClickListener {
	private LayoutInflater inflater;
	
	public static SettingActivity instance = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		instance = this;
		inflater = LayoutInflater.from(this);
		PTTUtil.getInstance().initOnCreat(this);

		showSettingList();
	}

	private void showSettingList() {
		ListView listView = new ListView(this);
		listView.setAdapter(new MyAdapter());
		setContentView(listView);
		listView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, final long id) {
		View v = inflater.inflate(R.layout.register_pw, null);
		final EditText pwEditText = (EditText) v.findViewById(R.id.registerpw);
		List<VersionInfo> list = parse();
		VersionInfo currentVersion = list.get(2);
		final String register_pw = currentVersion.getRegister_pw();
		if (id == PTTConstant.SETTING_ITEM_REGISTGER) {
			new AlertDialog.Builder(this)
					.setTitle(getApplicationContext().getString(R.string.setting_register_pw))
					.setIcon(android.R.drawable.ic_dialog_info)
					.setView(v)
					.setPositiveButton(getApplicationContext().getString(R.string.alert_btn_ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									if (pwEditText.getText().toString().trim().equals(register_pw)) {
										dispatchPage(id);
									} else {
										Toast.makeText(getApplicationContext(),
												getApplicationContext().getString(R.string.setting_register_pw_false),
												Toast.LENGTH_LONG).show();
									}
								}
							}).setNegativeButton(getApplicationContext().getString(R.string.alert_btn_cancel), null)
					.show();
		} else {
			dispatchPage(id);
		}
	}

	private void dispatchPage(long id) {
		Intent intent = new Intent(this, SettingDetailActivity.class);
		intent.putExtra(PTTConstant.SETTING_DISPATCH_KEY, (int) id);
		startActivity(intent);
	}

	// add by wangjunhui
	class MyAdapter extends BaseAdapter {
		String[] data = { getString(R.string.setting_talking), getString(R.string.setting_register),
				getString(R.string.setting_alertring), getString(R.string.setting_system) };

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return data.length;
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
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			TextView tv = new TextView(getApplicationContext());
			tv.setText(data[position]);
			tv.setHeight(120);
			tv.setTextSize(40);
			tv.setTextColor(Color.WHITE);
			return tv;
		}

	}
	
	

	List<VersionInfo> parse() {
		InputStream in = null;
		try {
			in = getAssets().open("versioninfo.xml");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		XmlPullParser parser = Xml.newPullParser();

		List<VersionInfo> versionList = new ArrayList<VersionInfo>();

		VersionInfo currentVersion = null;
		try {
			parser.setInput(in, "utf-8");

			int eventType = parser.getEventType();

			while (eventType != XmlPullParser.END_DOCUMENT) {

				switch (eventType) {

				case XmlPullParser.START_TAG:
					String tagName = parser.getName();
					if (tagName != null && tagName.equals("version")) {
						currentVersion = new VersionInfo();
						int id = Integer.parseInt(parser.getAttributeValue(null, "id"));
						currentVersion.setId(id);
					}

					if (tagName != null && tagName.equals("name")) {
						String name = parser.nextText();
						currentVersion.setVersion(name);
					}
					if (tagName != null && tagName.equals("build_time")) {
						String build_time = parser.nextText();
						currentVersion.setBuild_time(build_time);
					}
					if (tagName != null && tagName.equals("register_pw")) {
						String register_pw = parser.nextText();
						currentVersion.setRegister_pw(register_pw);
					}

					break;

				case XmlPullParser.END_TAG:
					if (parser.getName().equals("version")) {
						versionList.add(currentVersion);
					}
					break;
				default:
					break;
				}
				eventType = parser.next();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return versionList;

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		instance = null;
	}
}
