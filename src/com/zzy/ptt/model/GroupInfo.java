/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:GroupInfo.java
 * Description：TODO
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-16
 */

package com.zzy.ptt.model;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Administrator
 * 
 */
public class GroupInfo implements Parcelable {

	private String name;

	private String number;

	private char level;

	private boolean emergency;

	private List<MemberInfo> listMembers = new ArrayList<MemberInfo>();

	public GroupInfo() {

	}

	public GroupInfo(String name, String num) {
		this.name = name;
		this.number = num;
	}

	public List<MemberInfo> getListMembers() {
		return listMembers;
	}

	public void setListMembers(List<MemberInfo> listMembers) {
		this.listMembers = listMembers;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(number);
		dest.writeInt(level);
		dest.writeList(listMembers);
	}

	public static final Parcelable.Creator<GroupInfo> CREATOR = new Parcelable.Creator<GroupInfo>() {

		@SuppressWarnings("unchecked")
		@Override
		public GroupInfo createFromParcel(Parcel source) {
			GroupInfo grpInfo = new GroupInfo();
			grpInfo.setName(source.readString());
			grpInfo.setNumber(source.readString());
			grpInfo.setLevel((char) source.readInt());
			grpInfo.setListMembers(source.readArrayList(ArrayList.class.getClassLoader()));
			return grpInfo;
		}

		@Override
		public GroupInfo[] newArray(int size) {
			// TODO Auto-generated method stub
			return null;
		}
	};

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public char getLevel() {
		return level;
	}

	public void setLevel(char level) {
		this.level = level;
	}

	public boolean isEmergency() {
		return emergency;
	}

	public void setEmergency(boolean emergency) {
		this.emergency = emergency;
	}

	@Override
	public String toString() {
		return "GroupInfo [name=" + name + ", number=" + number + "]";
	}
}
