package engineer.powersystems;

import processing.core.PImage;
import processing.core.PVector;

import common.PlayerConsole;

public class CoilSubSystem extends SubSystem {

	public CoilSubSystem(PlayerConsole parent, String name, PVector pos,
			PImage p) {
		super(parent, name, pos, p);
		maxHealth = 90;
		health = 90;
	}

	@Override
	public void createFailure() {
		targetState = 1 - currentState;
	}

	@Override
	public void draw() {
		super.draw();
		if (isBroken()) {
			parent.tint(100, 100, 100);
		} else {
			if (isFailed()) {
				parent.fill(255, 0, 0);
			} else {
				parent.fill(0, 255, 0);
			}
			if (currentState == 0) {
				parent.textFont(parent.getGlobalFont(), 25);
				parent.text("A", pos.x + 50, pos.y + 65);
			} else {
				parent.textFont(parent.getGlobalFont(), 25);
				parent.text("B", pos.x + 50, pos.y + 65);
			}
		}
	}

	@Override
	public String getPuzzleString() {
		if (targetState == 1) {
			return "Set " + name + " to B";
		} else {
			return "Set " + name + " to A";
		}
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