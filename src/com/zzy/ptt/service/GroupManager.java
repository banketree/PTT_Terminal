/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : GroupManager.java
 * Author : LiXiaodong
 * Version : 1.0
 * Date : 2012-4-12
 */
package com.zzy.ptt.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.zzy.ptt.model.GroupInfo;
import com.zzy.ptt.model.MemberInfo;

/**
 * @author lxd
 * 
 */
public class GroupManager {

	private static final String LOG_TAG = "GroupManager";

	private List<GroupInfo> lstGroupData = new ArrayList<GroupInfo>();

	private Map<String, MemberInfo[]> groupNumMebersMap;
	
	private boolean bDynamic = false;

	private GroupManager() {
		groupNumMebersMap = new HashMap<String, MemberInfo[]>();
	}

	private static GroupManager instance = new GroupManager();

	public static GroupManager getInstance() {
		if (instance == null) {
			instance = new GroupManager();
		}
		return instance;
	}

	public void test() {
		// testCount = 10;
		// lstTestData.add("11");
		// lstTestData.add("22");
	}

	public void addGroups(GroupInfo[] groupInfos){
		lstGroupData.clear();
		for (int i = 0; i < groupInfos.length; i++) {
			String name = groupInfos[i].getName();
			String number = groupInfos[i].getNumber();
			Log.d(LOG_TAG, "add group " + name + ", num : " + number);
			lstGroupData.add(groupInfos[i]);
		}
	}

	public List<GroupInfo> getGroupData() {
		return lstGroupData;
	}

	public GroupInfo getGroupInfo(String grpNum) {
		int size = lstGroupData.size();
		GroupInfo info = new GroupInfo();
		info.setNumber(grpNum);

		if (size == 0) {
			info.setName(grpNum);
			return info;
		}

		for (int i = 0; i < size; i++) {
			info = lstGroupData.get(i);
			if (grpNum.trim().equals(info.getNumber().trim())) {
				return info;
			}
		}
		return info;
	}

	public void addReceivedMembers(String grpNumber, MemberInfo[] members) {
		if (groupNumMebersMap.containsKey(grpNumber)) {
			// do nothing
		} else {
			MemberInfo[] tempMembers = new MemberInfo[members.length];
			System.arraycopy(members, 0, tempMembers, 0, members.length);
			groupNumMebersMap.put(grpNumber, tempMembers);
		}
	}

	public Map<String, MemberInfo[]> getGroupNumMebersMap() {
		return groupNumMebersMap;
	}

	public boolean checkReceiveAll() {
		List<GroupInfo> groupData = GroupManager.getInstance().getGroupData();

		if (groupData == null) {
			return true;
		}
		return groupNumMebersMap.size() == groupData.size();
	}
	
	public void clear(){
		groupNumMebersMap.clear();
		lstGroupData.clear();
		bDynamic = false;
	}
	public void clearNumMemberMap(){
		groupNumMebersMap.clear();
	}

	public boolean isbDynamic() {
		return bDynamic;
	}

	public void setbDynamic(boolean bDynamic) {
		this.bDynamic = bDynamic;
	}
	
	
}
