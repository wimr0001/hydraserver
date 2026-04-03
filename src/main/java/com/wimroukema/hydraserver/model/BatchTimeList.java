package com.wimroukema.hydraserver.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class BatchTimeList {
	@JsonProperty("batchTimes")
	private ArrayList<BatchTime> batchTimeList;

	public String toCsv() {
		int i = 0;
		StringBuffer sb = new StringBuffer();
		for (BatchTime time : batchTimeList) {
			sb.append(time.getMinuteCode());
			sb.append("-");
			sb.append(time.getRun());
			if (i < batchTimeList.size() - 1) {
				sb.append(";");
			}
			i++;
		}
		
		sb.append("\n");
		return sb.toString();
	}
}
