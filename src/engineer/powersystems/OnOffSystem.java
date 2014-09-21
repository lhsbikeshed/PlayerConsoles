package engineer.powersystems;

import processing.core.PImage;
import processing.core.PVector;

import common.PlayerConsole;

public class OnOffSystem extends SubSystem {

	public OnOffSystem(PlayerConsole parent, String name, PVector pos, PImage p) {
		super(parent, name, pos, p);
		maxHealth = 40;
		health = maxHealth;
	}

	@Override
	public void createFailure() {
		targetState = 1 - currentState;
	}

	@Override
	public String getPuzzleString() {
		if (targetState == 0) {
			return "Turn " + name + " on";
		} else {
			return "Turn " + name + " off";
		}
	}
}