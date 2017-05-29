package modconsole;

import oscP5.OscMessage;
import oscP5.OscP5;
import controlP5.Bang;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.Knob;
import controlP5.Toggle;

/* controls for the war scene */

public class WarzoneControls extends PanelSet {

	String[] bangList = { 
			"StartWar", "BeamAttempt", "SpawnExit", "Shoot At Ship"
	};
	String[] bangMapping = {
			"/scene/warzone/warzonestart", "/system/transporter/startBeamAttempt", "/scene/warzone/spawnGate", "/scene/warzone/createBastard"
	};
	/* toggle buttons and their osc messages */
	String[] toggleList = { 
			"MissileLauncher", "RadarState"
	};
	String[] toggleMapping = {
			"/scene/warzone/missileLauncherStatus", "/scene/warzone/radarState"
	};

	String[] beamExcuses = { 
			"Beamed aboard, waiting for airlock..", "Beam blocked", "You broke in, prepare to shoot", "you were dumped out of airlock"
	};
	int currentExcuse = -1;

	long beamFailTime = 0;

	Knob missDiffKnob;
	int outstandingMissiles = 0;
	boolean missilesLaunched = false;
	int currentMissileDifficulty = 1;

	public WarzoneControls(String name, ModConsole parent, OscP5 p5, ControlP5 cp5) {
		super(name, parent, p5, cp5);
		sceneTag = "warzone-landing";
	}

	public void draw(ModConsole p) {
		p.textFont(p.getGlobalFont(), 12);

		p.text("Beam attempt status:", 140, 70 );
		if (currentExcuse!=-1) {
			p.fill(255, 255, 255);


			if (currentExcuse == 2) {
				p.text("SHOOT THE PLAYERS: " + (5 - (p.millis() - beamFailTime) / 1000), 340, 70);
			} 
			else {
				p.text(beamExcuses[currentExcuse], 340, 70);
			}
		}

		p.text("Missiles Launched?: " + missilesLaunched, 140, 100);
		p.text("missiles left: " + outstandingMissiles, 140, 120);
		p.text("missile difficulty:" + currentMissileDifficulty, 490,170);
	}

	public void reset() {
		currentExcuse = -1;

		beamFailTime = 0;
	}

	public void initGui() {

		//bang list
		for (int i = 0; i < bangList.length; i++) {
			cp5.addBang(bangList[i])
			.setPosition(140 + i * 75, 250)
			.setSize(50, 50)
			.setTriggerEvent(Bang.RELEASE)
			.setLabel(bangList[i])  
			.moveTo(sceneTag)   
			;
		}
		for (int i = 0; i < toggleList.length; i++) {
			// system toggles
			cp5.addToggle(toggleList[i])
			.setPosition(140 + i * 75, 340)
			.setSize(50, 20)
			.moveTo(sceneTag)
			;
		}

		cp5.addBang("SetMissileRate")
		.setPosition(430, 250)
		.setSize(50, 50)
		.setLabel("Set missile\r\nrate")
		.moveTo(sceneTag);

		missDiffKnob = cp5.addKnob("MissileRate")
				.setRange(1, 10)
				.setValue(1)
				.setPosition(500, 210)
				.setRadius(50)
				.setNumberOfTickMarks(10)
				.setTickMarkLength(1)
				.snapToTickMarks(true)
				.setColorForeground(parent.color(255))
				.setColorBackground(parent.color(0, 160, 100))
				.setColorActive(parent.color(255, 255, 0))
				.setDragDirection(Knob.HORIZONTAL)
				.moveTo(sceneTag)   
				;
	}


	void missDiffKnob(int theValue) {
		OscMessage msg = new OscMessage("/scene/warzone/missileRate");

		msg.add(theValue);
		oscP5.send(msg, parent.getNetAddress());
	}


	public void oscMessage(OscMessage theOscMessage) {
		if (theOscMessage.checkAddrPattern("/system/transporter/beamAttemptResult")) {
			int p = theOscMessage.get(0).intValue();
			currentExcuse = p;
			if (p == 2) {
				beamFailTime = parent.millis();
			}
		} 
		else if (theOscMessage.checkAddrPattern("/scene/warzone/missilelaunch") == true) {
			outstandingMissiles = theOscMessage.get(0).intValue();
			missilesLaunched = true;
		} 
		else if (theOscMessage.checkAddrPattern("/scene/warzone/missileOver") == true) {
			missilesLaunched = false;
		} 
		else if (theOscMessage.checkAddrPattern("/scene/warzone/flareResult") == true) {
			if ( theOscMessage.get(0).intValue() != 0) {
				outstandingMissiles -- ;
			}
		} 
		else if (theOscMessage.checkAddrPattern("/scene/warzone/missileResult") == true) {
			if (theOscMessage.get(0).intValue() == 1) {
				outstandingMissiles --;
			}
		} 
		else if (theOscMessage.checkAddrPattern("/scene/warzone/missileAttemptResult") == true) {
			if (theOscMessage.get(0).intValue() == 0) {
				outstandingMissiles --;
			}
		} else if (theOscMessage.checkAddrPattern("/scene/warzone/missileRate")){
			currentMissileDifficulty = theOscMessage.get(0).intValue();
		}
	}
	public void controlEvent(ControlEvent theControlEvent) {
		String name = theControlEvent.getName();

		/* do the toggle list first */
		try {
			Toggle t = (Toggle)theControlEvent.getController();

			for (int i = 0; i < toggleList.length; i++) {
				if (toggleList[i].equals(name)) {
					//get the state of the toggle
					int state = (int)theControlEvent.getValue();
					OscMessage msg = new OscMessage(toggleMapping[i]);

					msg.add(state);
					oscP5.send(msg, parent.getNetAddress());
				}
			}
		} 
		catch (ClassCastException e) {
		}

		/* do the bang list first */
		try {
			Bang t = (Bang)theControlEvent.getController();

			for (int i = 0; i < bangList.length; i++) {
				if (bangList[i].equals(name)) {
					//get the state of the toggle

					OscMessage msg = new OscMessage(bangMapping[i]);
					oscP5.send(msg, parent.getNetAddress());
				}
			} 

			if (name.equals("SetMissileRate")) {
				OscMessage msg = new OscMessage("/scene/warzone/missileRate");

				msg.add((int)missDiffKnob.value());
				oscP5.send(msg, parent.getNetAddress());
			}
		} 
		catch (ClassCastException e) {
		}
	}
}

