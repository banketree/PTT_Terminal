/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:BaseActivity.java
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-28
 */

package com.zzy.ptt.ui;

import android.app.Activity;
import android.os.Bundle;

/**
 * @author Administrator
 * 
 */
public class BaseActivity extends Activity {

	// private static final String LOG_TAG = "BaseActivity";

/*	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (this instanceof DialActivity) {
			return super.onKeyDown(keyCode, event);
		}

		if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
			Intent intent = new Intent(this, DialActivity.class);
			intent.putExtra("OK", 123);
			intent.putExtra("keyCode", (keyCode - 7) + "");
			startActivity(intent);
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}*/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

//	@Override
//	protected void onResume() {
//		super.onResume();
//		PTTService.topActivity = this;
//	}
	
	

}
