/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : PttGroupStatus.java
 * Author : LiXiaodong
 * Version : 1.0
 * Date : 2012-4-25
 */
package com.zzy.ptt.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.zzy.ptt.util.PTTConstant;
import com.zzy.ptt.util.PTTUtil;

/**
 * @author lxd
 * 
 */
public class PttGroupStatus implements Parcelable {

	private String groupNum;

	private int callId = -1;

	private int status = PTTConstant.PTT_IDLE;

	private String speakerNum;

	private String startTime;

	private String speakerStartTime;

	private int seqCallId;
	
	private int rejectReason;
	
	private int totalWaitingCount;
	
	private int currentWaitingPos;

	private static final String LOG_TAG = "PttGroupStatus";
	private static final boolean bDebug = true;

	public String getGroupNum() {
		return groupNum;
	}

	public void setGroupNum(String groupNum) {
		this.groupNum = groupNum;
	}

	public int getCallId() {
		return callId;
	}

	public void setCallId(int callId) {
		this.callId = callId;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		PTTUtil pttUtil = PTTUtil.getInstance();
		pttUtil.printLog(bDebug, LOG_TAG, "------ptt status from " + pttUtil.getPttStatusString(this.status) + " to "
				+ pttUtil.getPttStatusString(status));
		this.status = status;
	}

	public String getSpeakerNum() {
		return speakerNum;
	}

	public void setSpeakerNum(String speakerNum) {
		this.speakerNum = speakerNum;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(groupNum);
		dest.writeInt(callId);
		dest.writeInt(status);
		dest.writeString(speakerNum);
		dest.writeString(startTime);
		dest.writeString(speakerStartTime);
	}

	public static final Parcelable.Creator<PttGroupStatus> CREATOR = new Parcelable.Creator<PttGroupStatus>() {

		@Override
		public PttGroupStatus createFromParcel(Parcel source) {
			PttGroupStatus pttGroupStatus = new PttGroupStatus();
			pttGroupStatus.groupNum = source.readString();
			pttGroupStatus.callId = source.readInt();
			pttGroupStatus.status = source.readInt();
			pttGroupStatus.speakerNum = source.readString();
			pttGroupStatus.startTime = source.readString();
			pttGroupStatus.speakerStartTime = source.readString();
			return pttGroupStatus;
		}

		@Override
		public PttGroupStatus[] newArray(int size) {
			// TODO Auto-generated method stub
			return null;
		}
	};

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getSpeakerStartTime() {
		return speakerStartTime;
	}

	public void setSpeakerStartTime(String speakerStartTime) {
		this.speakerStartTime = speakerStartTime;
	}

	public int getSeqCallId() {
		return seqCallId;
	}

	public void setSeqCallId(int seqCallId) {
		this.seqCallId = seqCallId;
	}

	@Override
	public String toString() {
		return "PttGroupStatus [groupNum=" + groupNum + ", callId=" + callId + ", status=" + status + ", speakerNum="
				+ speakerNum + ", startTime=" + startTime + ", speakerStartTime=" + speakerStartTime + ", seqCallId="
				+ seqCallId + ", rejectReason=" + rejectReason + ", totalWaitingCount=" + totalWaitingCount
				+ ", currentWaitingPos=" + currentWaitingPos + "]";
	}

	public int getRejectReason() {
		return rejectReason;
	}

	public void setRejectReason(int rejectReason) {
		this.rejectReason = rejectReason;
	}

	public int getTotalWaitingCount() {
		return totalWaitingCount;
	}

	public void setTotalWaitingCount(int totalWaitingCount) {
		this.totalWaitingCount = totalWaitingCount;
	}

	public int getCurrentWaitingPos() {
		return currentWaitingPos;
	}

	public void setCurrentWaitingPos(int currentWaitingPos) {
		this.currentWaitingPos = currentWaitingPos;
	}

}
