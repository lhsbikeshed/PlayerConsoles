package engineer;

import java.awt.event.KeyEvent;

import oscP5.OscMessage;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import engineer.reactorsim.ReactorModel;
import engineer.reactorsim.ReactorSystem;

public class NewPowerDisplay extends Display {

	ReactorModel reactorModel;
	
	public NewPowerDisplay(PlayerConsole parent) {
		super(parent);
		reactorModel = new ReactorModel();
		
	}

	@Override
	public void draw() {
		// TODO Auto-generated method stub
		reactorModel.tick();
		for(ReactorSystem sys : reactorModel.getSystems()){
			sys.draw(parent);
		}
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		// TODO Auto-generated method stub
		if(evt.event == "MOUSECLICK"){
			for(ReactorSystem sys : reactorModel.getSystems()){
				if(sys.hitTest(parent.mouseX, parent.mouseY)){
					ConsoleLogger.log(this, sys.getName() + " was clicked");
					
					sys.controlSignal(evt);
				}
			}
		} else if (evt.event == "KEY"){
			if(evt.id == KeyEvent.VK_SPACE){
				reactorModel.damageReactor((int)(Math.random()* 30f));
			}
		}

	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
