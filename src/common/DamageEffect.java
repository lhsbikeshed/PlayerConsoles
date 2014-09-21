package common;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PVector;

public class DamageEffect {
	private class CrackItem {
		public int crackId = 0;
		public int rotation = 0;
		public PVector screenPosition = new PVector(0, 0);
		public float scale = 1f;

	}

	// time we last got damaged
	long damageTimer = -1000;

	PImage noiseImage; // static image that flashes

	boolean running = false;
	int tileX = 5;

	int tileY = 5;
	ArrayList<CrackItem> crackList = new ArrayList<CrackItem>();
	PImage[] crackImages;

	int maxCracks = 3;

	PApplet parent;

	public DamageEffect(PApplet parent) {
		this.parent = parent;
		ConsoleLogger.log(this, "generating damage images...");
		noiseImage = parent.createImage(parent.width / tileX, parent.height
				/ tileY, PConstants.RGB);
		noiseImage.loadPixels();
		for (int i = 0; i < noiseImage.width * noiseImage.height; i++) {
			noiseImage.pixels[i] = parent.color(parent.random(255));
		}
		noiseImage.updatePixels();
		ConsoleLogger.log(this, "     ...done");

		// window crack images
		ConsoleLogger.log(this, "Loading crack images..");
		crackImages = new PImage[maxCracks];
		for (int i = 0; i < maxCracks; i++) {
			String n = "common/crack" + (i + 1) + ".png";
			crackImages[i] = parent.loadImage(n);
		}
		ConsoleLogger.log(this, "     ...done");

	}

	public void addCrack() {
		CrackItem c = new CrackItem();
		c.rotation = (int) parent.random(360);
		c.screenPosition = new PVector(parent.random(1024), parent.random(768));
		c.crackId = (int) parent.random(maxCracks);
		c.scale = 0.8f + parent.random(0.4f);
		crackList.add(c);
	}

	public void clearCracks() {
		crackList.clear();
	}

	public void draw() {
		// image(noiseImage, 100,100);
		if (running) {
			if (damageTimer < parent.millis()) {
				running = false;
			} else {

				for (int x = 0; x < tileX; x++) {
					for (int y = 0; y < tileY; y++) {
						if (parent.random(100) < 25) {
							parent.image(noiseImage, x * noiseImage.width, y
									* noiseImage.height);
						}
					}
				}
			}
		}

	}

	/*
	 * this is called outside of the draw call above so that the shaking effect
	 * doesnt take place
	 */
	public void drawCracks() {
		for (CrackItem c : crackList) {

			parent.pushMatrix();
			parent.translate(c.screenPosition.x, c.screenPosition.y);
			parent.rotate(PApplet.radians(c.rotation));
			PImage p = crackImages[c.crackId];

			parent.image(p, -(p.width * c.scale) / 2f,
					-(p.height * c.scale) / 2f, p.width * c.scale, p.height
							* c.scale);
			parent.popMatrix();
		}

	}

	public void startEffect(long ms) {
		damageTimer = parent.millis() + ms;
		running = true;
	}

	public void startTransform() {
		parent.pushMatrix();
		if (running) {

			parent.translate(parent.random(-20, 20), parent.random(-20, 20));
			parent.tint(parent.random(255));
		}
	}

	public void stopTransform() {
		parent.popMatrix();
		if (running) {
			parent.noTint();
		}
	}

}
