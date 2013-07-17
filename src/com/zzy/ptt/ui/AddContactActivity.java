/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : AddContactActivity
 * Author : wangjunhui
 * Version : 1.0
 * Date : 2012-04-26
 */
package com.zzy.ptt.ui;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.zzy.ptt.db.ContactUserCl;
import com.zzy.ptt.model.ContactUser;

/**
 * @author wangjunhui
 * 
 */
public class AddContactActivity extends BaseActivity {
	private Button save;
	private EditText et_name, et_phone;
	private int imageId;
	private ContactUserCl ubc;
	private ArrayList<String> al = new ArrayList<String>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addnewcontact);
		this.setTitle(getApplicationContext().getString(R.string.add_contact) + "");
		// create database
		ubc = new ContactUserCl(this);
		al = ubc.getUserNums();
		et_name = (EditText) findViewById(R.id.et_name);
		et_phone = (EditText) findViewById(R.id.et_phone);

		save = (Button) findViewById(R.id.save);
		
		et_phone.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub
				if (s.toString().length() >= 20) {
					Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.dail_num_length), Toast.LENGTH_LONG).show();
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

		Intent intent = getIntent();
		if (intent != null) {
			String cellphone = intent.getStringExtra("address");
			et_phone.setText(cellphone);
		}

		save.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// add contact
				String userNum = et_phone.getText().toString();
				if ((et_name.getText().toString()).equals("") || userNum.equals("")) {
					Toast.makeText(AddContactActivity.this,
							getApplicationContext().getString(R.string.empty_contact) + "", Toast.LENGTH_SHORT).show();
					return;
				} else if (al.contains(userNum)) {
					Toast.makeText(AddContactActivity.this,
							getApplicationContext().getString(R.string.contact_already_in) + "", Toast.LENGTH_SHORT)
							.show();
					return;
				}
				ContactUser ub = new ContactUser();
				ub.setImageId(imageId);
				ub.setName(et_name.getText().toString());
				ub.setCellphone(et_phone.getText().toString());

				int totalNum = ubc.getTotalUserNum("");
				if (totalNum >= 200) {
					alertErrorMsg(getApplicationContext().getString(R.string.contact_save1));
					return;
				}
				
				// add to database
				long access = 0;
				try {
					access = ubc.addNew(ub);
				} catch (SQLiteFullException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					alertErrorMsg(getApplicationContext().getString(R.string.contact_save2));
					return;
				}
				if (access != -1) {
					// add success
					Toast.makeText(AddContactActivity.this,
							getApplicationContext().getString(R.string.add_success) + "", Toast.LENGTH_SHORT).show();
					AddContactActivity.this.finish();
				} else {
					Toast.makeText(AddContactActivity.this, getApplicationContext().getString(R.string.add_fault) + "",
							Toast.LENGTH_SHORT).show();
					AddContactActivity.this.finish();
				}
			}
		});

	}
	
	private void alertErrorMsg(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.alert_title));
		builder.setMessage(msg);
		builder.setNeutralButton(R.string.alert_btn_ok, new AlertDialog.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

}