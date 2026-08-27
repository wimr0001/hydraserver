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
	private boolean repeat;
}
