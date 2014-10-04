package common;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PImage;
import processing.core.PVector;
import processing.opengl.PShader;

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
	PShader damageDistortion;
	
	Object lock = new Object();

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
		
		damageDistortion = parent.loadShader("common/damageEffects/distort.glsl");

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
		synchronized (lock) {
			
		
			crackList.add(c);
		}
	}

	public void clearCracks() {
		synchronized (lock) {
			
		
			crackList.clear();
		}
	}

	public void draw() {
		// image(noiseImage, 100,100);
		if (running) {
			if (damageTimer < parent.millis()) {
				running = false;
			} else {
				damageDistortion.set("timer", parent.millis());
				parent.filter(damageDistortion);
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
		for(int i = 0; i < crackList.size(); i++){
			CrackItem c = crackList.get(i);
			parent.pushMatrix();
			parent.translate(c.screenPosition.x, c.screenPosition.y);
			PImage p = crackImages[c.crackId];
			//now randomly draw black parts behind the crack
			
			//now draw the crack
			parent.rotate(PApplet.radians(c.rotation));
			

			parent.image(p, -(p.width * c.scale) / 2f,
					-(p.height * c.scale) / 2f, p.width * c.scale, p.height
							* c.scale);
			
			
			
			parent.popMatrix();
			if(parent.random(10) <= 1){
				
				parent.fill(parent.random(255));
				parent.noStroke();
				int pos = (int) (c.screenPosition.y + p.height/2f + parent.random(2));
				parent.rect(0,pos, parent.width, 2);
			}
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
