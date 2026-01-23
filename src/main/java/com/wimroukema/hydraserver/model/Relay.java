package com.wimroukema.hydraserver.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties
public class Relay {
	@JsonProperty("relay_id")
	private int relayId;
	
	private int run;
	
	private int type;
	
	private String name;
	private int active;
	private long startedOn;
	private long stoppedOn;
	private long startPlanned;
	private int runLeft;
	private int sequence;
	private long time;
	private int period;
}
