/**
* Copyright 2012 zzy Tech. Co., Ltd.
* All right reserved.
* Project:zzy PTT V1.0
* Name:JniAdapter.java
* Description:TODO
* Author:LiXiaodong
* Version:1.0
* Date:2012-3-2
*/

package com.zzy.ptt.jni;



/**
 * @author Administrator
 *
 */
public abstract class JniManager {
	
	public static void initJni() {
		System.loadLibrary("pjsip-jni");
	}
}
