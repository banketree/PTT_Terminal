/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:GroupMemberInfo.java
 * Description：TODO
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-16
 */

package com.zzy.ptt.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Administrator
 * 
 */
public class MemberInfo implements Parcelable,Comparable<MemberInfo> {

	//offline, listening, speaking, online
	private int status;

	private String name;

	private String number;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(status);
		dest.writeString(name);
		dest.writeString(number);
	}

	public static final Parcelable.Creator<MemberInfo> CREATOR = new Parcelable.Creator<MemberInfo>() {

		@Override
		public MemberInfo createFromParcel(Parcel source) {
			MemberInfo memberInfo = new MemberInfo();
			memberInfo.setStatus(source.readInt());
			memberInfo.setName(source.readString());
			memberInfo.setNumber(source.readString());
			return memberInfo;
		}

		@Override
		public MemberInfo[] newArray(int size) {
			// TODO Auto-generated method stub
			return null;
		}
	};

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

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

	@Override
	public String toString() {
		return "MemberInfo [status=" + status + ", name=" + name + ", number=" + number + "]";
	}

	@Override
	public int compareTo(MemberInfo another) {
		// TODO Auto-generated method stub
		
		return another.status - this.status;
	}
}
