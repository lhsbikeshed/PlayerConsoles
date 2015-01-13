package engineer;

import java.awt.event.KeyEvent;

import oscP5.OscMessage;
import processing.core.PApplet;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.ShipState;
import engineer.reactorsim.ReactorManager;
import engineer.reactorsim.ReactorManager.ReactorCheck;
import engineer.reactorsim.ReactorModel;
import engineer.reactorsim.ReactorSystem;

public class NewPowerDisplay extends Display {

	ReactorModel reactorModel;
	ReactorManager reactorManager;
	float power = 0f;
	float reqPower = 0f;
	
	public NewPowerDisplay(PlayerConsole parent) {
		super(parent);
		reactorModel = new ReactorModel();
		reactorManager = new ReactorManager(reactorModel);
	}

	@Override
	public void draw() {
		// TODO Auto-generated method stub
		reactorModel.tick();
		for(ReactorSystem sys : reactorModel.getSystems()){
			sys.draw(parent);
		}
		
		reactorManager.tick();
		//get the reactor manager failure list and draw it
		
		
		parent.noFill();
		parent.rect(950, 750, 30, -650);
		
		
		power = PApplet.lerp(power, reactorModel.getAvailablePower(), .5f);
		
		float amt = PApplet.map(power, 0, 500, 0, -650);
		float c = PApplet.map(power, 0, 500, 0, 255);
		parent.fill(255 - c, c,0);
		parent.rect(950, 750, 30, amt);
		parent.text("Available power " + (int)power, 810, 755 + amt);
		
		reqPower = PApplet.lerp(reqPower, reactorModel.getRequiredPower(), .5f);
		amt = PApplet.map(reqPower, 0, 500, 0, -650);
		parent.text("Required power " + (int)reqPower, 810, 755 + amt);
		
		int ct = 100;
		for(ReactorCheck p : reactorManager.getProblemList()){
			if(p.getMessage() != null){
				parent.text(p.getMessage(), 20, ct);
				ct += 20;
			}
		}
		
		//for reference draw the current power levels
		parent.text("ENGINES: " + ShipState.instance.powerStates[0], 50, 500);
		parent.text("DAMAGECON: " + ShipState.instance.powerStates[1], 50, 520);
		parent.text("SENSORS: " + ShipState.instance.powerStates[2], 50, 540);
		parent.text("WEAPONS: " + ShipState.instance.powerStates[3], 50, 560);
		
		
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
