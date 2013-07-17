package com.zzy.ptt.model;

public class VersionInfo {
	private int id;
	private String version;
	private String build_time;
	private String register_pw;

	public VersionInfo() {
		super();
		// TODO Auto-generated constructor stub
	}

	public VersionInfo(int id, String version, String build_time, String register_pw) {
		super();
		this.id = id;
		this.version = version;
		this.build_time = build_time;
		this.register_pw = register_pw;
	}

	public String getRegister_pw() {
		return register_pw;
	}

	public void setRegister_pw(String register_pw) {
		this.register_pw = register_pw;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getBuild_time() {
		return build_time;
	}

	public void setBuild_time(String build_time) {
		this.build_time = build_time;
	}

	@Override
	public String toString() {
		return "VersionInfo [id=" + id + ", version=" + version + ", build_time=" + build_time + ", register_pw="
				+ register_pw + "]";
	}


}
