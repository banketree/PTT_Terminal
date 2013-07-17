/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:StringConstant.java
 * Description:Constant Class
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-5
 */

package com.zzy.ptt.util;

/**
 * @author Administrator
 * 
 */
public class PTTConstant {

	/* db */
	public static final String DB_NAME = "PTT_DB";
	public static final String DB_TABLE_CALLLOG = "PTT_CALLLOG";
	public static final String DB_TABLE_SMS_IN = "PTT_SMS_IN";
	public static final String DB_TABLE_SMS_OUT = "PTT_SMS_OUT";
	public static final String DB_TABLE_SMS_DETAIL = "PTT_SMS_DETAIL";
	public static final int DB_VERSION = 1;
	// call log table
	public static final String TABLE_CALLLOG_ID = "_id";
	public static final String TABLE_CALLLOG_CALLID = "call_id";
	public static final String TABLE_CALLLOG_LOGTYPE = "log_type";
	public static final String TABLE_CALLLOG_LOGTYPE_ICON = "log_type_icon";
	public static final String TABLE_CALLLOG_NUM = "num";
	public static final String TABLE_CALLLOG_DURATION = "duration";
	public static final String TABLE_CALLLOG_TIME = "time";
	public static final String TABLE_CALLLOG_REASON = "reason";
	public static final int CALLLOG_MAX = 200;

	/* shared preference */
	public static final String SHARED_PREFERENCE = "PTT_PREFERENCE";
	public static final String SP_SERVERIP = "PTT_SP_SERVERIP";
	public static final String SP_PORT = "PTT_SP_PORT";
	public static final String SP_USERNAME = "PTT_SP_USERNAME";
	public static final String SP_PASSWORD = "PTT_SP_PASSWORD";
	public static final String SP_AUTOREGISTER = "PTT_SP_AUTOREGISTER";
	public static final String SP_TEST_GROUP_NUM = "PTT_SP_GROUPNO";
	public static final String SP_CURR_GRP_NUM = "PTT_SP_CURR_GRP_NUM";

	/* setting key and item id */
	public static final String SETTING_DISPATCH_KEY = "SETTING_DISPATHC_KEY";
	public static final int SETTING_ITEM_TALKING = 0;
	public static final int SETTING_ITEM_REGISTGER = 1;
	public static final int SETTING_ITEM_ALERTRING = 2;
	public static final int SETTING_ITEM_SYSTEM = 3;
	public static final int SETTING_ITEM_GROUP = 4;

	/* intent key */
	public static final String KEY_REGISTER_STATE = "REGISTER_STATE";
	public static final String KEY_REREGISTER_FORCE = "REREGISTER_FORCE";
	public static final String KEY_INIT_STATE = "INIT_STATE";
	public static final String KEY_CALL_NUMBERINFO = "CALL_NUMBERINFO";
	public static final String KEY_CALL_STATE = "CALL_STATE";
	public static final String KEY_PTT_STATE = "PTT_STATE";
	public static final String KEY_GRP_INIT = "GRP_STATE_INIT";
	public static final String KEY_RETURN_VALUE = "RETURN_CODE";
	public static final String KEY_REGISTER_INFO = "REGISTER_INFO";
	public static final String KEY_USER_NULL = "USER_NULL";
	public static final String KEY_MESSAGE_RESULT = "MESSAGE_RESULT";
	public static final String KEY_MESSAGE_NUMBER = "MESSAGE_NUMBER";
	public static final String KEY_MESSAGE_TEXT = "MESSAGE_TEXT";
	public static final String KEY_GROUP_NUM = "GROUP_NUM";
	public static final String KEY_MEMBER_INFO = "MEMBER_INFO";
	public static final String KEY_SERVICE_COMMAND = "SERVICE_COMMAND";
	public static final String KEY_PTT_MBYE = "KEY_PTT_MBYE";
	public static final String KEY_NUMBER = "KEY_NUMBER";
	public static final String KEY_ERROR_CODE = "error_code";
	public static final String KEY_ERROR_MSG = "error_msg";
	public static final String KEY_ERROR_OR_STATUS = "error_or_status";
	public static final String KEY_DTMF = "dtmf_digit";
	public static final int VALUE_SERVER_NULL = 1;
	public static final int VALUE_USER_NULL = 1;
	public static final String KEY_STATUS_DEBUG_INFO = "status_debug_info";

	/* intent action */
	public static final String ACTION_REGISTER = "com.zzy.ptt.action.REGISTER";
	public static final String ACTION_INIT = "com.zzy.ptt.action.INIT";
	public static final String ACTION_DEINIT = "com.zzy.ptt.action.DEINIT";
	public static final String ACTION_GETGROUPDATA = "com.zzy.ptt.action.PULLGRP";
	public static final String ACTION_ALERT = "com.zzy.ptt.action.ALERT";
	public static final String ACTION_CALLSTATET = "com.zzy.ptt.action.CALLSTATE";
	public static final String ACTION_DIAL_UP = "android.intent.action.W_CALL.up";
	public static final String ACTION_DIAL_DOWN = "android.intent.action.W_CALL.down";
	public static final String ACTION_PTT = "android.intent.action.PTT";
	public static final String ACTION_PTT_STATUS_DEBUG = "android.intent.action.PTT.status.debug";
	public static final String ACTION_DYNAMIC_REGRP = "com.zzy.ptt.action.DYNAMIC_REGRP";
	public static final String ACTION_PTT_DOWN = "android.intent.action.PTT.down";
	public static final String ACTION_PTT_UP = "android.intent.action.PTT.up";
	public static final String ACTION_CM_CONNECTED = "android.intent.action.cm_connected";
	public static final String ACTION_CM_DISCONNECTED = "android.intent.action.cm_disconnected";
	public static final String ACTION_MESSAGE_RESULT = "android.intent.action.MESSAGE_RESULT";
	public static final String ACTION_NEW_MESSAGE = "android.intent.action.NEW_MESSAGE";
	public static final String ACTION_MEMBERS_INFO = "android.intent.action.MEMBERS_INFO";
	public static final String ACTION_COMMAND = "com.zzy.ptt.action.COMMAND";
	public static final String ACTION_DEBUG = "com.zzy.ptt.action.DEBUG";
	public static final String ACTION_NUMBER_KEY1 = "android.intent.action.key_number";
	public static final String ACTION_NUMBER_KEY2 = "com.zzy.ptt.action.KEY_NUMBER";
	public static final String ACTION_INPUT_NUMBER = "com.zzy.ptt.dialpage.neednum";
	public static final String ACTION_CM_HOIND = "android.intent.action.cm_hoind";
	public static final String ACTION_PTT_START = "android.intent.action.PTT.start";
	public static final String ACTION_PTT_CM_START = "android.intent.action.PTT.cm.start";

	/* register state */
	public static final int REG_NOT = 0;
	public static final int REG_ING = 1;
	public static final int REG_ERROR = 2;
	public static final int REG_ED = 3;

	/* ptt type */
	public static final int PTT_SINGLE = 1;
	public static final int PTT_PTT = 2;

	/* number type */
	public static final int NUMBER_DIGIT = 0;
	public static final int NUMBER_SIPURI = 1;

	/* call state */
	public static final int CALL_IDLE = 0;
	public static final int CALL_DIALING = 1;
	public static final int CALL_RINGBACK = 2;
	public static final int CALL_RINGING = 3;
	public static final int CALL_TALKING = 4;
	public static final int CALL_ENDING = 6;
	public static final int CALL_ENDED = 5;
	public static final int CALL_ERROR = 7;
	public static final int CALL_CANCELING = 8;
	public static final int CALL_REJECTING = 9;

	/* group statatus */
	public static final int GRP_BUYS = 0;
	public static final int GRP_FREE = 0;

	/* audio route */
	public static final int AR_HANDSET = 0;
	public static final int AR_SPEAKER = 1;
	public static final int AR_EARPHONE = 2;

	/* notification id */
	public static final int NOTIFY_ID = (int) (Math.random() * (100000));

	/* mainpage item key and value */
	public static final String ITEM_TEXT = "ItemText";
	public static final String ITEM_IMAGE = "ItemImage";

	/* jni return value */
	public static final int RETURN_SUCCESS = 0;
	public static final int RETURN_ERROR_CODE = 1;

	/* cm state */
	public static final int CM_CONNECTED = 0;
	public static final int CM_DISCONNECTED = 1;

	public static final int INIT_NOT = 0;
	public static final int INIT_ING = 1;
	public static final int INIT_SUCCESS = 2;
	public static final int INIT_FAIL = 3;

	public static final String TAG_PREFIX = "ZZY-PTT";

	// ptt custom
	public static final int KEYCODE_PTT = 92;
	public static final int KEYCODE_W_CALL = 93;
	public static final int KEYCODE_W_ENDCALL = 94;

	// log type
	public static final int LOG_ALL = 3;
	public static final int LOG_MISSING = 2;
	public static final int LOG_ANSWERED = 1;
	public static final int LOG_OUT = 0;

	public static final int INVALID_CALLID = -1;

	// group member status
	public static final int MEMBER_OFFLINE = 0;
	public static final int MEMBER_LISTENING = 1;
	public static final int MEMBER_SPEAKING = 2;
	public static final int MEMBER_ONLINE = 3;

	// ptt ui state
	public static final int PTT_IDLE = -1;
	public static final int PTT_INIT = 0;
	public static final int PTT_FREE = 1;
	public static final int PTT_LISTENING = 2;
	public static final int PTT_BUSY = 3;// same as PTT_LISTENING
	public static final int PTT_WAITING = 4;
	public static final int PTT_REJECTED = 5;
	public static final int PTT_CANCELED = 6;
	public static final int PTT_FORBIDDEN = 7;
	public static final int PTT_SPEAKING = 8;
	public static final int PTT_ACCEPTED = 9;
	public static final int PTT_APPLYING = 10;
	public static final int PTT_CANCELING = 11;
	public static final int PTT_CLOSED = 12;
	// public static final int PTT_BACKGROUND = 12;

	public static final int PTT_ALERT_ACCEPT = 0;
	public static final int PTT_ALERT_RELEASE = 1;

//	public static final int COMM_MAKE_PTT_CALL = 0;
	public static final int COMM_HANGUP_PTT_CALL = 1;
	public static final int COMM_APPLY_PTT_RIGHT = 2;
	public static final int COMM_CANCEL_PTT_RIGHT = 3;
	public static final int COMM_OPEN_PTT_UI = 4;
	public static final int COMM_START_APP = 5;
	public static final int COMM_UPDATE_PTT_UI = 6;
	public static final int COMM_CM_DISCONNECT = 7;
	public static final int COMM_CM_CONNECT = 8;
	public static final int COMM_DEFAULT_GRP_CHANGED = 9;
	public static final int COMM_CLOSE_PTT = 10;
	public static final int COMM_MAKE_CALL = 11;
	public static final int COMM_SEND_DTMF = 12;
	public static final int COMM_ANSWER_CALL = 13;
	public static final int COMM_HANGUP_CALL = 14;

	// error code
	public static final int ERROR_CODE_MINI = 300;
	public static final int ERROR_CODE_MEDIA_FAIL = 220202;
	public static final int RESPONSE_OK = 200;
	public static final int ERROR_CODE_401 = 401;
	public static final int ERROR_CODE_403 = 403;
	public static final int ERROR_CODE_408 = 408;
	public static final int ERROR_CODE_481 = 481;
	public static final int ERROR_CODE_500 = 500;
	public static final int ERROR_CODE_503 = 503;
	public static final int ERROR_CODE_502 = 502;
	public static final int ERROR_CODE_486 = 486;
	public static final int ERROR_CODE_504 = 504;
	public static final int ERROR_CODE_120128 = 120128;
	public static final int ERROR_CODE_70010 = 70010;
	
/*	public static final String DEBUG_LOG_ROOT = "/data/data/com.zzy.ptt/files";
	public static final String DEBUG_LOG_PATH = "/data/data/com.zzy.ptt/files/log/";
	public static final String DEBUG_PCAP_PATH = "/data/data/com.zzy.ptt/files/pcap/";*/
	
	public static final boolean DUMMY = false;

}
