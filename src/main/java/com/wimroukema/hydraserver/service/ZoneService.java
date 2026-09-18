package com.wimroukema.hydraserver.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.wimroukema.hydraserver.model.Relay;

public class ZoneService implements Runnable {

	private boolean stopProcess = true;
	private Relay relay;

	private RelayService relayService;

	public ZoneService(RelayService rs) {
		this.relayService = rs;
	}
	public void setStopProcess(boolean b) {
		System.out.println("stopped process "+b);
		stopProcess = b;
	}
	public boolean isProcessStopped() {
		return stopProcess;
	}
	public void setRelay(Relay relay) {
		this.relay = relay;
	}

	public void run() {
		while (!stopProcess) {
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
					e.printStackTrace();
					// stop the process
					stopProcess = true;
				}
			} catch (InterruptedException e) {
				//System.out.println("interrupted");
				//stopProcess = true;
			}
		}
	}
}
