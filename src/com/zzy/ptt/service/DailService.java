package com.zzy.ptt.service;

public class DailService {
	private static DailService instance = new DailService();
	public int strNumber=-1;
	public static DailService getInstance() {
		if (instance == null) {
			instance = new DailService();
		}
		return instance;
	}


}
