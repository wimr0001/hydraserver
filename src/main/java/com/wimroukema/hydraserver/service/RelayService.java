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
import reactor.core.publisher.Mono;

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
	private int round = 0;
	private int maxRounds = 2;
	private ZoneService zs = new ZoneService(this);

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
		return relays;
	}

	public void repeatProcess() {
		round++;
		try {
			startProcess(this.relays, 0, this.username);
		} catch (Exception e) {
			round = 0;
			try {
				this.stopProcess();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	public List<Relay> startProcess(List<Relay> list, int delay, String username) throws Exception {
		for (Relay relay : list) {
			relay.setRunLeft(relay.getRun());
			relay.setStartedOn(0);
			relay.setStartPlanned(0);
			relay.setStoppedOn(0);
			relay.setActive(1);
		}
		this.relays = list;
		this.delay = delay;
		this.username = username;
		try {
			zs.setStopProcess(false);
			this.setStartpoint();
			this.writeNotificationFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (round < 2) {
			thread = new Thread(zs);
			thread.start();
		}
		return this.relays;
	}

	private void stopRelay(int relayId, boolean stopProcess) throws Exception {
		int i = -1;
		round = 0;
		LocalDateTime ldt = LocalDateTime.now();
		for (Relay relay : relays) {
			i++;
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
		}
		try {
			thread.interrupt();
		} catch (Exception e) {
			// TODO Auto-generated catch block

		}
		this.doStopCall(relayId);
		Relay relay = this.getRelay(relayId);
		this.writeLogMessage(relay, false, true);
		if (!stopProcess) {
			if (i < relays.size() - 1) {
				this.setStartpoint();
			} else {
				zs.setStopProcess(true);
				try {
					thread.interrupt();
				} catch (Exception e) {
					// TODO Auto-generated catch block
				}
				if (batch) {
					relays = new ArrayList<Relay>(0);
				}
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
		round = 0;
		for (Relay relay : relays) {
			if (relay.getActive() == 2) {
				this.stopRelay(relay.getRelayId(), true);
			} else {
				if (relay.getActive() < 2) {
					relay.setActive(3);

				}
			}
		}
		try {
			thread.interrupt();
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
		this.batch = false;
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
						if (relay.getRunLeft() < 0) {
							relay.setRunLeft(0);
							relay.setActive(9);
						}
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
			if (round > 0 && round < maxRounds) {
				this.repeatProcess();
				return;
			}
			zs.setStopProcess(true);
			try {
				thread.interrupt();
				thread = null;
			} catch (Exception e) {
				// nothing to do
			}

			this.processEnded();
			return;
		}
		relay.setActive(2);
		this.adjustRelayList();
		zs.setRelay(relay);
		this.doStartCall(relay);
		this.writeLogMessage(relay, true, false);
	}

	private void doStartCall(Relay relay) throws Exception {
		String uri = zoneUrl + "?api_key=" + apiKey + "&action=run&period_id=999&custom=" + relay.getRun()
				+ "&relay_id=" + relay.getRelayId();
		webClient.get().uri(uri).retrieve().bodyToMono(HydraResponse.class).doOnNext(response -> {
			if (response.getMessageType().equals("error")) {
				round = 0;
				try {
					stopProcess();
				} catch (Exception e) {
					relays = new ArrayList<Relay>(0);
				}
			}
		}).doOnError(error -> {
			System.err.println("Error: " + error.getMessage());
		}).subscribe();
	}

	private void doStopCall(int relayId) throws Exception {
		String uri = zoneUrl + "?api_key=" + apiKey + "&action=stop" + "&relay_id=" + relayId;
		webClient.get().uri(uri).retrieve().bodyToMono(HydraResponse.class).doOnNext(response -> {
			if (response.getMessageType().equals("error")) {
				round = 0;
				try {
					stopProcess();
				} catch (Exception e) {
					relays = new ArrayList<Relay>(0);
				}
			}
		}).doOnError(error -> {
			System.err.println("Error: " + error.getMessage());
		}).subscribe();
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
