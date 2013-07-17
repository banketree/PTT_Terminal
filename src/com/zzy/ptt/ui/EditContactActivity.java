/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : EditContactActivity
 * Author : wangjunhui
 * Version : 1.0
 * Date : 2012-04-26
 */
package com.zzy.ptt.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.os.Bundle;
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
public class EditContactActivity extends BaseActivity {

	private EditText et_name, et_phone;
	private Button saveOrchang, delete;
	int imageId;
	private HashMap<String, String> map = null;
	private ContactUser ub = new ContactUser();
	private ContactUserCl ubc;
	private ArrayList<String> allNums = new ArrayList<String>();
	String oName,oNum;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.detailinfo);
		this.setTitle(getApplicationContext().getString(R.string.contact_detail) + "");
		ubc = new ContactUserCl(this);
		allNums = ubc.getUserNums();
		
		et_name = (EditText) findViewById(R.id.et_name);
		et_phone = (EditText) findViewById(R.id.et_phone);
		saveOrchang = (Button) findViewById(R.id.saveOrchang);
		delete = (Button) findViewById(R.id.delete);
		
		Intent it = getIntent();
		map = (HashMap<String, String>) it.getSerializableExtra("userInfo");
		this.imageId = Integer.parseInt(map.get("imageId").toString());
		this.ub.set_id(Integer.parseInt(map.get("_id").toString()));
		oName = map.get("userName").toString();
		oNum = map.get("cellphone").toString();
		et_name.setText(oName);
		et_phone.setText(oNum);
		
		
		initWidget();
		
	}

	public void initWidget() {

		saveOrchang.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				saveOrchang.setText(getApplicationContext().getString(R.string.contact_edit1) + "");
				String userName = et_name.getText().toString();
				String cellphone = et_phone.getText().toString();
				
				if (allNums.contains(cellphone) && !cellphone.equals(oNum)) {
					Toast.makeText(EditContactActivity.this,
							getApplicationContext().getString(R.string.contact_already_in) + "", Toast.LENGTH_SHORT)
							.show();
					return;
				}
				
				SaveChangeContact(userName, cellphone);
			}

		});

		delete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				ContactUserCl ubc = new ContactUserCl(EditContactActivity.this);
				if (ubc.delete(ub.get_id())) {
					Toast.makeText(EditContactActivity.this,
							getApplicationContext().getString(R.string.contact_del_success1) + "", Toast.LENGTH_SHORT)
							.show();
					setResult(1);
					EditContactActivity.this.finish();
				} else {
					Toast.makeText(EditContactActivity.this,
							getApplicationContext().getString(R.string.contact_del_faulte1) + "", Toast.LENGTH_SHORT)
							.show();

				}
			}
		});

	}

	private void SaveChangeContact(String userName, String cellphone) {

		ub.setImageId(EditContactActivity.this.imageId);
		ub.setName(userName);
		ub.setCellphone(cellphone);

		int access = ubc.updateUser(ub);
		if (access == 1) {
			Toast.makeText(EditContactActivity.this,
					getApplicationContext().getString(R.string.contact_edit_success) + "", Toast.LENGTH_SHORT).show();
			setResult(1);
			EditContactActivity.this.finish();
		} else {
			Toast.makeText(EditContactActivity.this,
					getApplicationContext().getString(R.string.contact_edit_fault) + "", Toast.LENGTH_SHORT).show();

		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 0) {
			if (resultCode == 1) {
				this.imageId = data.getIntExtra("imageId", R.drawable.ic_launcher);
			} else if (resultCode == 2) {
			}
		}
	}


}
