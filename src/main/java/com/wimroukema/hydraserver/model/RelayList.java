package com.wimroukema.hydraserver.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class RelayList {
	private long time;
	private int nextpoll;
	private String message;
	private int simRelays;
	private int options;
	private int stupdate;
	private int master;
	@JsonProperty("master_timer")
	private int masterTimer;
	@JsonProperty("master_post_timer")
	private int masterPostTimer;
	private String[] expanders;
	private String[] sensors;
	
	@JsonProperty("relays")
	private ArrayList<Relay> relayList;	
	
	public ArrayList<Relay> removeSuspendedRelays() {
		ArrayList<Relay> list = new ArrayList<Relay>(relayList.size());
		for (Relay relay : relayList) {
			if (relay.getType() != 110) {
				list.add(relay);
			}
		}
		return list;
	}
}
