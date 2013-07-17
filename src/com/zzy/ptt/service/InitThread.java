/**
 * Copyright 2012 zzy Tech. Co., Ltd.
 * All right reserved.
 * Project:zzy PTT V1.0
 * Name:InitThread.java
 * Description：TODO
 * Author:LiXiaodong
 * Version:1.0
 * Date:2012-3-17
 */

package com.zzy.ptt.service;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.util.Log;

import com.zzy.ptt.R;
import com.zzy.ptt.exception.PTTException;
import com.zzy.ptt.proxy.SipProxy;
import com.zzy.ptt.util.PTTConstant;

/**
 * @author Administrator
 * 
 */
public class InitThread extends Thread {

	private static final String LOG_TAG = "InitThread";
	private Context context;

	public InitThread(Context context) {
		this.context = context;
	}

	@Override
	public void run() {
		Log.d(LOG_TAG, "InitThread RequestInit start");
		int result = -1;
		try {
			result = SipProxy.getInstance().RequestInit();
		} catch (PTTException e) {
			Log.e(LOG_TAG, "RequestInit," + e.getMessage());
			result = PTTConstant.INIT_FAIL;
		}
		
		if(PTTConstant.DUMMY) {
			result = PTTConstant.RETURN_SUCCESS;
		}

		Log.d(LOG_TAG, "InitThread RequestInit return : " + result);
		Intent intent = new Intent(PTTConstant.ACTION_INIT);
		// intent.setClass(context, MainPageActivity.class);
		// !importatnt : add the line above will make the broadcast never be
		// received
		if (result == PTTConstant.RETURN_SUCCESS) {
			// init success
			StateManager.setInitState(PTTConstant.INIT_SUCCESS);
			intent.putExtra(PTTConstant.KEY_INIT_STATE, PTTConstant.INIT_SUCCESS);
			Log.d(LOG_TAG, "InitThread sendBroadcast ok>>>>>>>>>>>");
			context.sendBroadcast(intent);
		} else {
			StateManager.setInitState(PTTConstant.INIT_FAIL);
			intent.putExtra(PTTConstant.KEY_INIT_STATE, PTTConstant.INIT_FAIL);
			intent.putExtra(PTTConstant.KEY_RETURN_VALUE, result);
			Log.d(LOG_TAG, "InitThread sendBroadcast ok>>>>>>>>>>>");
			context.sendBroadcast(intent);
		}
		// clear noise
		clearMicNoise();
	}

	private void clearMicNoise() {
		Log.d(LOG_TAG, "InitThread clearMicNoise >>>>>>>>>>>");
		MediaPlayer mp = MediaPlayer.create(context, R.raw.pttrelease);
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setSpeakerphoneOn(false);
		audioManager.setMode(AudioManager.MODE_NORMAL);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
		mp.setLooping(false);

		//remove the illegal state
		if (mp != null) {
			mp.stop();
		}
		try {
			mp.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mp.start();
		mp.setOnCompletionListener(new OnCompletionListener() {

			@Override
			public void onCompletion(MediaPlayer player) {
				player.release();
			}
		});
		mp.setOnErrorListener(new OnErrorListener() {

			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				mp.stop();
				mp.release();
				return true;
			}
		});
	}
}
