package engineer.powersystems;

import processing.core.PImage;
import processing.core.PVector;

import common.PlayerConsole;

public class FuelFlowRateSystem extends SubSystem {
	String fuelType = "";
	boolean targetBelowInitial = false;

	public FuelFlowRateSystem(PlayerConsole parent, String name, PVector pos,
			PImage p) {
		super(parent, name, pos, p);
		maxHealth = 60;
		health = 60;
	}

	@Override
	public void createFailure() {
		int randAmt = ((difficulty * 20) / 2) + (50 - (int) parent.random(100));
		randAmt = randAmt - (randAmt / 2);
		int newState = currentState + randAmt;
		if (newState < 0) {
			newState = 0;
		}
		if (newState > 999) {
			newState = 999;
		}

		if (newState < currentState) {
			targetBelowInitial = true;
		} else {
			targetBelowInitial = false;
		}
		targetState = newState;
	}

	@Override
	public void draw() {

		if (isBroken()) {
			parent.tint(100, 100, 100);
		} else {

			if (isFailed()) {
				failedFor++;
				if (failedFor > 400 + (10 - difficulty) * 50) {
					smash();
					failedFor = 0;
				}
				if (parent.globalBlinker) {
					parent.tint(255, 0, 0);
				} else {
					parent.tint(255, 255, 0);
				}
			} else {
				failedFor = 0;
				parent.tint(0, 255, 0);
			}
		}
		parent.image(getImg(), pos.x, pos.y);
		parent.noTint();
		if (isFailed()) {
			parent.fill(255, 0, 0);
		} else {
			parent.fill(0, 255, 0);
		}
		parent.text(getStatusString(), pos.x + 137, pos.y + 46);
	}

	@Override
	public String getPuzzleString() {

		return "Set " + name + " flow close to " + targetState + "mg/sec";
	}

	@Override
	public String getStatusString() {
		return currentState + "m";
	}

	@Override
	public boolean isFailed() {

		return !((currentState - 50 < targetState) && (currentState + 50 > targetState));
	}

	@Override
	public void toggleState() {
		if (isBroken()) {
			return;
		}
		if (isFailed()) {
			currentState = targetState;
		}
	}
}