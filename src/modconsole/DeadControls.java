package modconsole;

import controlP5.Bang;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.Toggle;
import oscP5.OscMessage;
import oscP5.OscP5;

/* controls for the dead scene */

public class DeadControls extends PanelSet {

	String[] bangList = { "ResetGame" };
	String[] bangMapping = {"/game/reset"};
	/* toggle buttons and their osc messages */
	String[] toggleList = { "Dummy"};
	String[] toggleMapping = {"/Dummy/Dummy", };



	public DeadControls(String name, ModConsole parent, OscP5 p5, ControlP5 cp5){
		super(name, parent, p5, cp5);
		sceneTag = "deadscene";
	}

	public void draw(){

	}


	public void initGui(){

		//bang list
		for (int i = 0; i < bangList.length; i++){
			cp5.addBang(bangList[i])
			.setPosition(140 + i * 75, 250)
			.setSize(50, 50)
			.setTriggerEvent(Bang.RELEASE)
			.setLabel(bangList[i])  
			.moveTo(sceneTag)   
			;
		}
		for(int i = 0; i < toggleList.length; i++){
			// system toggles
			cp5.addToggle(toggleList[i])
			.setPosition(140 + i * 75,340)
			.setSize(50,20)
			.moveTo(sceneTag)
			;
		}


	}

	public void oscMessage(OscMessage msg){


	}
	public void controlEvent(ControlEvent theControlEvent) {
		String name = theControlEvent.getName();
		/* do the toggle list first */
		try {
			Toggle t = (Toggle)theControlEvent.getController();

			for(int i = 0; i < toggleList.length; i++){
				if(toggleList[i].equals(name)){
					//get the state of the toggle
					int state = (int)theControlEvent.getValue();
					OscMessage msg = new OscMessage(toggleMapping[i]);

					msg.add(state);
					oscP5.send(msg, parent.getNetAddress());

				}      
			} 
		} catch (ClassCastException e){}

		/* do the bang list first */
		try {
			Bang t = (Bang)theControlEvent.getController();

			for(int i = 0; i < bangList.length; i++){
				if(bangList[i].equals(name)){
					//get the state of the toggle

					OscMessage msg = new OscMessage(bangMapping[i]);
					oscP5.send(msg, parent.getNetAddress());

				}      
			} 
		} catch (ClassCastException e){}


	}

}
