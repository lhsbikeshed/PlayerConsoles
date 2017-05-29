package modconsole;

import controlP5.Bang;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import oscP5.OscMessage;
import oscP5.OscP5;

/* controls for the drop scene */

public class PreloadControls extends PanelSet {


	String[] bangList = { "Start"};
	String[] bangMapping = {"/scene/preload/start"};



	public PreloadControls(String name, ModConsole parent, OscP5 p5, ControlP5 cp5){
		super(name, parent, p5, cp5);
		sceneTag = "preload";
	}

	public void reset(){

	}

	public void draw(ModConsole p){

		p.textFont(p.getGlobalFont(), 15);



	}


	public void initGui(){

		//bang list
		for (int i = 0; i < bangList.length; i++){
			cp5.addBang(bangList[i])
			.setPosition(440 + i * 75, 300)
			.setSize(50, 50)
			.setTriggerEvent(Bang.RELEASE)
			.setLabel(bangList[i])  
			.moveTo(sceneTag)

			;
		}

	}

	public void oscMessage(OscMessage msg){



	}

	public void controlEvent(ControlEvent theControlEvent) {
		/* do the bang list first */
		String name = theControlEvent.getName();

		try {
			Bang t = (Bang)theControlEvent.getController();

			for(int i = 0; i < bangList.length; i++){
				if(bangList[i].equals(name)){          
					OscMessage msg = new OscMessage(bangMapping[i]);
					oscP5.send(msg, parent.getNetAddress());

				}      
			} 



		} catch (ClassCastException e){}


	}

}
