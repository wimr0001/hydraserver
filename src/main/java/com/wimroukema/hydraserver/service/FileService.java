package com.wimroukema.hydraserver.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimroukema.hydraserver.model.BatchTimeList;
import com.wimroukema.hydraserver.model.LogMessage;
import com.wimroukema.hydraserver.model.Relay;
import com.wimroukema.hydraserver.model.RelayList;

@Service
public class FileService {
	@Value("${file.relays.json}")
	private String fileRelays;
	@Value("${file.groups.json}")
	private String fileGroups;
	@Value("${file.log}")
	private String logFile;
	@Value("${file.batchtimes}")
	private String batchtimes;
	
	private static String CONFIG_HEADER = "{\"time\": 1690699123,\"nextpoll\": 60,\"message\": \"\",\"simRelays\": 1,\"options\": 1,\"stupdate\": 0,\"master\": 0,\"master_timer\": 0,\"master_post_timer\": 0,\"expanders\": [],\"sensors\": [],";
	
	private String baseDir = System.getProperty("user.dir");

	public String getRelaysAsJsonString(ArrayList<Relay> list) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = mapper.writeValueAsString(list);
		return jsonString;
	}
	public RelayList getAllRelays() throws IOException {
		File file = new File(fileRelays);
		BufferedReader reader = new BufferedReader(new FileReader(baseDir+"/"+file));
		String line = null;
		StringBuffer sb = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			sb.append(line.trim());
		}
		reader.close();
		ObjectMapper mapper = new ObjectMapper();
		RelayList list = mapper.readValue(sb.toString(), RelayList.class);
		return list;
	}
	public String getGroupsInput() throws Exception {
		File file = new File(fileGroups);
		BufferedReader reader = new BufferedReader(new FileReader(baseDir+"/"+file));
		String line = null;
		StringBuffer sb = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			// sb.append(line.replaceAll("\\s+", ""));
			sb.append(line.trim());
		}
		reader.close();
		return sb.toString();
	}

	public ArrayList<LogMessage> getActuallog() throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(baseDir+"/"+logFile));
		return this.getLogMessages(reader);
	}

	public ArrayList<LogMessage> getLogFromFile(String filename) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		return this.getLogMessages(reader);
	}
	public String[] getBatchtimes() throws Exception {
		File file = new File(batchtimes);
		BufferedReader reader = new BufferedReader(new FileReader(baseDir+"/"+file));
		String line = null;
		StringBuffer sb = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			sb.append(line.trim());
		}
		reader.close();
		String regex = "[;\s]";
		String[] arr = sb.toString().split(regex);
		String[] times = new String[arr.length];
		int i = 0;
		for (String time : arr) {
			times[i] = time;
			i++;
		}
		return times;
	}
	public void saveBatchTimes(BatchTimeList list) throws Exception {
		String csv = list.toCsv();
		FileWriter writer = new FileWriter(new File(batchtimes));
		writer.write(csv);
		writer.flush();
		writer.close();
	}
	public void storeRelays(String jsonString) throws IOException {
	
		File file = new File(fileRelays);
		FileWriter writer = new FileWriter(baseDir+"/"+file);
		StringBuffer sb = new StringBuffer();
		sb.append(CONFIG_HEADER);
		sb.append("\"relays\":");
		sb.append(jsonString);
		sb.append("}");
		writer.write(sb.toString());
		writer.close();
	}

	private ArrayList<LogMessage> getLogMessages(BufferedReader reader) throws Exception {
		String line = null;
		ArrayList<LogMessage> list = new ArrayList<LogMessage>();
		LogMessage lm;
		while ((line = reader.readLine()) != null) {
			if (line.trim().length() > 0) {
				lm = new LogMessage();
				lm.fillContent(line);
				list.add(lm);
			}
		}
		reader.close();
		return list;
	}
}
