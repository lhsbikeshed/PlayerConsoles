package common.displays;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PImage;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;

public class FailureScreen extends Display {

	PImage background;
	PImage warningImage;
	PImage leftIcon, rightIcon;

	int animTime = 0;
	float leftIconPos = 309f;
	float rightIconPos = 435f;

	public FailureScreen(PlayerConsole parent) {
		super(parent);
		background = parent.loadImage("common/failureScreen/bg.png");
		warningImage = parent.loadImage("common/failureScreen/warning.png");

		leftIcon = parent.loadImage("common/failureScreen/leftPart.png");
		rightIcon = parent.loadImage("common/failureScreen/rightPart.png");
	}

	@Override
	public void draw() {
		parent.image(background, 0, 0, parent.width, parent.height);
		animTime += 4;
		leftIconPos -= 0.2f;
		rightIconPos += 0.2f;
		
		int i;
	    float rot, rad, dist;
	    rot = PApplet.radians(animTime / 10.0f);

		parent.pushMatrix();
		parent.translate(leftIconPos + leftIcon.width, 328 + 328);
		parent.rotate(-rot);
		parent.translate(0, -328);
		for (i = 0; i < 5; i++) {
			if (parent.random(10) <= 7 )
				continue;
			dist = parent.random(50);
			parent.fill(255, 115 + dist * 2, 0);
			rad = 100 - dist / 2;
			parent.ellipse(-60 + dist, 50 + parent.random(-20, 20), rad, rad);
		}
		parent.image(leftIcon, -leftIcon.width, 0);
		parent.popMatrix();

		parent.pushMatrix();
		parent.translate(rightIconPos + 10, 229 + 229);
		parent.rotate(rot);
		parent.translate(0, -229);
		for (i = 0; i < 5; i++) {
			if (parent.random(10) <= 7)
				continue;
			dist = parent.random(50);
			parent.fill(255, 155 + dist * 2, 0);
			rad = 100 - dist / 2;
			parent.ellipse(40 + -dist, 180 + parent.random(-20, 20), rad, rad);
		}
		parent.image(rightIcon, -10, 0);
		parent.popMatrix();

		if (parent.globalBlinker) {
			parent.image(warningImage, 44, 28);
		}
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
	}

	@Override
	public void serialEvent(HardwareEvent content) {
	}

	@Override
	public void start() {
		animTime = 0;
		parent.getConsoleAudio().playClip("structuralFailure");
	}

	@Override
	public void stop() {
	}
}
