/**
* Copyright 2012 zzy Tech. Co., Ltd.
* All right reserved.
* Project : zzy PTT V1.0
* Name : ContactUser
* Author : wangjunhui
* Version : 1.0
* Date : 2012-04-26
*/
package com.zzy.ptt.model;
/**
 * @author wangjunhui
 * 
 */
public class ContactUser {

	private int _id;
	private int imageId;
	private String name;
	private String cellphone;
	public int get_id() {
		return _id;
	}
	public void set_id(int _id) {
		this._id = _id;
	}
	public int getImageId() {
		return imageId;
	}
	public void setImageId(int imageId) {
		this.imageId = imageId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCellphone() {
		return cellphone;
	}
	public void setCellphone(String cellphone) {
		this.cellphone = cellphone;
	}
	@Override
	public String toString() {
		return "ContactUser [_id=" + _id + ", imageId=" + imageId + ", name="
				+ name + ", cellphone=" + cellphone + "]";
	}
	
	
}
