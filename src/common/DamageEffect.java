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

	Boolean running = false;
	Boolean doBoom = false;
	
	Float damageSet = 0f;
	
	ArrayList<CrackItem> crackList = new ArrayList<CrackItem>();
	PImage[] crackImages;

	int maxCracks = 3;

	PApplet parent;
	PShader damageDistortion;
	
	Object lock = new Object();

	public DamageEffect(PApplet parent) {
		this.parent = parent;
		
		ConsoleLogger.log(this, "Loading damage shader..");
		damageDistortion = parent.loadShader("common/damageEffects/distort.glsl");
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
		synchronized (lock) {
			
		
			crackList.add(c);
		}
	}

	public void clearCracks() {
		synchronized (lock) {
			
		
			crackList.clear();
		}
	}
	
	int lastDistort = 0;

	public void draw() {
		int now = parent.millis();
		damageDistortion.set("timer", now);
		synchronized(doBoom) {
			if (doBoom) {
				damageDistortion.set("boom", true);
				doBoom = false;
			}
		}
		synchronized(damageSet) {
			if (damageSet > 0) {
				damageDistortion.set("damage", damageSet);
				damageSet = 0f;
			}
		}
		synchronized(running) {
			if (running) {
				if (damageTimer < now) {
					running = false;
					damageDistortion.set("boom", false);
					ConsoleLogger.log(this, String.format("unbooming at %d!", now));
				} else {
					damageDistortion.set("boom", true);				
				}
			}
		}
		parent.filter(damageDistortion);
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
		}

	}
	public void setDamageLevel(float dmg) {
		synchronized(damageSet) {
			damageSet += dmg;
		}
	}

	public void startEffect(long ms) {
		ConsoleLogger.log(this, String.format("Enbooming at %d for %d!", parent.millis(), ms));
		synchronized(doBoom) {
			doBoom = true;
		}
		synchronized(running) {
			damageTimer = parent.millis() + ms;
			running = true;
		}
	}

}
