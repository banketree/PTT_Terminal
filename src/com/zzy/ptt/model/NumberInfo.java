/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:NumberInfo.java
 * Description:TODO
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-7
 */

package com.zzy.ptt.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Administrator
 * 
 */
public class NumberInfo implements Parcelable {

	private int numberType;
	private int pttType;
	private String number;
	private String sipUri;

	public int getNumberType() {
		return numberType;
	}

	public void setNumberType(int numberType) {
		this.numberType = numberType;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getSipUri() {
		return sipUri;
	}

	public void setSipUri(String sipUri) {
		this.sipUri = sipUri;
	}

	public int getPttType() {
		return pttType;
	}

	public void setPttType(int pttType) {
		this.pttType = pttType;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(number);
		dest.writeInt(pttType);
		dest.writeInt(numberType);
	}

	public static final Parcelable.Creator<NumberInfo> CREATOR = new Parcelable.Creator<NumberInfo>() {

		@Override
		public NumberInfo createFromParcel(Parcel source) {
			NumberInfo numberInfo = new NumberInfo();
			numberInfo.number = source.readString();
			numberInfo.pttType = source.readInt();
			numberInfo.numberType = source.readInt();
			return numberInfo;
		}

		@Override
		public NumberInfo[] newArray(int size) {
			// TODO Auto-generated method stub
			return null;
		}
	};

	@Override
	public String toString() {
		return "NumberInfo [numberType=" + numberType + ", pttType=" + pttType + ", number="
				+ number + ", sipUri=" + sipUri + "]";
	}

}
