package tactical;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PImage;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;

/**
 * during the drop scene show warning that systems are offline show references
 * to flight manual for patching emergency jump power in
 */

public class DropDisplay extends Display {

	PImage bg;
	PImage repairedBg;
	PImage structFailOverlay;
	PImage offlineBlinker;
	PImage damagedIcon;
	

	boolean fixed = false;
	boolean structFail = false;
	boolean jumpCharged = false;

	int curStep = -1;

	public DropDisplay(PlayerConsole parent) {
		super(parent);
		bg = parent.loadImage("tacticalconsole/dropBackground.png");

		repairedBg = parent.loadImage("tacticalconsole/dropscenefixed.png");
		structFailOverlay = parent
				.loadImage("tacticalconsole/structuralFailure.png");
		damagedIcon = parent.loadImage("tacticalconsole/dropDamage.png");
		offlineBlinker = parent.loadImage("tacticalconsole/dropOffline.png");

	}

	@Override
	public void draw() {
		parent.background(0);
		parent.fill(255, 255, 255);
		parent.image(bg, 0, 0, parent.width, parent.height);
		parent.fill(0);
		parent.noStroke();
		if(parent.globalBlinker){
			parent.rect(179,396, 664, 62);
		}

		if (structFail) { // show the "structural failure" warning

			parent.image(structFailOverlay, 128, 200);
		}
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		// println(theOscMessage);
		if (theOscMessage.checkAddrPattern("/scene/drop/structuralFailure") == true) {
			structFail = true;
		} else if (theOscMessage.checkAddrPattern("/ship/jumpStatus") == true) {
			int v = theOscMessage.get(0).intValue();
			if (v == 0) {
				jumpCharged = false;
			} else if (v == 1) {
				jumpCharged = true;
			}
		}
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		//String[] evtData = evt.split(":");

		if (evt.event.equals("CONDUIT")) {
			int cable = evt.id;
			int state = evt.value;
			
			OscMessage msg;
			if(state == 1){
				msg = new OscMessage("/scene/drop/conduitConnect");
				
			} else {
				msg = new OscMessage("/scene/drop/conduitDisconnect");
			}
			msg.add(cable);
			parent.getOscClient().send(msg, parent.getServerAddress());
		}

	}

	@Override
	public void start() {
		fixed = false;
		structFail = false;
		jumpCharged = false;
		// probe for current cable state
		TacticalHardwareController.instance.probeCableState();
	}

	@Override
	public void stop() {
	}
}
