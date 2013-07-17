/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:AlertDialogManager.java
 * Description:TODO
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-12
 */

package com.zzy.ptt.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.widget.Toast;

import com.zzy.ptt.R;
import com.zzy.ptt.util.PTTConstant;

/**
 * @author Administrator
 * 
 */
public class AlertDialogManager {

	private Context context;

	private static AlertDialogManager instance = new AlertDialogManager();

	private AlertDialogManager() {
	};

	public static AlertDialogManager getInstance() {
		return instance;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public AlertDialog alertRegisterFail(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.alert_title_register));
		builder.setMessage(context.getString(R.string.alert_msg_register_fail));
		builder.setNeutralButton(R.string.alert_btn_ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
		return dialog;
	}

	public AlertDialog alertRegisterFail(Context context, String customMsg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.alert_title_register));
		builder.setMessage(context.getString(R.string.alert_msg_register_fail) + " : " + customMsg);
		builder.setNeutralButton(R.string.alert_btn_ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
		return dialog;
	}

	public void alertInitFail(Context context, String customMsg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.alert_title_init));
		builder.setMessage(context.getString(R.string.alert_msg_init_fail) + " : " + customMsg);
		builder.setNeutralButton(R.string.alert_btn_ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}

	public void alertOutingFail(Context context, String customMsg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.alert_title_outingcall));
		builder.setMessage(context.getString(R.string.alert_msg_outingcall_fail) + " : "
				+ customMsg);
		builder.setNeutralButton(R.string.alert_btn_ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}

	public void alertServerNull() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.alert_title_server_null));
		builder.setMessage(context.getString(R.string.alert_msg_server_wrong));
		builder.setPositiveButton(context.getString(R.string.alert_btn_yes), new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(context, SettingDetailActivity.class);
				intent.putExtra(PTTConstant.SETTING_DISPATCH_KEY,
						PTTConstant.SETTING_ITEM_REGISTGER);
				context.startActivity(intent);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(context.getString(R.string.alert_btn_no), new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}

	public void alertUserNull() {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(context.getString(R.string.alert_title_user_null));
		builder.setMessage(context.getString(R.string.alert_msg_user_wrong));
		builder.setPositiveButton(context.getString(R.string.alert_btn_yes), new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(context, SettingDetailActivity.class);
				intent.putExtra(PTTConstant.SETTING_DISPATCH_KEY,
						PTTConstant.SETTING_ITEM_REGISTGER);
				context.startActivity(intent);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(context.getString(R.string.alert_btn_no), new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}

	public void toastServerWrong() {
		Toast.makeText(context, R.string.alert_msg_server_wrong, Toast.LENGTH_LONG).show();
	}

	public void toastUserWrong() {
		Toast.makeText(context, R.string.alert_msg_user_wrong, Toast.LENGTH_LONG).show();
	}

	public void toastPasswordWrong() {
		Toast.makeText(context, R.string.alert_msg_password_null, Toast.LENGTH_LONG).show();
	}

	public void toastNumberNull() {
		Toast.makeText(context, R.string.alert_number_null, Toast.LENGTH_LONG).show();
	}

	public void toastNumberWrong() {
		Toast.makeText(context, R.string.alert_number_onlynum, Toast.LENGTH_LONG).show();
	}

	public void toastNoPtt() {
		Toast.makeText(context, R.string.alert_msg_no_ptt, Toast.LENGTH_LONG).show();
	}

	public ProgressDialog showProgressDialog(Context context, String title, String msg) {
		ProgressDialog dialog = ProgressDialog.show(context, title, msg);
		dialog.setCancelable(true);
		return dialog;
	}

	public void dismissProgressDialog(ProgressDialog progressDialog) {
		if (progressDialog != null)
			progressDialog.dismiss();
	}

}
