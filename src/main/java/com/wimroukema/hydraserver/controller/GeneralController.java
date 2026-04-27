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

import com.wimroukema.hydraserver.model.BatchTime;
import com.wimroukema.hydraserver.model.BatchTimeList;
import com.wimroukema.hydraserver.model.BatchTimeRequest;
import com.wimroukema.hydraserver.model.LogMessage;
import com.wimroukema.hydraserver.model.Relay;
import com.wimroukema.hydraserver.model.RelayList;
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
			ArrayList<Relay> list = fileService.getAllRelays().removeSuspendedRelays();
			map.put("relays", fileService.getRelaysAsJsonString(list));
			String groups = fileService.getGroupsInput();
			map.put("groups", groups);
			map.put("active_relays", relayService.getActiveRelays());
			if (relayService.getActiveRelays().size() == 0) {
				map.put("username", request.getUsername());
			} else {
				map.put("username", relayService.getUsername());
			}
			map.put("errormsg", "");
			ArrayList<LogMessage> logLines = fileService.getActuallog();
			long started = 0;
			if (logLines.size() > 0) {
				LogMessage lm = logLines.get(logLines.size() - 1);
				started = lm.getStartedOn();
			}
			map.put("lastStarted", Long.valueOf(started));
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			e.printStackTrace();
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
			if (relayService.getActiveRelays().size() > 0) {
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
			e.printStackTrace();
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

	@GetMapping(path = "/actives", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getActives() {
		Map<String, Object> map = new HashMap<String, Object>(6);
		try {
			map.put("active_relays", relayService.getActiveRelays());
			String username = relayService.getUsername();
			map.put("username", username);
			map.put("errormsg", "");
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			map.put("errormsg", "Opvragen sproei-informatie is mislukt");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
		}
	}

	@GetMapping(path = "/clearProcess", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> clearProcess() {
		Map<String, Object> map = new HashMap<String, Object>(6);
		try {
			relayService.clear();
			ArrayList<Relay> list = fileService.getAllRelays().removeSuspendedRelays();
			map.put("relays", fileService.getRelaysAsJsonString(list));
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
	public ResponseEntity<String> backGroundProcess() {
		if (relayService.isActive()) {
			return ResponseEntity.status(HttpStatus.OK).body("Er loopt al een sproeiproces");
		}
		LocalDateTime ldt = LocalDateTime.now();
		int h = ldt.getHour();
		int m = ldt.getMinute();
		try {
			String result = "OK";
			String[] batchtimes = fileService.getBatchtimes();
			// from 0 - 23
			// 1 = whole hour
			// 2 = after 15 minutes 3 = after 30 minutes and 4 = after 45 minutes
			// and -0 : default time -1-5 time in minutes -99 blocked
			if (h >= batchtimes.length) {
				result = "Timestamp out of range";
				return ResponseEntity.status(HttpStatus.OK).body(result);
			}

			String regex = "[-\s]";
			String[] arr = batchtimes[h].split(regex);
			int timeCode = Integer.valueOf(arr[0]);
			int run = Integer.valueOf(arr[1]);
			if (run == 99 && timeCode > 0) {
				result = "Execution blocked";
				return ResponseEntity.status(HttpStatus.OK).body(result);
			}
			if (timeCode == 0) {
				result = "Must not execute";
			} else {
				boolean mustExecute = false;
				switch (timeCode) {
				case 1: {
					if (m < 15) {
						mustExecute = true;
					}
					break;
				}
				case 2: {
					if (m >= 15 && m < 30) {
						mustExecute = true;
					}
					break;
				}
				case 3: {
					if (m >= 30 && m < 45) {
						mustExecute = true;
					}
					break;
				}
				case 4: {
					if (m >= 45) {
						mustExecute = true;
					}
				}

				}
				if (!mustExecute) {
					result = "Not executed";
				}
			}
			if (!result.equals("OK")) {
				return ResponseEntity.status(HttpStatus.OK).body(result);
			}
			ArrayList<Relay> list = fileService.getAllRelays().removeSuspendedRelays();
			int duration = 0;
			if (run > 0 && run < 6) {
				duration = run * 60;
			}

			for (Relay relay : list) {
				if (duration > 0) {
					relay.setRun(duration);
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
			System.out.println(e.toString());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Process in error");
		}
	}

	@GetMapping(path = "/getBatchTimes", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<BatchTimeList> getBatchTimes() {
		try {
			String[] batchTimes = fileService.getBatchtimes();
			BatchTimeList timeList = new BatchTimeList();
			ArrayList<BatchTime> list = new ArrayList(7);
			BatchTime time = null;
			String regex = "[-s]";
			int i = 0;
			for (String str : batchTimes) {
				String[] arr = str.split(regex);
				int timeCode = Integer.valueOf(arr[0]);
				int run = Integer.valueOf(arr[1]);
				time = new BatchTime();
				time.setHour(i);
				time.setMinuteCode(timeCode);
				time.setRun(run);
				list.add(time);
				i++;
			}
			timeList.setBatchTimeList(list);
			return ResponseEntity.status(HttpStatus.OK).body(timeList);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new BatchTimeList());
		}
	}

	@PostMapping(path = "/postBatchTimes", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> postBatchTimes(@RequestBody BatchTimeRequest request) {
		try {
			String result = "OK";
			BatchTimeList list = new BatchTimeList();
			list.setBatchTimeList(request.getBatchTimes());
			fileService.saveBatchTimes(list);
			return ResponseEntity.status(HttpStatus.OK).body(result);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("NOK");
		}
	}

	@GetMapping(path = "/getRelays", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> getRelays() {
		Map<String, Object> map = new HashMap<String, Object>(1);
		try {
			RelayList list = fileService.getAllRelays();
			String json = fileService.getRelaysAsJsonString(list.getRelayList());
			map.put("relays", json);
			return ResponseEntity.status(HttpStatus.OK).body(map);
		} catch (Exception e) {
			// e.printStackTrace();
			map.put("errormsg", "Opvragen relays is mislukt");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
		}
	}

	@PostMapping(path = "/storeRelays", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> storeRelays(@RequestBody String json) {
		try {
			fileService.storeRelays(json);
			return ResponseEntity.status(HttpStatus.OK).body("OK");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("NOK");
		}
	}
}
