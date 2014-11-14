package engineer.reactorsim;

import processing.core.PApplet;

import common.HardwareEvent;

public class FuelTankSystem extends ReactorSystem {

	public FuelTankSystem() {
		ReactorResource fuel = new ReactorResource();
		fuel.typeTag = "FUEL";
		fuel.maxAmount = 1500f;
		fuel.setAmount(1000f);
		resourceStore.put("FUEL", fuel);
		
	}

	@Override
	public void controlSignal(HardwareEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void draw(PApplet context) {
		// TODO Auto-generated method stub

	}

	@Override
	public void applyDamage(float amount) {
		// TODO Auto-generated method stub

	}

}
