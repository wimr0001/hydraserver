package com.wimroukema.hydraserver.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wimroukema.hydraserver.model.LogMessage;
import com.wimroukema.hydraserver.model.Relay;
import com.wimroukema.hydraserver.model.WateringRequest;
import com.wimroukema.hydraserver.service.FileService;
import com.wimroukema.hydraserver.service.RelayService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Configuration
//@ComponentScan(basePackageClasses = RelayService.class)
@CrossOrigin(origins = "*")
public class GeneralController {

	@Autowired
	private FileService fileService;
	@Autowired
	private RelayService relayService;

	@PostMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> status(@RequestBody WateringRequest request) {
		Map<String, Object> map = new HashMap<String, Object>(6);
		try {
			String relays = fileService.getRelaysInput();
			map.put("relays", relays);
			String groups = fileService.getGroupsInput();
			map.put("groups", groups);
			map.put("active_relays", relayService.getActiveRelays());
			map.put("username", relayService.getUsername());
			map.put("errormsg", "");
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			// e.printStackTrace();
			map.put("errormsg", "Opvragen informatie is mislukt");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
		}
	}

	@PostMapping(path = "/startProcess", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> startProcess(@RequestBody WateringRequest request) {
		Map<String, Object> map = new HashMap<String, Object>(6);
		try {
			if (request.getRelays().size() < 1) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
			}
			relayService.setRelays(request.getRelays());
			relayService.setBatch(request.isBatch());
			relayService.setDelay(request.getDelay());
			relayService.setUsername(request.getUsername());
			map.put("active_relays", relayService.startProcess());
			map.put("errormsg", "");
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			// e.printStackTrace();
			map.put("errormsg", "Starten van sproeien is mislukt");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
		}
	}

	@PostMapping(path = "/stopProcess", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> stopProcess(@RequestBody WateringRequest request) {
		Map<String, Object> map = new HashMap<String, Object>(6);
		try {
			map.put("active_relays", relayService.stopProcess());
			map.put("errormsg", "");
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			map.put("errormsg", "Stoppen van sproeien is mislukt");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
		}
	}

	@PostMapping(path = "/stopRelay", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> stopRelay(@RequestBody WateringRequest request) {
		Map<String, Object> map = new HashMap<String, Object>(6);
		try {
			relayService.stopRelay(request.getRelayId());
			map.put("active_relays", relayService.getActiveRelays());
			map.put("errormsg", "");
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			map.put("errormsg", "Stoppen van sproeien is mislukt");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
		}
	}

	@PostMapping(path = "/removeRelay", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> removeRelay(@RequestBody WateringRequest request) {
		Map<String, Object> map = new HashMap<String, Object>(6);
		try {
			relayService.removeRelay(request.getRelayId());
			map.put("active_relays", relayService.getActiveRelays());
			map.put("errormsg", "");
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			map.put("errormsg", "Verwijderen niet toegestaan cq. mislukt");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
		}
	}

	@PostMapping(path = "/actives", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getActives(@RequestBody WateringRequest request) {
		Map<String, Object> map = new HashMap<String, Object>(6);
		try {
			map.put("active_relays", relayService.getActiveRelays());
			String username = relayService.getUsername();
			if (username == null) {
				username = "";
			}
			map.put("username", username);
			map.put("errormsg", "");
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			map.put("errormsg", "Opvragen sproei-informatie is mislukt");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
		}
	}

	@PostMapping(path = "/clearProcess", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> clearProcess(@RequestBody WateringRequest request) {
		Map<String, Object> map = new HashMap<String, Object>(6);
		try {
			relayService.clear();
			String relays = fileService.getRelaysInput();
			map.put("relays", relays);
			String groups = fileService.getGroupsInput();
			map.put("groups", groups);
			map.put("active_relays", "");
			map.put("username", relayService.getUsername());
			map.put("errormsg", "");
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			map.put("errormsg", "Initialiseren sproei-informatie is mislukt");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
		}
	}

	@PostMapping(path = "/repeatProcess", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> repeatProcess(@RequestBody WateringRequest request) {
		Map<String, Object> map = new HashMap<String, Object>(6);
		try {
			relayService.restartProcess();
			map.put("active_relays", relayService.getRelays());
			map.put("username", relayService.getUsername());
			map.put("errormsg", "");
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			map.put("errormsg", "herhaalde uitvoering is mislukt");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
		}
	}

	@PostMapping(path = "/addRelays", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> addRelays(@RequestBody WateringRequest request) {
		Map<String, Object> map = new HashMap<String, Object>(6);
		try {
			if (request.getRelays().size() < 1) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
			}
			relayService.addRelays(request.getRelays());
			map.put("active_relays", relayService.getActiveRelays());
			map.put("errormsg", "");
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			// e.printStackTrace();
			map.put("errormsg", "Toevoegen van banen is mislukt");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
		}
	}

	@GetMapping(path = "/actuallog", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ArrayList<LogMessage>> actuallog() {
		try {
			ArrayList<LogMessage> logLines = fileService.getActuallog();
			return ResponseEntity.status(HttpStatus.OK).body(this.skipStartMessages(logLines));
		} catch (Exception e) {
			// e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<LogMessage>(0));
		}
	}

	@GetMapping(path = "/otherlog", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ArrayList<LogMessage>> otherLog(@RequestParam String filename) {
		try {
			ArrayList<LogMessage> logLines = fileService.getLogFromFile(filename);

			return ResponseEntity.status(HttpStatus.OK).body(this.skipStartMessages(logLines));
		} catch (Exception e) {
			// e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<LogMessage>(0));
		}
	}

	private ArrayList<LogMessage> skipStartMessages(ArrayList<LogMessage> logLines) throws Exception {
		// skip the start entries
		ArrayList<LogMessage> result = new ArrayList<LogMessage>(logLines.size());
		int j = 0;
		for (LogMessage lm : logLines) {
			j++;
			if (lm.getStoppedOn() == 0) {
				if (j < logLines.size()) {
					if (logLines.get(j).getRelayId() == lm.getRelayId()) {
						// skip this entry
					} else {
						// skip this entry and write error in error logfile because
						// the relay was started but never stopped in the log
						relayService.writeErrorEntry(lm);
					}
				}
			} else {
				result.add(lm);
			}
		}
		return result;
	}

	@GetMapping(path = "/backgroundProcess", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> backGroundProcess(@RequestParam String runTime) {
		LocalDateTime ldt = LocalDateTime.now();
		int h = ldt.getHour();
		int m = ldt.getMonthValue();
		try {
			String result = "OK";
			int[] batchtimes = fileService.getBatchtimes(); // from 0 - 7
			// 1 = whole hour
			// 2 = after 15 minutes 3 = after 30 minutes and 4 = after 45 minutes
			if (h >= batchtimes.length) {
				result = "Not executed";
				return ResponseEntity.status(HttpStatus.OK).body(result);
			}
			if (batchtimes[h] == 0) {
				result = "Not executed";
			}
			boolean mustExecute = false;
			switch (batchtimes[h]) {
			case 1: {
				if (m <= 15) {
					mustExecute = true;
				}
			}
			case 2: {
				if (m > 15 && m <= 30) {
					mustExecute = true;
				}
			}
			case 3: {
				if (m > 30 && m <= 45) {
					mustExecute = true;
				}
			}
			case 4: {
				if (m > 45) {
					mustExecute = true;
				}
			}

			}
			if (!mustExecute) {
				result = "Not executed";
			}
			if (relayService.isActive()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Process already active");
			}
			ArrayList<Relay> list = fileService.getRelayList();
			int run = Integer.valueOf(runTime);
			if (run > 0) {
				run = Math.min(300, run);
			}
			if (!result.equals("OK")) {
				return ResponseEntity.status(HttpStatus.OK).body(result);
			}
			for (Relay relay : list) {
				if (run > 0) {
					relay.setRun(run);
				}
				relay.setActive(1);
			}

			relayService.setRelays(list);
			relayService.setBatch(true);
			relayService.setUsername("batch");
			relayService.startProcess();
			return ResponseEntity.status(HttpStatus.OK).body(result);
		} catch (Exception e) {
			 e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Process in error");
		}
	}
}
