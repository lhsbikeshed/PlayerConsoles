package engineer.powersystems;

import processing.core.PImage;
import processing.core.PVector;

import common.PlayerConsole;

public class ModeratorCoilSystem extends SubSystem {
	String fuelType = "";
	boolean targetBelowInitial = false;

	public ModeratorCoilSystem(PlayerConsole parent, String name, PVector pos,
			PImage p) {
		super(parent, name, pos, p);
		maxHealth = 70;
		health = 70;
	}

	@Override
	public void createFailure() {
		int randAmt = ((difficulty * 20) / 2) + (50 - (int) parent.random(100));
		randAmt = randAmt - (randAmt / 2); // offset it back by half to give
											// -250 -> 250
		int newState = currentState + randAmt;
		if (newState < 0) {
			newState = 0;
		}
		if (newState > 999) {
			newState = 999;
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
		parent.text(getStatusString(), pos.x + 67, pos.y + 23);
	}

	@Override
	public String getPuzzleString() {

		return "Set " + name + " to " + (targetState / 10.0f) + "%";
	}

	@Override
	public String getStatusString() {
		return (currentState / 10.0f) + "%";
	}

	@Override
	public boolean isFailed() {
		return !((currentState - 100 < targetState) && (currentState + 100 > targetState));
	}

	@Override
	public void setState(int state) {
		if (isBroken()) {
			return;
		}
		super.setState(1000 - state);
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