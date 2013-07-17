/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:CallState.java
 * Description:CallState Model
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-7
 */

package com.zzy.ptt.model;

import com.zzy.ptt.util.PTTUtil;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author Administrator
 * 
 */
public class CallState implements Parcelable {
	private int callId;
	private String number;
	private long seconds;
	private int callState;
	private int errorCode;// sip_status_code, check when eCallState is error

	public int getCallId() {
		return callId;
	}

	public void setCallId(int callId) {
		this.callId = callId;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public long getSeconds() {
		return seconds;
	}

	public void setSeconds(long seconds) {
		this.seconds = seconds;
	}

	public int getCallState() {
		return callState;
	}

	public void setCallState(int eCallState) {
		this.callState = eCallState;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(callId);
		dest.writeString(number);
		dest.writeLong(seconds);
		dest.writeInt(callState);
		dest.writeInt(errorCode);
	};

	public static final Parcelable.Creator<CallState> CREATOR = new Parcelable.Creator<CallState>() {

		@Override
		public CallState createFromParcel(Parcel source) {
			CallState callState = new CallState();
			callState.callId = source.readInt();
			callState.number = source.readString();
			callState.seconds = source.readLong();
			callState.callState = source.readInt();
			callState.errorCode = source.readInt();
			return callState;
		}

		@Override
		public CallState[] newArray(int size) {
			// TODO Auto-generated method stub
			return null;
		}
	};

	@Override
	public String toString() {
		return "CallState [callId=" + callId + ", number=" + number + ", seconds=" + seconds
				+ ", callState=" + PTTUtil.getInstance().getCallStateString(callState)
				+ ", errorCode=" + errorCode + "]";
	}

}
