package engineer.powersystems;

import processing.core.PImage;
import processing.core.PVector;

import common.PlayerConsole;

import engineer.EngineerConsole;
import engineer.Highlighter;

/* base storage class for subsystems to be drawn to screen*/
public abstract class SubSystem {
	public PVector pos;
	public PVector size;
	public String name;

	protected String[] stateNames;
	protected int currentState = 0; // current "value" of this system (i.e. flow
									// rate or coil field mode
	protected int targetState = 0; // target for the puzzle

	protected boolean isBlinking = false;
	protected long blinkStart = 0;
	private PImage img;
	// protected boolean isBroken = false;
	protected int maxHealth = 100;
	protected int health = maxHealth;
	protected int failedFor = 0;
	protected int difficulty = 1;

	protected boolean firstValueSet = true;
	protected PlayerConsole parent;

	public SubSystem(PlayerConsole parent, String name, PVector pos, PImage p) {
		this.parent = parent;
		setImg(p);
		this.name = name;
		this.pos = pos;
		this.size = new PVector(p.width, p.height);
	}

	public abstract void createFailure(); // make this system fail, pass in
											// difficulty

	public void doRepairs() {
		// deal with repairs
		if (health < maxHealth) {
			health += parent.random(3);
			if (health >= maxHealth) {
				health = maxHealth;
				failedFor = 0;
			}
		}
	}

	public void draw() {

		if (isBroken()) {
			parent.tint(100, 100, 100);
			parent.image(getImg(), pos.x + parent.random(4) - 2,
					pos.y + parent.random(4) - 2);
		} else {

			if (currentState != targetState) {
				if (parent.globalBlinker) {
					parent.tint(255, 0, 0);
				} else {
					parent.tint(255, 255, 0);
				}
				failedFor++;
				if (failedFor > 400 + (10 - difficulty) * 50) {
					smash();
					failedFor = 0;
				}
			} else {
				failedFor = 0;
				parent.tint(0, 255, 0);
			}
			parent.image(getImg(), pos.x, pos.y);
		}

		parent.noTint();
	}

	public PImage getImg() {
		return img;
	}

	public abstract String getPuzzleString(); // get the instruction that the
												// user sees for this system

	public String getStatusString() { // the state that is drawn to screen i.e.
										// "A" or "300/sec"
		return stateNames[currentState];
	}

	public boolean isBroken() {
		return health <= 10;
	}

	public boolean isFailed() {
		return currentState != targetState;
	}

	public void reset() {
		firstValueSet = true;
		currentState = targetState;
		failedFor = 0;
		// health = maxHealth;
	}

	public void setDifficulty(int i) {
		difficulty = i;
	}

	public void setImg(PImage img) {
		this.img = img;
	}

	public void setState(int state) {
		if (isBroken()) {
			return;
		}
		currentState = state;
		if (firstValueSet == true) {
			firstValueSet = false;
			targetState = currentState;
		}
	}

	public void setStateNames(String[] names) {
		stateNames = names;
	}

	public void smash() {
		if (parent.getConsoleAudio().clipExists(name + "-dead")) {
			parent.getConsoleAudio().playClip(name + "-dead");
		} else {
			parent.getConsoleAudio().playClip("systemDamage");
		}
		((EngineerConsole) parent).addHighlight(new Highlighter(parent,
				new PVector(pos.x + getImg().width / 2, pos.y + getImg().height
						/ 2), new PVector(getImg().width * 5,
						getImg().height * 5), new PVector(getImg().width,
						getImg().height), 1000, 2000));
		health = 0;
		// isBroken = true;
		createFailure();
	}

	public void toggleState() {
		if (isBroken()) {
			return;
		}
		setState(1 - currentState);
	}
}