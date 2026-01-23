package com.wimroukema.hydraserver.model;

import lombok.Data;

@Data
public class LogMessage {
	private String logDate;
	private int relayId;
	private long startedOn;
	private long stoppedOn;
	private String username;
	
	public String toCsvString() {
		StringBuffer sb = new StringBuffer();
		sb.append(logDate);
		sb.append(";");
		sb.append(relayId);
		sb.append(";");
		sb.append(startedOn);
		sb.append(";");
		sb.append(stoppedOn);
		sb.append(";");
		sb.append(username);
		sb.append("\n");
		return sb.toString();
	}
	public LogMessage fillContent(String line) {
		String regex = "[;\s]";
		String[] arr = line.split(regex);
		this.logDate = arr[0];
		this.relayId = Integer.valueOf(arr[1]);
		this.relayId = Integer.valueOf(arr[1]);
		this.startedOn = Long.valueOf(arr[2]);
		this.stoppedOn = Long.valueOf(arr[3]);
		this.username = arr[4];
		return this;
	}
}
