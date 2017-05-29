package modconsole;

import controlP5.Bang;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.Knob;
import controlP5.Numberbox;
import controlP5.Toggle;
import oscP5.OscMessage;
import oscP5.OscP5;

/* controls for the dead scene */


/* ADD A BUTTON TO CANCEL SD, as itwill need to trigger "all ok" message and turn off the infection thing */
public class NebulaControls extends PanelSet {

	String[] bangList = { 
			"Lightning\r\nStrike", "Blow Up Gate", "start\r\ndistress\r\nsignal", 
			"Reposition\r\nVan", "Spawn\r\nAnomaly"
	};
	String[] bangMapping = {
			"/scene/nebula/spawnLightning", "/scene/nebula/blowUpGate", "/scene/nebula/startPuzzle", 
			"/scene/nebula/repositionVan", "/scene/nebula/spawnAnomaly"
	};
	/* toggle buttons and their osc messages */
	String[] toggleList = { 
			"dummy"
	};
	String[] toggleMapping = {
			"/dummy"
	};

	boolean shipHasLeft = false;
	float sigStrength = 0.0f;

	public NebulaControls(String name, ModConsole parent, OscP5 p5, ControlP5 cp5) {
		super(name, parent, p5, cp5);
		sceneTag = "nebula";


		cp5.addNumberbox("Disk1")
		.setPosition(780,120)
		.setSize(100,14)
		.setScrollSensitivity(0.1f)
		.setValue(3)
		.setMax(20)
		.setMin(1)
		.moveTo(sceneTag)   
		;
		cp5.addNumberbox("Disk2")
		.setPosition(780,150)
		.setSize(100,14)
		.setScrollSensitivity(0.1f)
		.setValue(11)
		.setMax(20)
		.setMin(1)
		.moveTo(sceneTag)   
		;
		cp5.addNumberbox("Disk3")
		.setPosition(780,180)
		.setSize(100,14)
		.setScrollSensitivity(0.1f)
		.setValue(6)
		.setMax(20)
		.setMin(1)
		.moveTo(sceneTag)   
		;
		cp5.addBang("SetDisks")
		.setPosition(780, 220)
		.setSize(50, 20)
		.setTriggerEvent(Bang.RELEASE)
		.setLabel("Set Disks")
		.moveTo(sceneTag)        
		;

		cp5.addBang("InsertDisk")
		.setPosition(780, 260)
		.setSize(50, 20)
		.setTriggerEvent(Bang.RELEASE)
		.setLabel("Insert Disk")
		.moveTo(sceneTag)        
		;

	}

	public void draw(ModConsole p) {
		p.textFont(p.font,14);
		if(shipHasLeft){

			p.text("Dead ship has left the building\r\nCall the players and ask wtf", 440,116);
		}
		p.text ("sig strength = " + sigStrength, 440, 140);

	}

	public void initGui() {

		//bang list
		for (int i = 0; i < bangList.length; i++) {
			cp5.addBang(bangList[i])
			.setPosition(140 + i * 75, 250)
			.setSize(50, 50)
			.setTriggerEvent(Bang.RELEASE)
			.setLabel(bangList[i])  
			.moveTo(name)   
			;
		}
		for (int i = 0; i < toggleList.length; i++) {
			// system toggles
			cp5.addToggle(toggleList[i])
			.setPosition(140 + i * 75, 340)
			.setSize(50, 20)
			.moveTo(name)
			;
		}
	}

	public void reset(){
		shipHasLeft = false;
	}

	public void oscMessage(OscMessage msg) {


		if(msg.checkAddrPattern("/scene/nebulascene/shipHasLeft")){
			shipHasLeft = true;
		} else if(msg.checkAddrPattern("/clientscreen/TacticalStation/signalStrength")){
			sigStrength = msg.get(0).floatValue();
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

			if(name.equals("SetDisks")){

				//transmit the disk numbers for engineering
				OscMessage msg = new OscMessage("/system/boot/diskNumbers");
				msg.add( (int)(cp5.get(Numberbox.class,"Disk1").getValue()) );
				msg.add( (int)(cp5.get(Numberbox.class,"Disk2").getValue()) );
				msg.add( (int)(cp5.get(Numberbox.class,"Disk3").getValue()) );
				oscP5.send(msg, parent.getNetAddress());

			} else if(name.equals("InsertDisk")){
				OscMessage msg = new OscMessage("/scene/nebula/diskInsert");
				msg.add(1);
				oscP5.send(msg, parent.getNetAddress());
			}
		} 
		catch (ClassCastException e) {
		}

		try {
			Knob b = (Knob)theControlEvent.getController();


		}
		catch (ClassCastException e) {
		}
	}
}

