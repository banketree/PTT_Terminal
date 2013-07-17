/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project : zzy PTT V1.0
 * Name : Message
 * Author : wangjunhui
 * Version : 1.0
 * Date : 2012-04-26
 */
package com.zzy.ptt.model;

import java.io.Serializable;

/**
 * @author wangjunhui
 * 
 */
public class Message implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int thread_id;
	private String address;
	private String date;
	private String body;
	private int type;
	private int readstatus;
	private int sendstatus;

	public int getReadstatus() {
		return readstatus;
	}

	public void setReadstatus(int readstatus) {
		this.readstatus = readstatus;
	}

	public int getSendstatus() {
		return sendstatus;
	}

	public Message(int thread_id, String address, String date, String body,
			int type, int readstatus, int sendstatus) {
		super();
		this.thread_id = thread_id;
		this.address = address;
		this.date = date;
		this.body = body;
		this.type = type;
		this.readstatus = readstatus;
		this.sendstatus = sendstatus;
	}

	public void setSendstatus(int sendstatus) {
		this.sendstatus = sendstatus;
	}

	public Message() {
		super();
	}

	public Message(int thread_id, String address, String date) {
		super();
		this.thread_id = thread_id;
		this.address = address;
		this.date = date;
	}

	public Message(String address, String date, String body) {
		super();
		this.address = address;
		this.date = date;
		this.body = body;
	}

	public Message(int type, int thread_id) {
		super();
		this.type = type;
		this.thread_id = thread_id;
	}

	public Message(String date, String address) {
		super();
		this.date = date;
		this.address = address;
	}

	public Message(String address) {
		super();
		this.address = address;
	}

	public Message(int thread_id) {
		super();
		this.thread_id = thread_id;
	}

	public Message(String address, String date, String body, int type) {
		super();
		this.address = address;
		this.date = date;
		this.body = body;
		this.type = type;
	}

	public Message(String date, String body, int type) {
		super();
		this.date = date;
		this.body = body;
		this.type = type;
	}

	public Message(int thread_id, String address, String body, int type) {
		super();
		this.thread_id = thread_id;
		this.address = address;
		this.body = body;
		this.type = type;
	}

	public Message(int thread_id, String address, String body, int type,
			int readstatus) {
		super();
		this.thread_id = thread_id;
		this.address = address;
		this.body = body;
		this.type = type;
		this.readstatus = readstatus;
	}

	public Message(int thread_id, String address, String body, int type,
			int readstatus, int sendstatus) {
		super();
		this.thread_id = thread_id;
		this.address = address;
		this.body = body;
		this.type = type;
		this.readstatus = readstatus;
		this.sendstatus = sendstatus;
	}

	public Message(int thread_id, String address, String date, String body) {
		super();
		this.thread_id = thread_id;
		this.address = address;
		this.date = date;
		this.body = body;
	}

	public Message(int thread_id, String address, String date, String body,
			int type) {
		super();
		this.thread_id = thread_id;
		this.address = address;
		this.date = date;
		this.body = body;
		this.type = type;
	}

	public Message(int thread_id, String address, String date, String body,
			int type, int readstatus) {
		super();
		this.thread_id = thread_id;
		this.address = address;
		this.date = date;
		this.body = body;
		this.type = type;
		this.readstatus = readstatus;
	}

	public int getThreadId() {
		return thread_id;
	}

	public void setThreadId(int thread_id) {
		this.thread_id = thread_id;
	}

	public String getNumber() {
		return address;
	}

	public void setNumber(String address) {
		this.address = address;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "Message [thread_id=" + thread_id + ", address=" + address
				+ ", date=" + date + ", body=" + body + ", type=" + type
				+ ", readstatus=" + readstatus + ", sendstatus=" + sendstatus
				+ "]";
	}

}
