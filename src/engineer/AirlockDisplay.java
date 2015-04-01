package engineer;

import java.awt.event.KeyEvent;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PImage;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;

public class AirlockDisplay extends Display {

	// failure text @ 315:544
	/*
	 * 325:396 473:391 620:391 768:389
	 */

	PImage bgImage;
	PImage dumpOverlay;
	PImage failedDumpOverlay;

	OscP5 p5;
	String serverIP = "";
	NetAddress myRemoteLocation;

	String[] possibleAuthCodes = { "12345", "62918", "26192" };

	// game state
	boolean locked = true;
	boolean failedCode = false;
	boolean greatSuccess = false;
	boolean doneSuccessMessage = false;
	String stateText = "DOOR LOCKED";
	int failTime = 0;
	int successTime = 0;

	String authCode = "";

	String doorCode = "12345";
	long puzzleStartTime = 0;
	boolean totalFailure = false;

	public AirlockDisplay(PlayerConsole parent) {
		super(parent);
		this.p5 = parent.getOscClient();
		serverIP = parent.getServerIP();
		myRemoteLocation = new NetAddress(serverIP, 12000);
		bgImage = parent.loadImage("engineerconsole/airlockscreen.png");
		dumpOverlay = parent.loadImage("engineerconsole/airlockoverlay.png");
		failedDumpOverlay = parent
				.loadImage("engineerconsole/airlockFailedOverlay.png");
		authCode = "";
	}

	@Override
	public void draw() {
		parent.image(bgImage, 0, 0, parent.width, parent.height);

		parent.textFont(font, 50);

		parent.fill(0, 255, 0);
		for (int i = 0; i < authCode.length(); i++) {
			parent.text("" + authCode.charAt(i), 170 + i * 150, 390);
		}

		parent.textFont(font, 50);
		if (locked) {
			parent.fill(255, 0, 0);
		} else {
			parent.fill(0, 255, 0);
		}
		parent.text(stateText, 207, 544);

		if (failedCode) {
			if (failTime + 1500 < parent.millis()) {
				failedCode = false;
				authCode = "";
				stateText = "DOOR LOCKED";
			}
		}

		if (locked == false) {
			parent.image(dumpOverlay, 160, 232);
		}

		if (puzzleStartTime + 25000 < parent.millis() && !greatSuccess) {
			// we failed, show that the intruder has disabled the airlock dump
			parent.image(failedDumpOverlay, 160, 232);
			if (totalFailure == false) {
				totalFailure = true;
				OscMessage msg = new OscMessage(
						"/system/transporter/beamAttemptResult");
				msg.add(2);
				p5.send(msg, myRemoteLocation);
			}
		}

		if (greatSuccess) {
			if (!doneSuccessMessage) {
				doneSuccessMessage = true;
				// turn off airlock dump light
				setAirlockLightState(false);
			}

		}

	}

	public void keyEntered(int k) {
		char c = (char) k;

		if (locked) {

			if (c == KeyEvent.VK_BACK_SPACE) {
				if (authCode.length() > 0) {

					authCode = authCode.substring(0, authCode.length() - 1);
				}

			} else if (c == KeyEvent.VK_ENTER || authCode.length() >= 5) {
				// authCode +=c;
				if (authCode.equals(doorCode)) {
					failedCode = false;
					locked = false;
					stateText = "CODE OK";
					// turn on the airlock dump light
					setAirlockLightState(true);
					parent.getConsoleAudio().playClip("codeOk");
				} else {
					ConsoleLogger.log(this, "failed code");
					parent.getConsoleAudio().playClip("codeFail");
					authCode = "";
				}
			} else if (c >= KeyEvent.VK_NUMPAD0 && c <= KeyEvent.VK_NUMPAD9) {
				authCode += (c - KeyEvent.VK_NUMPAD0);

			}
		}
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		if (evt.event.equals("KEY") && evt.value == 1) {

			if (evt.id >= KeyEvent.VK_NUMPAD0 && evt.id <= KeyEvent.VK_NUMPAD9
					|| evt.id == KeyEvent.VK_ENTER
					|| evt.id == KeyEvent.VK_BACK_SPACE) {
				// char c = (char)evt.value;

				keyEntered(evt.id);

			} else if (evt.id == KeyEvent.VK_L) {

				dumpAirlock();
			}
		}

		if (evt.event.equals("BUTTON")) {
			if (evt.id == UpperPanelHardware.BT_AIRLOCK && locked == false) {
				dumpAirlock();
			}
		}

	}

	private void dumpAirlock() {
		greatSuccess = true;
		successTime = parent.millis();
		if (!doneSuccessMessage) {

			OscMessage msg = new OscMessage(
					"/system/transporter/beamAttemptResult");
			msg.add(3);
			p5.send(msg, myRemoteLocation);
			parent.getConsoleAudio().playClip("airlockDump", -1.0f);

		}

	}

	void setAirlockLightState(boolean state) {
		ConsoleLogger.log(this, "setting airlock light to : " + state);
		OscMessage msg = new OscMessage("/system/effect/airlockLight");
		msg.add(state == true ? 1 : 0);
		p5.send(msg, myRemoteLocation);
	}

	@Override
	public void start() {
		puzzleStartTime = parent.millis();
		failedCode = false;
		totalFailure = false;
		locked = true;
		authCode = "";
		greatSuccess = false;
		successTime = 0;
		doneSuccessMessage = false;
		stateText = "DOOR LOCKED";

		doorCode = possibleAuthCodes[0];

	}

	@Override
	public void stop() {
	}
}
