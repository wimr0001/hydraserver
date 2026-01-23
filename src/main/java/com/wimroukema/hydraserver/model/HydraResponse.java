package com.wimroukema.hydraserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class HydraResponse {
	private String message;
	@JsonProperty("message_type")
	private String messageType;
}
