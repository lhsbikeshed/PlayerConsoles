package engineer;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

import common.Display;
import common.PlayerConsole;

public class HyperSpaceDisplay extends Display {

	protected class Emitter {
		PVector pos;
		PVector size;
		public int id = 0;
		public char keyChar;

		public static final int STATE_OFF = 0;
		public static final int STATE_FAIL = 1;
		public static final int STATE_WRONG = 2;
		public static final int STATE_OK = 3;
		private long timerStart = 0;
		private int state = STATE_OFF;

		public Emitter() {
		}

		public void draw() {
			if (state == STATE_FAIL) {
				if (parent.globalBlinker) {
					parent.fill(0, 0, 255, 150);
				} else {
					parent.fill(0, 0, 128, 150);
				}
				parent.rect(pos.x, pos.y, size.x, size.y);
			} else if (state == STATE_WRONG) {

				parent.fill(255, 0, 0, 150);

				if (timerStart + 750 < parent.millis()) {
					state = STATE_OFF;
				}
				parent.rect(pos.x, pos.y, size.x, size.y);
			} else if (state == STATE_OK) {

				parent.fill(0, 255, 0, 150);

				if (timerStart + 750 < parent.millis()) {
					state = STATE_OFF;
				}
				parent.rect(pos.x, pos.y, size.x, size.y);
			}
		}

		public int getState() {
			return state;
		}

		public void setState(int state) {
			this.state = state;
			if (state == STATE_WRONG || state == STATE_OK) {
				timerStart = parent.millis();
			}
		}
	}

	// osc
	OscP5 p5;
	String serverIP = "";

	NetAddress myRemoteLocation;
	// state things
	boolean haveFailed = false; // have we failed/
	long failStart = 0; // when fail started

	long failDelay = 0;
	float timeRemaining = 0; // how long until exit

	int failsRemaining = 5;
	long nextFailTime = 5000;
	long lastFailTime = 0;

	int keypressesSinceFuckUp = 0;
	// assets
	PImage bgImage;
	PImage overlayImage;

	PImage warningBanner;

	char[] charMap = { 'a', 'f', 'k', 'p', 'b', 'q', 'c', 'r', 'd', 's', 'e',
			'j', 'o', 't' };

	// text labels

	Emitter[] emitters = new Emitter[14];

	int[][] keyMapping = new int[20][2]; /*
										 * { {97, 0, 0}, {98, 1, 0}, {99, 2, 0},
										 * {100, 3, 0},
										 */

	public HyperSpaceDisplay(PlayerConsole parent) {
		super(parent);
		this.p5 = parent.getOscClient();
		serverIP = parent.getServerIP();
		myRemoteLocation = new NetAddress(serverIP, 12000);

		// load assets
		bgImage = parent.loadImage("engineerconsole/hyperspace2.png");
		overlayImage = parent.loadImage("engineerconsole/hyperfailoverlay.png");
		warningBanner = parent.loadImage("engineerconsole/warpWarning.png");
		int idCt = 0;
		for (int y = 0; y < 5; y++) {
			for (int x = 0; x < 4; x++) {

				if (y == 0 || y == 4) {
					Emitter e = new Emitter();
					e.pos = new PVector(334 + x * 67 + x * 29, 220 + y * 67 + y
							* 27);
					e.size = new PVector(67, 67);
					e.id = idCt;
					e.keyChar = charMap[idCt];
					emitters[idCt] = e;
					idCt++;
				} else {
					if (x == 0 || x == 3) {
						Emitter e = new Emitter();
						e.pos = new PVector(334 + x * 67 + x * 29, 220 + y * 67
								+ y * 27);
						e.size = new PVector(67, 67);
						e.id = idCt;
						e.keyChar = charMap[idCt];
						emitters[idCt] = e;
						idCt++;
					}
				}
			}
		}
	}

	@Override
	public void draw() {

		parent.image(bgImage, 0, 0, parent.width, parent.height);
		if (haveFailed) {
			parent.image(overlayImage, 40, 200);
		} else {
			parent.fill(255, 255, 0);
			parent.textFont(font, 22);
			if (timeRemaining > 0.0f) {

				parent.text("Time Remaining: " + timeRemaining, 294, 700);
			} else {
				parent.text("EXITING HYPERSPACE", 294, 700);
			}
			int h = (failsRemaining * 20);
			if (h < 0) {
				h = 0;
			}
			parent.text("Hyperspace Tunnel Health: " + h + "%", 149, 740);

			if (lastFailTime + nextFailTime < parent.millis()) {
				lastFailTime = parent.millis();
				nextFailTime = (long) PApplet.map(keypressesSinceFuckUp, 0, 5,
						5000, 1000) + (long) parent.random(500);
				int r = (int) parent.random(14);
				emitters[r].state = Emitter.STATE_FAIL;
			}
			int deadCount = 0;
			for (Emitter e : emitters) {
				if (e.getState() == Emitter.STATE_FAIL) {
					deadCount++;
				}
				e.draw();
				if (e.getState() == Emitter.STATE_OK) {
					parent.stroke(15, 15, 255);
					parent.strokeWeight(4);
					parent.line(515, 437, e.pos.x + 35, e.pos.y + 35);
				}
			}

			if (deadCount > 5) {
				sendFailMessage();
				for (Emitter b : emitters) {
					b.state = Emitter.STATE_OFF;
				}
			}
			if (deadCount >= 3) {
				parent.image(warningBanner, 30, 218);
			}
		}

	}

	public void keyPressed() {
	}

	public void keyReleased() {
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {

		if (theOscMessage.checkAddrPattern("/scene/warp/updatestats") == true) {
			timeRemaining = (int) theOscMessage.get(1).floatValue();
			failsRemaining = (int) theOscMessage.get(0).floatValue();
		} else if (theOscMessage.checkAddrPattern("/scene/warp/failjump") == true) {
			haveFailed = true;
			failStart = parent.millis();
			failDelay = theOscMessage.get(0).intValue() * 1000;
		}
	}

	private void sendFailMessage() {
		OscMessage myMessage = new OscMessage("/scene/warp/warpfail");
		OscP5.flush(myMessage, myRemoteLocation);

		keypressesSinceFuckUp = 0;
	}

	private void sendOkMessage() {
		OscMessage myMessage = new OscMessage("/scene/warp/warpkeepalive");

		OscP5.flush(myMessage, myRemoteLocation);
	}

	@Override
	public void serialEvent(String evt) {
		String[] va = evt.split(":");
		if (va[0].equals("KEY")) {

			char c2 = va[1].charAt(0);
			if (c2 >= 'a' && c2 <= 't') {
				for (Emitter e : emitters) {
					if (e.keyChar == c2) {
						if (e.getState() == Emitter.STATE_FAIL) {
							e.setState(Emitter.STATE_OK);
							keypressesSinceFuckUp++;
							if (keypressesSinceFuckUp > 5) {
								keypressesSinceFuckUp = 5;
							}
						} else {
							for (Emitter b : emitters) {
								b.state = Emitter.STATE_OFF;
							}
							e.setState(Emitter.STATE_WRONG);
							keypressesSinceFuckUp = 0;
							sendFailMessage();
							return;
						}
					}
				}
			}
		}
	}

	@Override
	public void start() {
		haveFailed = false;
		failsRemaining = 5;
		for (Emitter e : emitters) {
			e.state = Emitter.STATE_OFF;
		}
		keypressesSinceFuckUp = 0;
	}

	@Override
	public void stop() {
	}
}
