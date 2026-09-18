package com.wimroukema.hydraserver.model;

import java.util.ArrayList;

import lombok.Data;

@Data
public class WateringRequest {

	private String username;
	private int relayId;
	private ArrayList<Relay> relays;
	private int delay;
	private boolean sendMessage;
	private int round = 0;
	private int maxRounds = 2;
	private boolean restart = false;
}
