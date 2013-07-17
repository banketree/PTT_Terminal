/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : CallLog.java
 * Author : LiXiaodong
 * Version : 1.0
 * Date : 2012-3-31
 */
package com.zzy.ptt.model;

import java.sql.Timestamp;

import com.zzy.ptt.util.PTTConstant;

/**
 * @author lxd
 * 
 */
public class CallLog {

	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	private int callId;
	private String num;
	private String reason;
	private int logType = PTTConstant.LOG_ALL;
	private long duration;
	private Timestamp time;

	public int getCallId() {
		return callId;
	}

	public void setCallId(int callId) {
		this.callId = callId;
	}

	public String getNum() {
		return num;
	}

	public void setNum(String num) {
		this.num = num;
	}

	public int getLogType() {
		return logType;
	}

	public void setLogType(int logType) {
		this.logType = logType;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duraiton) {
		this.duration = duraiton;
	}

	public Timestamp getTime() {
		return time;
	}

	public void setTime(Timestamp time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return "CallLog [id=" + id + ", callId=" + callId + ", num=" + num + ", logType=" + logType
				+ ", duration=" + duration + ", reason=" + reason + ", time=" + time.toLocaleString() + "]";

	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

}
