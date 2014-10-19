package engineer;

import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.plaf.SliderUI;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.ShipState;
import common.UsefulShit;
import engineer.powersystems.CoilSubSystem;
import engineer.powersystems.FuelFlowRateSystem;
import engineer.powersystems.ModeratorCoilSystem;
import engineer.powersystems.MultiValueSystem;
import engineer.powersystems.OnOffSystem;
import engineer.powersystems.SubSystem;

public class PowerDisplay extends Display {

	// osc
	OscP5 p5;
	String serverIP = "";
	// NetAddress myRemoteLocation;

	// assets
	PImage bgImage, hullStateImage, reactorFailOverlay, fuelLeakImage;

	// logic thingies
	//int[] power = new int[4];
	int lastReducedIndex = -1;
	float oxygenLevel = 100.0f;
	float hullState = 100.0f;
	float jumpCharge = 0.0f;
	boolean reactorFailWarn = false;
	boolean failureState = true;
	int lastFailureCount = 0;
	int difficulty = 1; // 1 - 10
	int maxReactorHealth = 2500;

	int[] powerColours = { UsefulShit.makeColor(255, 0, 0),
			UsefulShit.makeColor(255, 255, 0), UsefulShit.makeColor(0, 255, 0) };

	// subsystem stuff
	SubSystem[] subsystemList = new SubSystem[13]; // list of subsystem switches
	Hashtable<String, SubSystem> switchToSystemMap = new Hashtable<String, SubSystem>(); // map
																							// strings
																							// from
																							// serial
																							// port
																							// to
																							// subsystems
	ArrayList<int[]> systemGroupList = new ArrayList<int[]>(); // list of
																// subsystem
																// groups, if
																// all things in
																// a group are
																// failed then
																// we slowy
																// break the
																// reactor.
																// Indexes into
																// subsystemList
	int[] analogVals = new int[4]; // analog vals from arduino
	long lastFailureTime = 0;
	long nextFailureTime = 3000;
	int reactorHealth = maxReactorHealth;

	public PowerDisplay(PlayerConsole parent) {
		super(parent);

		this.p5 = parent.getOscClient();
		serverIP = parent.getServerIP();
		// myRemoteLocation = new NetAddress(sIP, 12000);
		bgImage = parent.loadImage("engineerconsole/powerman2.png");
		hullStateImage = parent
				.loadImage("engineerconsole/hulldamageoverlay.png");
		reactorFailOverlay = parent
				.loadImage("engineerconsole/reactorFailOverlay.png");
		fuelLeakImage = parent.loadImage("engineerconsole/leakIcon.png");

		// configure subsystems
		subsystemList[0] = new FuelFlowRateSystem(parent, "Deuterium",
				new PVector(10, 91),
				parent.loadImage("engineerconsole/icons/deuterium.png"));
		switchToSystemMap.put("NEWDIAL:2", subsystemList[0]);
		subsystemList[1] = new FuelFlowRateSystem(parent, "Tritium",
				new PVector(368, 91),
				parent.loadImage("engineerconsole/icons/tritium.png"));
		switchToSystemMap.put("NEWDIAL:3", subsystemList[1]);
		systemGroupList.add(new int[] { 0, 1 });

		subsystemList[2] = new ModeratorCoilSystem(parent, "Moderator Rod 1",
				new PVector(405, 172),
				parent.loadImage("engineerconsole/icons/mod1.png"));
		switchToSystemMap.put("NEWDIAL:0", subsystemList[2]);
		subsystemList[3] = new ModeratorCoilSystem(parent, "Moderator Rod 2",
				new PVector(396, 402),
				parent.loadImage("engineerconsole/icons/mod2.png"));
		switchToSystemMap.put("NEWDIAL:1", subsystemList[3]);
		systemGroupList.add(new int[] { 2, 3 });

		// field coils
		subsystemList[4] = new CoilSubSystem(parent, "Field Coil 1",
				new PVector(157, 205),
				parent.loadImage("engineerconsole/icons/coil1.png"));
		switchToSystemMap.put("NEWSWITCH:0", subsystemList[4]);
		subsystemList[5] = new CoilSubSystem(parent, "Field Coil 2",
				new PVector(437, 205),
				parent.loadImage("engineerconsole/icons/coil2.png"));
		switchToSystemMap.put("NEWSWITCH:2", subsystemList[5]);
		systemGroupList.add(new int[] { 4, 5 });

		// coolant valves
		subsystemList[6] = new OnOffSystem(parent, "Coolant Valve 1",
				new PVector(52, 350),
				parent.loadImage("engineerconsole/icons/cool1.png"));
		switchToSystemMap.put("NEWSWITCH:4", subsystemList[6]);
		subsystemList[7] = new OnOffSystem(parent, "Coolant Valve 2",
				new PVector(94, 350),
				parent.loadImage("engineerconsole/icons/cool2.png"));
		switchToSystemMap.put("NEWSWITCH:6", subsystemList[7]);
		subsystemList[8] = new OnOffSystem(parent, "Coolant Valve 3",
				new PVector(133, 350),
				parent.loadImage("engineerconsole/icons/cool3.png"));
		switchToSystemMap.put("NEWSWITCH:8", subsystemList[8]);
		systemGroupList.add(new int[] { 6, 7, 8 });
		// coolant mixer
		subsystemList[9] = new OnOffSystem(parent, "Coolant mixer",
				new PVector(73, 390),
				parent.loadImage("engineerconsole/icons/mixer.png"));
		switchToSystemMap.put("NEWSWITCH:10", subsystemList[9]);

		// power dist
		subsystemList[10] = new MultiValueSystem(parent, "Power Dist Route",
				new PVector(322, 617),
				parent.loadImage("engineerconsole/icons/powerdist.png"), 3);
		switchToSystemMap.put("NEWSWITCH:13", subsystemList[10]);

		// turbines
		subsystemList[11] = new OnOffSystem(parent, "Turbine #1", new PVector(
				324, 456),
				parent.loadImage("engineerconsole/icons/turbine.png"));
		switchToSystemMap.put("NEWSWITCH:1", subsystemList[11]);
		subsystemList[12] = new OnOffSystem(parent, "Turbine #2", new PVector(
				324, 525),
				parent.loadImage("engineerconsole/icons/turbine.png"));
		switchToSystemMap.put("NEWSWITCH:3", subsystemList[12]);
		systemGroupList.add(new int[] { 11, 12 });
	}

	private void addFailure() {

		// find a random system that isnt failed or broken
		SubSystem s = getRandomSystem(true, true);
		if (s != null) {
			s.createFailure();
		}
	}

	

	private int countSystemFailures() {
		int ct = 0;
		for (SubSystem s : subsystemList) {
			if (s.isFailed()) {
				ct++;
			}
		}
		return ct;
	}

	/*
	 * find a non damaged reactor element, if damaging it causes the entire
	 * group to be broken then dont as when the ship restarts it'll still be
	 * broken and kill the ship
	 */
	private void damageSomeShit() {

		SubSystem s = getRandomSystem(false, true);

		// if this is part of a group and would cause the group to entirely be
		// damaged then dont damage it
		// its unfair:p

		// find out which group were in
		int[] gList = null;
		for (int[] g : systemGroupList) {
			boolean found = false;
			for (int i = 0; i < g.length; i++) {
				if (subsystemList[g[i]] == s) {
					found = true;
					gList = g;
					break; // we found the group containing this
				}
			}
			if (found) {
				break;
			}
		}

		// we got a group, now lets see if failing this would cause the entire
		// group to be failed
		if (gList != null) {
			int brokenCount = 0;
			for (int i = 0; i < gList.length; i++) {
				if (subsystemList[gList[i]].isBroken()) {
					brokenCount++;
				}
			}
			// if(brokenCount + 1 < gList.length){
			s.smash();
			parent.getConsoleAudio().playClip("systemDamage");

			// } else {
			// println("cant break " + s.name);
			// }
		} else {
			// this isnt part of a group, smash it in!
			s.smash();
			parent.getConsoleAudio().playClip("systemDamage");
		}
	}

	@Override
	public void draw() {
		
		// check to see if we need to add a failure
		if (lastFailureTime + nextFailureTime < parent.millis() && failureState) {
			lastFailureTime = parent.millis();
			// nextFailureTime = 5000 + (long)random(3000);
			nextFailureTime = 2000 + (long) PApplet.map(difficulty, 1, 10,
					5000, 1000);

			addFailure();
		}
		if (lastFailureCount != countSystemFailures()) {
			lastFailureCount = countSystemFailures();
			updateFailCounts();
		}

		// calculate the reactor health changes
		int reactorDelta = 5;
		// find groups that have failed entirely, for each one lower the
		// reactordelta
		for (int[] sl : systemGroupList) {
			boolean groupFail = true;
			for (int i = 0; i < sl.length; i++) {
				groupFail &= (subsystemList[sl[i]].isFailed() | subsystemList[sl[i]]
						.isBroken());
				// groupFail &= subsystemList[ sl[i] ].isBroken();
			}
			if (groupFail) {
				reactorDelta -= 3;
			}
		}

		// does this addition take us under 20% reactor health?
		int threshHold = (int) (maxReactorHealth * 0.2f);
		if (reactorHealth > threshHold
				&& reactorHealth + reactorDelta <= threshHold) {
			reactorFailWarn = true;
			parent.getConsoleAudio().playClip("failWarning");
		} else if (reactorHealth > threshHold) {
			reactorFailWarn = false;
		}
		// finally change the actual reactor health
		reactorHealth += reactorDelta;
		if (reactorHealth >= maxReactorHealth) {
			reactorHealth = maxReactorHealth;
		}
		// fail if failed
		if (reactorHealth <= 0) {
			reactorHealth = 0;
			failReactor();
		}
		;

		// OK LETS DRAW SOME THINGS
		parent.noStroke();
		// draw a reactor pulsing
		int num = parent.height / 20;
		for (int i = 0; i < num; i++) {
			int c = (int) PApplet.map(
					PApplet.sin(parent.millis() / 200.0f - i / 2.0f), -1.0f,
					1.0f, 0, 255);
			if (reactorHealth > threshHold) {
				parent.fill(0, 0, c);
			} else {
				parent.fill(c, 0, 0);
			}
			parent.rect(0, i * 20, parent.width, 20);
		}

		// bg image
		parent.image(bgImage, 0, 0, parent.width, parent.height);
		// draw reactor health
		parent.fill(255);
		parent.textFont(font, 15);
		parent.text("REACTOR POWER", 267, 296);
		parent.text(
				(int) PApplet.map(reactorHealth, 0, maxReactorHealth, 0, 100),
				348, 310);

		// draw hull damage
		parent.tint((int) PApplet.map(hullState, 0, 100, 255, 0),
				(int) PApplet.map(hullState, 0, 100, 0, 255), 0);
		parent.image(hullStateImage, 747,487);
		parent.noTint();

		

		// bits o text
		parent.textFont(font, 15);
		parent.text((int) hullState + "%", 916,655);
		parent.textFont(font, 12);
		parent.fill((int) PApplet.map(oxygenLevel, 0, 100, 255, 0),
				(int) PApplet.map(oxygenLevel, 0, 100, 0, 255), 0);
		parent.text((int) oxygenLevel + "%", 793, 444);

		if (parent.getShipState().fuelLeaking) {
			if (parent.globalBlinker) {
				parent.tint(255, 0, 0);
			} else {
				parent.noTint();
			}
			parent.image(fuelLeakImage, 728,468);
		}

		// draw the subssystem icons
		int baseX = 625;
		int baseY = 85;
		parent.textFont(font, 12);
		for (SubSystem s : subsystemList) {
			// whilst were at it lets repair the systems if the power to
			// internal is on full
			float repairRate = PApplet.map(parent.getShipState().powerStates[ShipState.POWER_DAMAGE], 0f, 12f, 0.0025f, 0.5f);
			
			s.doRepairs(repairRate);
			
			
			s.draw();
			// if(s.isBroken()){
			// noTint();
			// fireSprite.draw((int)(s.pos.x +s.size.x / 2), (int)(s.pos.y +
			// s.size.y / 2), (int)s.size.x);
			// }
			// draw the instruction list in the top right
			// ignoring failed items that are now broken
			parent.textFont(font, 12);
			if (s.isFailed() && !s.isBroken()) {
				parent.fill(0, 255, 0);
				parent.text(s.getPuzzleString(), baseX, baseY);
				baseY += 20;
			}
		}

		if (parent.getShipState().powerStates[1] == 3) {
			parent.fill(255, 255, 255);
			parent.textFont(font, 15);
			parent.text("Repairing..", 196, 757);
		}

		if (reactorFailWarn && parent.globalBlinker) {
			parent.image(reactorFailOverlay, 207, 631);
		}
	}

	private void failReactor() {
		ConsoleLogger.log(this, "reactor FAILED ");
		OscMessage msg = new OscMessage("/system/reactor/fail");
		OscP5.flush(msg, new NetAddress(serverIP, 12000));
		for (SubSystem s : subsystemList) {
			s.reset();
		}
		((EngineerConsole) parent).probeEngPanel();
	}

	/* pick a random system with either failed or broken ones filtered out */
	private SubSystem getRandomSystem(boolean filterWrong, boolean filterBroken) {
		ArrayList<SubSystem> notFailedList = new ArrayList<SubSystem>();
		for (SubSystem s : subsystemList) {
			boolean filtered = false;
			if (filterWrong && s.isFailed()) {
				filtered = true;
			}
			if (filterBroken && s.isBroken()) {
				filtered = true;
			}

			if (!filtered) {
				notFailedList.add(s);
			}
		}

		int rand = PApplet.floor(parent.random(notFailedList.size()));
		if (rand >= 0 && rand < notFailedList.size()) {
			SubSystem s = notFailedList.get(rand);
			return s;
		} else {
			return null;
		}
	}

	public void keyPressed() {
	}

	public void keyReleased() {
	}

	public void mouseClick(int x, int y) {
		ConsoleLogger.log(this, "mx: " + x + " my: " + y);
		for (SubSystem s : subsystemList) {
			if (x > s.pos.x && x < s.pos.x + s.getImg().width) {
				if (y > s.pos.y && y < s.pos.y + s.getImg().height) {
					ConsoleLogger.log(this, s.name);
					s.toggleState();
					parent.getConsoleAudio().randomBeep();
					s.smash();
					break;
				}
			}

		}
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		if (theOscMessage.checkAddrPattern("/ship/stats") == true) {
			jumpCharge = theOscMessage.get(0).floatValue() * 100.0f;
			oxygenLevel = theOscMessage.get(1).floatValue();
			hullState = theOscMessage.get(2).floatValue();
		} else if (theOscMessage
				.checkAddrPattern("/system/powerManagement/failureState")) {
			boolean state = theOscMessage.get(0).intValue() == 1 ? true : false;
			failureState = state;
		} else if (theOscMessage
				.checkAddrPattern("/system/powerManagement/failureSpeed")) {
			difficulty = theOscMessage.get(0).intValue();
			ConsoleLogger.log(this, "engineer diff changed " + difficulty);
			for (SubSystem s : subsystemList) {
				s.setDifficulty(difficulty);
			}
		} else if (theOscMessage
				.checkAddrPattern("/system/reactor/stateUpdate")) { // qhen
																	// reactor
																	// starts
																	// set the
																	// health
																	// back to
																	// 1000

			int state = theOscMessage.get(0).intValue();
			if (state == 0) {
				//
				reactorHealth = 0;
			} else {
				reactorHealth = maxReactorHealth;
			}
		} 
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		if (evt.event.equals("NEWDIAL")) {
			String lookup = "NEWDIAL:" + evt.id;

			SubSystem s = switchToSystemMap.get(lookup);
			if (s != null) {
				s.setState(evt.value);
			}
		} else if (evt.event.equals("NEWSWITCH")) {

			String lookup = "NEWSWITCH:" + evt.id;

			SubSystem s = switchToSystemMap.get(lookup);
			if (s != null) {
				s.setState(evt.value);
				// make a beep if the system isnt currently broken
				if (!s.isBroken()) {
					parent.getConsoleAudio().randomBeep();
				}
			}
		}
	}

	public void shipDamaged(float damage) {
		if (damage >= 9.0f) {
			damageSomeShit();
		}
	}

	/*
	 * on screen start: reset all of the subsystems probe the panel hardware for
	 * current switch states reset timers for failure and healht
	 */
	@Override
	public void start() {
		reset();
		
	}

	public void reset(){
		for (SubSystem s : subsystemList) {
			s.reset();
		}

		((EngineerConsole) parent).probeEngPanel();
		lastFailureTime = parent.millis();
		reactorFailWarn = false;
		reactorHealth = maxReactorHealth;
	}
	
	@Override
	public void stop() {
	}

	void updateFailCounts() {
		// update everyone with the current fail count
		OscMessage msg = new OscMessage("/system/powerManagement/failureCount");
		msg.add(countSystemFailures());
		msg.add(subsystemList.length);
		OscP5.flush(msg, new NetAddress(serverIP, 12000));
	}
}
