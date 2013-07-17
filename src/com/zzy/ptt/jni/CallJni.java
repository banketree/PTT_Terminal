/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:CallJni.java
 * Description:JNI, declares the native methods
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-2
 */

package com.zzy.ptt.jni;

import com.zzy.ptt.model.NumberInfo;
import com.zzy.ptt.model.SipServer;
import com.zzy.ptt.model.SipUser;

/**
 * @author Administrator
 * 
 */
public class CallJni {

	/**
	 * 
	 * @return int : fail or success
	 */
	public static native int init();

	public static native int login(SipServer server, SipUser user);

	public static native int makeCall(NumberInfo numberInfo);

	public static native int answer(int callId);

	public static native int hangup(int callId);
	//methord add by wangjunhui
	public static native int sendMessage(String number, String text);
	
	public static native int getMember(String groupNumber);

	public static native int destroy();
	
	public static native int openPtt();
	
	public static native int applyPttRight(String groupNumber);
	
	public static native int cancelPttRight(String groupNumber);
	
	public static native int exitPttUIManually(String groupNumber);
	
	//methord add by wangjunhui
	public static native int hoInd(String ip);
	
	public static native int getGroupNo(String anyGroupNo);
	
	public static native int sendDTMF(String digit);

}
