/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : AddContactActivity
 * Author : wangjunhui
 * Version : 1.0
 * Date : 2012-05-21
 */
package com.zzy.ptt.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;

import android.os.Bundle;
import android.util.Xml;
import android.view.Window;
import android.widget.TextView;

import com.zzy.ptt.R;
import com.zzy.ptt.model.VersionInfo;

/**
 * @author wangjunhui
 * 
 */
public class AboutActivity extends BaseActivity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.aboutptt);
		List<VersionInfo> list = parse();
		VersionInfo currentVersion = list.get(0);
		String version = currentVersion.getVersion();
		VersionInfo currentVersion1 = list.get(1);
		String build_time = currentVersion1.getBuild_time();
		TextView versiontv = (TextView) findViewById(R.id.textViewversion);
		TextView buildtimetv = (TextView) findViewById(R.id.textViewbuildtime);
		versiontv.setText(versiontv.getText() + version);
		buildtimetv.setText(getApplicationContext().getString(R.string.aboutv5) + build_time);
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
}
