package engineer;

import java.awt.event.KeyEvent;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PImage;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;

public class JamDisplay extends Display {

	OscP5 p5;
	String serverIP = "";
	NetAddress myRemoteLocation;

	// assets
	PImage bgImage;
	PImage intruderOverlay;
	PImage jammingOverlay;

	// game data
	int STATE_SCAN = 0;
	int STATE_PLAYING = 1;
	int STATE_FAIL = 2;
	int STATE_OK = 3;

	int[][] graphData = new int[2][10]; // live graph data (drawn to screen)
	int[][] targetData = new int[2][10]; // where we are moving the graph bars
											// too
	int[][] prevData = new int[2][10]; // bar position at point of changing the
										// targets
	int[] target = new int[2]; // the target frequencies
	int gameState = STATE_SCAN;
	int scanStart = -5000;
	int playStart = -5000;
	int failStart = -2000;
	int lastChangeTime = 0;
	int newValueTime = 0; // time that we created new values for the graph, used
							// to smoothly change the graph
	int attempts = 3;

	int gameDuration = 35000;
	int nextChangeTime = 4500; // time between changes of the graph

	int dialA = 1;
	int dialB = 1;
	boolean jamAttempt = false;
	boolean jamSuccess = false; //
	int jamTimer = 0;
	boolean jamMessageDone = false;

	public JamDisplay(PlayerConsole parent) {
		super(parent);

		this.p5 = parent.getOscClient();
		serverIP = parent.getServerIP();
		myRemoteLocation = new NetAddress(serverIP, 12000);
		bgImage = parent.loadImage("engineerconsole/screens/jamming/background.png");
		intruderOverlay = parent
				.loadImage("engineerconsole/screens/jamming/intruderoverlay.png");
		jammingOverlay = parent.loadImage("engineerconsole/screens/jamming/jammingoverlay.png");
		for (int i = 0; i < 10; i++) {

			prevData[0][i] = 0;
			prevData[1][i] = 0;
		}
		resetPuzzle();
		scanStart = parent.millis();
	}

	@SuppressWarnings("static-access")
	@Override
	public void draw() {
		
		//removeme
		//playStart = parent.millis();
		// dialA =(int) map(mouseX, 0, width, 0, 12);
		// dialB = (int) map(mouseY, 0, height, 0, 12);
		
		int progress = (int) parent.map(parent.millis() - playStart, gameDuration,0f, 396, 0);
		parent.fill(0,128,10);
		parent.rect(873, 267, 120, progress);

		parent.image(bgImage, 0, 0, parent.width, parent.height);
		parent.textFont(font, 20);
		// text("test " + mouseX + ":" + mouseY, mouseX, mouseY);

		if (gameState == STATE_SCAN) {
				if(playStart + 1000 < parent.millis()){
					gameState = STATE_PLAYING;
					
					parent.getConsoleAudio().setToneState(true);
				}
			
		} else if (gameState == STATE_PLAYING) {
			if (lastChangeTime + nextChangeTime < parent.millis()) {
				newValues();
				lastChangeTime = parent.millis();
			}

			parent.getConsoleAudio().setToneValue(
					PApplet.map(dialA, 0, 12, 150, 220),
					PApplet.map(dialB, 0f, 12f, 0.1f, 15f));

			if (playStart + gameDuration < parent.millis()) {
				// failed, beam aboard
				gameState = STATE_FAIL;
				parent.getConsoleAudio().setToneState(false);

				failStart = parent.millis();
				// also sent a failure OSC message
				OscMessage msg = new OscMessage("/system/jammer/jamresult");
				msg.add(0);
				OscP5.flush(msg, myRemoteLocation);
				parent.getConsoleAudio().playClip("beamIn", -1.0f);
			}
			// draw the graphs

			parent.textFont(font, 10);
			parent.pushMatrix();
			parent.rotate(PApplet.radians(90));
			parent.translate(182,-505);
			for (int i = 0; i < 10; i++) {
				int graphHeightA = 0;
				int graphHeightB = 0;

				if (!jamAttempt) {

					if (newValueTime + 500 > parent.millis()) {
						graphData[0][i] = -(int) PApplet.lerp(prevData[0][i],
								targetData[0][i], PApplet.map(parent.millis()
										- newValueTime, 0, 500, 0f, 1.0f));
						graphData[1][i] = -(int) PApplet.lerp(prevData[1][i],
								targetData[1][i], PApplet.map(parent.millis()
										- newValueTime, 0, 500, 0f, 1.0f));

						graphHeightA = graphData[0][i]
								+ (int) PApplet.map(PApplet.sin((parent
										.millis() + i * 100) / 100.0f), -1.0f,
										1.0f, -3.0f, 3.0f);
						graphHeightB = graphData[1][i]
								+ (int) PApplet.map(PApplet.sin((parent
										.millis() + i * 100) / 100.0f), -1.0f,
										1.0f, -3.0f, 3.0f);
					} else {

						graphHeightA = -targetData[0][i]
								+ (int) PApplet.map(PApplet.sin((parent
										.millis() + i * 100) / 100.0f), -1.0f,
										1.0f, -3.0f, 3.0f);
						graphHeightB = -targetData[1][i]
								+ (int) PApplet.map(PApplet.sin((parent
										.millis() + i * 100) / 100.0f), -1.0f,
										1.0f, -3.0f, 3.0f);
					}
				} else {
					graphHeightA = -(int) parent.random(150);
					graphHeightB = -(int) parent.random(150);
				}
				parent.fill(0, 255, 0);
				parent.text(i + 1, 10 + 35 * i, 100);
				parent.text(i + 1, 10 + 35 * i, 80);

				int val = (int) parent.map(parent.sin(parent.frameCount * 0.8f), -1f, 1f, 0, 255);
				int col = parent.color(val, val, 0);
				if (dialA == i) {
					parent.fill(col);
				}
				parent.rect(35 * i, 50, 28, graphHeightA);
				

				parent.fill(0, 255, 0);
				if (dialB  == i) {
					parent.fill(col);
				}
				parent.noStroke();
				parent.rect( 35 * i, 120, 28, -graphHeightB);
			}

			parent.popMatrix();
			
			parent.fill(0, 255, 0);
			parent.textFont(font, 35);
			parent.text(dialA, 197, 653);
			parent.text(dialB, 606, 653);

			if (jamAttempt) {
				// overlay
				parent.fill(0, 0, 0, 128);
				parent.rect(0, 0, parent.width, parent.height);
				parent.image(jammingOverlay, 167, 251);
				jamTimer--;
				if (jamTimer <= 0) {
					jamAttempt = false;
					parent.getConsoleAudio().setToneState(true);

					if (jamSuccess) {
						// skip out were done here

						// parent.changeDisplay(0);
					}
				}
				if (jamTimer < 60) {

					// show the success/failure message
					if (jamSuccess) {
						parent.textFont(font, 45);
						parent.fill(0, 255, 0);
						parent.text("SUCCESS", 439, 426);
						if (!jamMessageDone) {
							parent.getConsoleAudio().playClip("codeOk");
							OscMessage msg = new OscMessage(
									"/system/jammer/jamresult");
							msg.add(1);
							OscP5.flush(msg, myRemoteLocation);
							jamMessageDone = true;
							// consoleAudio.setToneState(true);
							parent.getConsoleAudio()
									.playClip("beamFail", -1.0f);
						}
					} else {
						if (!jamMessageDone) {
							parent.getConsoleAudio().playClip("codeFail");
							jamMessageDone = true;
						}
						parent.textFont(font, 45);
						parent.fill(255, 0, 0);
						parent.text("FAILED", 439, 426);
					}
				}
			}
		} else if (gameState == STATE_FAIL) {
			parent.fill(0, 0, 0, 128);
			parent.rect(0, 0, parent.width, parent.height);
			parent.getConsoleAudio().setToneState(false);

			if (failStart + 2000 > parent.millis()) {

				parent.image(intruderOverlay, 170, 250);
			} else {
				// switch the display to the airlock subdisplay
				// parent.changeDisplay(2);
			}
		}
	}

	public void jamAttempt() {
		jamAttempt = true;
		jamMessageDone = false;

		parent.getConsoleAudio().setToneState(false);
		parent.getConsoleAudio().playClip("jamattempt");

		jamTimer = 120;
		if (dialA  == target[0] && dialB == target[1]) {
			jamSuccess = true;
		} else {
			jamSuccess = false;
		}
	}

	public void keyPressed() {
	}

	public void keyReleased() {
	}

	public void newValues() {
		target[0] = 1 + (int) parent.random(10);// (int)random(10);
		target[1] = 1 + (int) parent.random(10);

		for (int i = 0; i < 10; i++) {
			prevData[0][i] = targetData[0][i];
			prevData[1][i] = targetData[1][i];
			targetData[0][i] = 150 - (parent.abs(target[0] - i)) * 15;
			targetData[1][i] = 150 - (parent.abs(target[1] - i)) * 15;
		}
		newValueTime = parent.millis();
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {

		if (theOscMessage.checkAddrPattern("/system/jammer/setDifficulty")) {
			int d = theOscMessage.get(0).intValue();
			if (d >= 1 && d <= 10) {
				nextChangeTime = (7 - (1 + d / 2)) * 750;
			}
		}
	}

	public void resetPuzzle() {

		newValues();
		scanStart = -5000;
		playStart = parent.millis(); //-5000;
		failStart = -2000;
		gameState = STATE_SCAN;
		jamMessageDone = false;
		jamSuccess = false;
		jamAttempt = false;
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		
		

		// when the jam dials change and they change to the correct ones add a
		// little time to the
		// jamfrequency change timout, makes it a little easier
		if(evt.event.equals("JAMDIAL")){
			if (evt.id == 1) {
				dialA = (int) (Math.floor(PApplet.map(evt.value, 50, 1000, 0, 9)));
				dialA = PApplet.constrain(dialA, 0, 9);
				if (dialA == target[0] && dialB == target[1]) {
					lastChangeTime += 800; // give em an extra 800ms to whack the
											// button
				}
			} else if (evt.id == 0) {
				dialB = (int) (Math.floor(PApplet.map(evt.value, 50, 1000, 0, 9)));
				dialB = PApplet.constrain(dialB, 0, 9);

				if (dialA  == target[0] && dialB  == target[1]) {
					lastChangeTime += 800; // give em an extra 800ms to whack the
											// button
				}
			}
		
		} else if (evt.event.equals("KEY") && evt.value == 1) {
			if (evt.id == KeyEvent.VK_SEMICOLON) {
				jamAttempt();
			}
		} else if (evt.event.equals("NEWSWITCH")) {
			if(evt.id == 11 && evt.value == 1) jamAttempt();
		}
	}

	@Override
	public void start() {

		resetPuzzle();
	}

	@Override
	public void stop() {
		parent.getConsoleAudio().setToneState(false);
	}
}
