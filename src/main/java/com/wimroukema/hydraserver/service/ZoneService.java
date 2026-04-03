package com.wimroukema.hydraserver.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.wimroukema.hydraserver.model.Relay;

public class ZoneService implements Runnable {

	private boolean stopProcess = false;
	private Relay relay;

	private RelayService relayService;

	public ZoneService(RelayService rs) {
		this.relayService = rs;
	}

	public void run() {
		while (!stopProcess) {
			relay = relayService.getActiveRelay();
			if (relay == null) {
				stopProcess = true;
				relayService.processEnded();
				return;
			}
			try {
				Thread.sleep(relay.getRun()*1000);
				relay.setActive(9);
				LocalDateTime ldt = LocalDateTime.now();
				relay.setStoppedOn(ldt.toEpochSecond(ZoneOffset.UTC));
				relay.setRunLeft(0);
				relayService.writeLogMessage(relay, false, true);
				try {
					relayService.setStartpoint();
				} catch (Exception e) {
					// stop the process
					stopProcess = true;
				}
			} catch (InterruptedException e) {
				stopProcess = true;
			}
		}
	}
}
