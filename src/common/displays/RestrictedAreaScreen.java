package common.displays;

import oscP5.OscMessage;
import processing.core.PImage;

import common.Display;
import common.PlayerConsole;

public class RestrictedAreaScreen extends Display {

	PImage bgImage;

	public RestrictedAreaScreen(PlayerConsole parent) {
		super(parent);
		bgImage = parent.loadImage("common/RestrictedAreaScreen/bg.png");
	}

	@Override
	public void draw() {

		// signalStrength = map(mouseY, 0, height, 0, 1.0f);
		parent.background(0, 0, 0);
		parent.image(bgImage, 0, 0, parent.width, parent.height);

	}

	public void keyPressed() {
	}

	public void keyReleased() {
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {

	}

	@Override
	public void serialEvent(String evt) {
	}

	@Override
	public void start() {
		parent.getConsoleAudio().playClip("structuralFailure");
	}

	@Override
	public void stop() {
	}

}
