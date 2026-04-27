package com.wimroukema.hydraserver.service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wimroukema.hydraserver.model.HydraResponse;
import com.wimroukema.hydraserver.model.LogMessage;
import com.wimroukema.hydraserver.model.Relay;

import lombok.Data;

@ApplicationScope
@Data
@Component
public class RelayService {

	@Autowired
	WebClient webClient;
	@Autowired
	MailSenderImpl mailSender;

	@Value("${file.log}")
	private String logFile;
	@Value("${file.error}")
	private String errorFile;
	@Value("${file.notification}")
	private String notificationFile;
	@Value("${rootUser}")
	private String rootUser;
	@Value("${batchUser}")
	private String batchUser;
	@Value("${zoneUrl}")
	private String zoneUrl;
	@Value("${apiKey}")
	private String apiKey;
	@Value("${notification_link}")
	private String notificationLink;
	@Value("${mailsender.sendmail}")
	private boolean sendMail;

	private String username;
	private List<Relay> relays = new ArrayList<Relay>(0);
	private int delay;
	private boolean batch;
	private boolean sendMessage;
	private Thread thread;
	private FileWriter writer;
	private FileWriter errorWriter;
	private int notificationNumber = 0;
	private ZoneService zs;

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		try {
			writer = new FileWriter(logFile, true);
			errorWriter = new FileWriter(errorFile, true);
		} catch (IOException e) {
			// no log writing
		}
	}

	@EventListener(ApplicationFailedEvent.class)
	public void onFailed() {
		try {
			writer.flush();
			writer.close();
			errorWriter.flush();
			errorWriter.close();
		} catch (IOException e) {
			// no log writing
		}
	}

	@EventListener(ContextClosedEvent.class)
	public void onShutdown() {
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// no log writing
		}
	}

	public List<Relay> getActiveRelays() {
		// active relays, adjust the runleft time
		if (relays.size() == 0) {
			return relays;
		}
		adjustRelayList();
		for (Relay relay : relays) {
			if (relay.getActive() == 2) {
				if (relay.getRunLeft() < 2) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// do nothing
					}
				}
				break;
			}
		}
		return relays;
	}

	public List<Relay> startProcess() throws Exception {
		this.zs = new ZoneService(this);
		try {
			this.setStartpoint();
			this.writeNotificationFile();
		} catch (Exception e) {
			this.relays = new ArrayList<Relay>(0);
			throw e;
		}

		thread = new Thread(zs);
		thread.start();
		return this.relays;
	}

	public void restartProcess() throws Exception {
		if (relays.size() == 0) {
			throw new Exception("Cannot repeat the process");
		}
		// set activecode to 1
		for (Relay relay : relays) {
			relay.setActive(1);
			relay.setRunLeft(relay.getRun());
			relay.setStartedOn(0);
		}
		try {
			this.setStartpoint();
		} catch (Exception e) {
			this.relays = new ArrayList<Relay>(0);
			throw e;
		}
		thread = new Thread(zs);
		thread.start();
	}

	private void stopRelay(int relayId, boolean stopProcess) throws Exception {
		int i = 0;
		LocalDateTime ldt = LocalDateTime.now();
		for (Relay relay : relays) {
			if (relay.getRelayId() == relayId) {
				if (relay.getActive() == 2) {
					relay.setActive(3);
					ldt = LocalDateTime.now();
					relay.setStoppedOn(ldt.toEpochSecond(ZoneOffset.UTC));
					relay.setRunLeft(0);
					break;
				} else {
					throw new Exception("Kan het sproeien van deze baan niet stoppen");
				}
			}
			i++;
		}
		thread.interrupt();
		this.doStopCall(relayId);
		Relay relay = this.getRelay(relayId);
		this.writeLogMessage(relay, false, true);

		if (i < relays.size() - 1) {
			this.setStartpoint();
			thread = new Thread(zs);
			thread.start();
		} else {
			zs.setStopProcess(true);
			if (batch) {
				relays = new ArrayList<Relay>(0);
			}
		}
	}

	public void stopRelay(int relayId) throws Exception {
		this.stopRelay(relayId, false);
	}

	public void removeRelay(int relayId) throws Exception {
		int i = 0;
		boolean removed = false;
		for (Relay relay : relays) {
			if (relay.getRelayId() == relayId) {
				if (relay.getActive() < 2) {
					removed = true;
					break;
				} else {
					throw new Exception("Kan het sproeien van deze baan niet stoppen");
				}
			}
			i++;
		}
		if (removed) {
			relays.remove(i);
			i--;
		}
	}

	public List<Relay> stopProcess() throws Exception {
		for (Relay relay : relays) {
			if (relay.getActive() == 2) {
				this.stopRelay(relay.getRelayId(), true);
			} else {
				if (relay.getActive() < 2) {
					relay.setActive(3);

				}
			}
		}
		if (batch) {
			relays = new ArrayList<Relay>(0);
			username = "";
		}
		return relays;
	}

	public void addRelays(ArrayList<Relay> list) {
		for (Relay relay : list) {
			this.relays.add(relay);
		}
		this.adjustRelayList();
	}

	protected void processEnded() {
		if (batch) {
			relays = new ArrayList<Relay>(0);
			username = "";
		}
	}

	public void clear() {
		relays = new ArrayList<Relay>(0);
		username = "";
	}

	public void adjustRelayList() {
		LocalDateTime ldt = LocalDateTime.now();
		int runLeft = 0;
		for (Relay relay : relays) {
			ldt = ldt.plusSeconds(runLeft);
			if (relay.getActive() < 3) {
				if (relay.getActive() == 2) {
					if (relay.getStartedOn() == 0) {
						relay.setStartedOn(ldt.toEpochSecond(ZoneOffset.UTC));
						relay.setStartPlanned(ldt.toEpochSecond(ZoneOffset.UTC));
						relay.setRunLeft(relay.getRun());
						runLeft = relay.getRunLeft();
					} else {
						int n = (int) (ldt.toEpochSecond(ZoneOffset.UTC) - relay.getStartedOn());
						relay.setRunLeft(relay.getRun() - n);
						runLeft = relay.getRunLeft();
					}
				} else {
					relay.setRunLeft(relay.getRun());
					relay.setStartPlanned(ldt.toEpochSecond(ZoneOffset.UTC));
					relay.setActive(1);
					runLeft = relay.getRunLeft();
				}
			}
		}
	}

	public void setStartpoint() throws Exception {
		Relay relay = this.getNext();
		if (relay == null) {
			zs.setStopProcess(true);
			return;
		}
		relay.setActive(2);
		this.adjustRelayList();
		this.doStartCall(relay);
		this.writeLogMessage(relay, true, false);
	}

	private void doStartCall(Relay relay) throws Exception {
		String uri = zoneUrl + "?api_key=" + apiKey + "&action=run&period_id=999&custom=" + relay.getRun()
				+ "&relay_id=" + relay.getRelayId();
		String resp = webClient.get().uri(uri).retrieve()
//				.onStatus(HttpStatusCode::is4xxClientError,
//						clientResponse -> clientResponse.bodyToMono(String.class)
//								.flatMap(body -> Mono.error(new RuntimeException("Client Error: " + body))))
//				.onStatus(HttpStatusCode::is5xxServerError,
//						clientResponse -> clientResponse.bodyToMono(String.class)
//								.flatMap(body -> Mono.error(new RuntimeException("Server Error: " + body))))
				.bodyToMono(String.class)
//				.doOnError(WebClientResponseException.class, e -> {
//					// Handle error and log it
//					System.err.println("Error occurred: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
//				})
				.block();
		ObjectMapper mapper = new ObjectMapper();
		HydraResponse hydraResponse;
		try {
			hydraResponse = mapper.readValue(resp, HydraResponse.class);
		} catch (JsonMappingException e) {
			e.printStackTrace();
			System.out.println(e.toString());
			throw new Exception("Exception in Hunter server");
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			System.out.println(e.toString());
			throw new Exception("Exception in Hunter server");
		}
		if (hydraResponse.getMessageType().equals("error")) {
			System.out.println(hydraResponse.getMessageType());
			throw new Exception("Exception in Hunter server");
		}
	}

	private void doStopCall(int relayId) throws Exception {
		String uri = zoneUrl + "?api_key=" + apiKey + "&action=stop" + "&relay_id=" + relayId;
		String resp = webClient.get().uri(uri).retrieve().bodyToMono(String.class).block();

		ObjectMapper mapper = new ObjectMapper();
		HydraResponse hydraResponse;
		try {
			hydraResponse = mapper.readValue(resp, HydraResponse.class);
		} catch (JsonMappingException e) {
			throw new Exception("Exception in Hunter server");
		} catch (JsonProcessingException e) {
			throw new Exception("Exception in Hunter server");
		}
		if (hydraResponse.getMessageType().equals("error")) {
			throw new Exception("Exception in Hunter server");
		}
	}

	public Relay getNext() {
		if (relays.size() == 0) {
			return null;
		}
		for (Relay relay : relays) {
			if (relay.getActive() < 2) {
				return relay;
			}
		}
		return null;
	}

	public Relay getActiveRelay() {
		if (relays.size() == 0) {
			return null;
		}
		for (Relay relay : relays) {
			if (relay.getActive() == 2) {
				return relay;
			}
		}
		return null;
	}

	public boolean isActive() {
		for (Relay relay : relays) {
			if (relay.getActive() < 3) {
				return true;
			}
		}
		return false;
	}

	public void writeLogMessage(Relay relay, boolean start, boolean stopped) {
		LogMessage lm = new LogMessage();
		LocalDateTime ldt = LocalDateTime.now();
		lm.setRelayId(relay.getRelayId());
		lm.setStartedOn(relay.getStartedOn());
		if (stopped) {
			lm.setStoppedOn(relay.getStoppedOn());
		}
		int h = ldt.getYear();
		int m = ldt.getMonthValue();
		int d = ldt.getDayOfMonth();
		lm.setLogDate(h + "-" + String.format("%02d", m) + "-" + String.format("%02d", d));
		if (this.username.equals("")) {
			lm.setUsername("xxx");
		} else {
			lm.setUsername(this.username);
		}
		try {
			writer.write(lm.toCsvString());
			writer.flush();
		} catch (IOException e) {
			// no logging
		}
	}

	public void writeErrorEntry(LogMessage lm) throws Exception {
		errorWriter.write(lm.toCsvString());
	}

	private Relay getRelay(int relayId) {
		for (Relay relay : relays) {
			if (relay.getRelayId() == relayId) {
				return relay;
			}
		}
		return null;
	}

	private void writeNotificationFile() throws Exception {
		if (username.equals(rootUser)) {
			return;
		}
		String fn = notificationFile.replace("{number}", Integer.toString(notificationNumber));
		notificationNumber++;
		FileWriter writer = new FileWriter(fn);
		HashMap<String, String> map = new HashMap<String, String>(3);
		map.put("title", "Opstarten sproeien door " + username);
		StringBuffer sb = new StringBuffer();
		sb.append("Banen: ");
		for (Relay relay : relays) {
			sb.append(relay.getName());
			sb.append(",");
		}
		map.put("content", sb.toString());
		map.put("link", notificationLink);
		ObjectMapper objectMapper = new ObjectMapper();
		String jacksonData = objectMapper.writeValueAsString(map);
		writer.write(jacksonData);
		writer.flush();
		writer.close();
		if (sendMail) {
			try {
				mailSender.sendMail(sb.toString());
			} catch (Exception e) {
				// no mail sent, it's a pity but no serious problem
			}
		}
	}
}
