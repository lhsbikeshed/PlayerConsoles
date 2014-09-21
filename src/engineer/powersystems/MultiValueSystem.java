package engineer.powersystems;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

import common.PlayerConsole;

public class MultiValueSystem extends SubSystem {

	int maxVals = 1;

	public MultiValueSystem(PlayerConsole parent, String name, PVector pos,
			PImage p, int maxVals) {
		super(parent, name, pos, p);
		this.maxVals = maxVals;
		maxHealth = 120;
		health = maxHealth;
	}

	@Override
	public void createFailure() {
		int ra = PApplet.floor(parent.random(maxVals));
		while (ra != targetState) {
			targetState = ra;
			ra = PApplet.floor(parent.random(maxVals));
		}
	}

	@Override
	public String getPuzzleString() {
		return "Set " + name + " to " + (targetState + 1);
	}

	@Override
	public void toggleState() {
		if (isBroken()) {
			return;
		}
		currentState++;
		currentState %= maxVals;
	}
}