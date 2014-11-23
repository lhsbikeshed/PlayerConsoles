package engineer.reactorsim;

import java.util.ArrayList;



import java.util.Hashtable;

import common.ConsoleLogger;
import common.HardwareEvent;
import engineer.powersystems.SubSystem;
import processing.core.PVector;

public class ReactorModel {

	
	float powerLevel = 100f;
	
	ArrayList<ReactorSystem> systems = new ArrayList<ReactorSystem>();
	
	Hashtable<String, ReactorSystem> switchToSystemMap = new Hashtable<String, ReactorSystem>(); // map switch numbers to subsystems
	PowerDistributionSystem powerSys;
	
	public ArrayList<ReactorSystem> getSystems() {
		return systems;
	}

	public ReactorModel() {
		CoolantValveSystem coolantValves = new CoolantValveSystem();
		coolantValves.setScreenPosition(new PVector(300,300));
		systems.add(coolantValves);
		
		CoolantMixerSystem coolantMixer = new CoolantMixerSystem();
		coolantMixer.setScreenPosition(new PVector(300, 380));
		systems.add(coolantMixer);
		
		coolantValves.addInboundConnection(coolantMixer);
		
		FuelTankSystem fuelTank = new FuelTankSystem();
		fuelTank.setScreenPosition(new PVector(460, 50));
		systems.add(fuelTank);
		
		//reactor vessel
		ReactorVesselSystem rvSystem = new ReactorVesselSystem();
		rvSystem.setScreenPosition(new PVector(460, 280));
		rvSystem.addInboundConnection(coolantValves);
		rvSystem.addInboundConnection(fuelTank);
		systems.add(rvSystem);
		
		TurbineSystem turbines = new TurbineSystem();
		turbines.setScreenPosition(new PVector(460, 600));
		turbines.addInboundConnection(rvSystem);
		systems.add(turbines);
		
	    powerSys = new PowerDistributionSystem();
		powerSys.setScreenPosition(new PVector(650, 600));
		powerSys.addInboundConnection(turbines);
		systems.add(powerSys);
	}
	
	//simulate one iteration of the reactor model
	public void tick(){
		
		for(ReactorSystem r : systems){
			r.tick();
		}
	
	}
	
	public void hardwareEvent(HardwareEvent e){
		String lookup = e.event + e.id;
		ReactorSystem sys = switchToSystemMap.get(lookup);
		if(sys != null){
			sys.controlSignal(e);
		}
	
		
	}
	
	public float getAvailablePower(){
		return ((PowerDistributionSystem) powerSys).getOutputPower();
		
	}
	
	public void repairReactor(float rate){
		
	}
	
	public void damageReactor(float amount){
		ConsoleLogger.log(this, "Damage to reactor: " + amount);
		int numberToDamage = (int)(Math.random() * amount * 0.2f);
		while (numberToDamage > 0){
			//pick a random system and apply damage
			int ind = (int) Math.floor(Math.random() * systems.size());
			ReactorSystem rand = systems.get(ind);
			ConsoleLogger.log(this, "damaged: " + rand.getClass().getName());
			rand.applyDamage(amount);
			numberToDamage--;
			
			
		}
	}
	
	
	
	
	
	
	

}
