package modconsole;

import controlP5.ControlEvent;
import controlP5.ControlP5;
import oscP5.OscMessage;
import oscP5.OscP5;

/* controls for the hyper scene */

public class HyperControls extends PanelSet {

	int secondsUntilExit = 0;
	long exitTime = 0;
	boolean exiting = false;
	boolean failedExit = false;


	public HyperControls(String name, ModConsole parent, OscP5 p5, ControlP5 cp5){
		super(name, parent, p5, cp5);
		sceneTag = "hyper1";
	}

	public void draw(ModConsole p){

		p.textFont(p.font,20);
		if(exiting){

			p.text("State : Exiting!", 60,230);
			p.text("exiting in: " + (secondsUntilExit*1000 - (p.millis() - exitTime)), 60,245);
			if(failedExit){
				p.text("FAILED JUMP - WARN PLAYERS OF ROUGH RIDE AND DAMAGE", 60,260);
			}
		} else {
			p.text("State : In Jump", 60,230);
		}

	}

	public void reset(){
		exiting = false;
		secondsUntilExit = 0;
		failedExit = false;
		exitTime = 0;
	}


	public void initGui(){

		cp5.addTextlabel(name+"label")
		.setText("SCRIPT ----------------\r\n1. Prompt engineer to look at screen and follow instructions \r\n2. IF WARNING OCCURS tell them were bailing out of jump early and to expect a rough ride")
		.setPosition(12,50)
		.setColorValue(0xffffff00)
		.setFont(parent.createFont("Georgia",15))
		.moveTo(sceneTag)
		;

	}

	public void oscMessage(OscMessage msg){
		/*
      /warpscene/failjump    x = seconds until exit
      /warpscene/exitjump
		 */
		if(msg.checkAddrPattern("/scene/warp/failjump")){
			exiting = true;
			failedExit = true;
			secondsUntilExit = msg.get(0).intValue();
			exitTime = parent.millis();
		} else if (msg.checkAddrPattern("/warpscene/exitjump")){
			exiting = true;
			failedExit = false;
			exitTime = parent.millis();
			secondsUntilExit = msg.get(0).intValue();
		}


	}
	public void controlEvent(ControlEvent theControlEvent) {}

}
