package common.displays;

import java.awt.Point;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import common.BannerOverlay;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;

public class WarpDisplay extends Display {

	PImage bgImage;
	PImage overlayImage;
	PImage shipIcon;
	PImage planetImage;

	// 22

	// state things
	boolean haveFailed = false; // have we failed/
	long failStart = 0; // when fail started
	long failDelay = 0;
	long exitStartTime = -1;

	float timeRemaining = 30;
	float lastTimeRemaining = 30;
	long lastUpdate = 0;
	boolean thisIsFail = false;

	int gridWidth = 20;
	int gridHeight = 20;
	Point[][] gridPts;

	int gridX = 0;
	int gridY = 279;

	float warpAmount = 0.0f;
	long sceneStart = 0;
	boolean warpingIn = false;
	boolean warpingOut = false;
	float sinOffset = 0.0f;
	Point[] stars = new Point[50];
	PGraphics warpGrid;

	int planetX;
	float planetScale = 0.6f;

	int screenFlashTimer = 0;
	BannerOverlay bannerSystem;

	public WarpDisplay(PlayerConsole parent) {
		super(parent);
		bannerSystem = parent.getBannerSystem();
		bgImage = parent.loadImage("common/warpScreen/hyperspace2.png");
		overlayImage = parent
				.loadImage("common/warpScreen/hyperfailoverlay.png");
		shipIcon = parent.loadImage("common/warpScreen/hyperShipIcon.png");
		planetImage = parent.loadImage("common/warpScreen/hyperPlanet.png");
		// setup grid points
		int cellW = (2 * parent.width) / gridWidth;
		int cellH = 400 / gridHeight;
		gridPts = new Point[gridWidth][gridHeight];
		for (int x = 0; x < gridWidth; x++) {
			for (int y = 0; y < gridHeight; y++) {
				int tlx = gridX + cellW * x;
				int tly = cellH * y;
				gridPts[x][y] = new Point(tlx, tly);
			}
		}

		for (int i = 0; i < 50; i++) {
			stars[i] = new Point((int) parent.random(parent.width + 100), gridY
					+ (int) parent.random(400));
		}
		warpGrid = parent.createGraphics(parent.width, 400);
	}

	private float clamp(float in, float min, float max) {
		if (in < min) {
			return min;
		} else if (in > max) {
			return max;
		} else {
			return in;
		}
	}

	@Override
	public void draw() {

		parent.background(0, 0, 0);

		drawGrid();

		if (thisIsFail) {
			int pW = (int) (planetImage.width * planetScale);
			int pH = (int) (planetImage.height * planetScale);

			planetX = (int) PApplet.lerp(parent.width, 637,
					(parent.millis() - sceneStart) / 20000.0f);
			planetScale = PApplet.lerp(0.6f, 1.0f,
					(parent.millis() - sceneStart) / 20000.0f);
			parent.image(planetImage, planetX + pW / 2, 485 - pH / 2, pW, pH);
		}

		parent.image(bgImage, 0, 0, parent.width, parent.height);

		parent.image(shipIcon, 75, 422);
		parent.fill(255, 255, 0);
		parent.textFont(parent.getGlobalFont(), 40f);
		if (timeRemaining >= 0.0f) {
			if (parent.millis() - lastUpdate < 275f) {
				float t = PApplet.lerp(lastTimeRemaining, timeRemaining,
						(parent.millis() - lastUpdate) / 250.0f);
				parent.text(t, 756, 114);
			}
		} else {
			if (warpingOut == false) {
				exitStartTime = parent.millis();
				warpingOut = true;
			}
		}

		if (screenFlashTimer > 0) {
			screenFlashTimer--;
			float alpha = PApplet.map(screenFlashTimer, 40, 0, 255, 0);
			parent.fill(255, 255, 255, alpha);
			parent.rect(0, 0, parent.width, parent.height);

		}
	}

	void drawGrid() {
		warpGrid.beginDraw();
		warpGrid.background(0, 0, 0);
		warpGrid.stroke(255, 0, 0);
		warpGrid.strokeWeight(1);
		sinOffset += 0.01f;
		if (sceneStart + 5000 > parent.millis() && warpingIn) {
			warpAmount = PApplet.map(parent.millis() - sceneStart, 0f, 5000f,
					0.0f, 1.0f);
		} else {
			if (warpingIn) {
				screenFlashTimer = 40;
			}
			warpingIn = false;
		}
		if (warpingOut) {
			warpAmount = PApplet.map(parent.millis() - exitStartTime, 0f,
					8000f, 1.0f, 0.0f);
			warpAmount = clamp(warpAmount, 0.0f, 1.0f);

		}
		warpGrid.fill(255, 255, 255);
		warpGrid.noStroke();
		for (int i = 0; i < stars.length; i++) {
			stars[i].x -= 55 * warpAmount;
			if (stars[i].x < 0) {
				stars[i].x = parent.width + (int) parent.random(100);
				stars[i].y = (int) parent.random(400);
			}
			warpGrid.rect(stars[i].x, stars[i].y,
					(float) (40 * (warpAmount + 0.1)), 1);
		}
		warpGrid.strokeWeight(1);
		warpGrid.stroke(255, 0, 0);
		warpGrid.noFill();
		for (int y = 0; y < gridHeight - 1; y++) {
			for (int x = 0; x < gridWidth - 1; x++) {

				// calc sin offset
				float s = 10 - Math.abs(y - 10) - ((y - 10) * (y - 10))
						* warpAmount * 20.0f;
				// s = sin(s) * (mouseX / 100.0f);
				float sNext = 10 - Math.abs(y - 9) - ((y - 9) * (y - 9))
						* warpAmount * 20.0f;
				// sNext = sin(sNext) * (mouseX / 100.0f);

				Point tl = gridPts[x][y];
				Point tr = gridPts[x + 1][y];
				Point bl = gridPts[x][y + 1];
				Point br = gridPts[x + 1][y + 1];

				int randAmt = haveFailed == true ? 1 : 0;

				float tlx = tl.x + s + parent.random(15) * randAmt;
				float trx = tr.x + s + parent.random(15) * randAmt;
				float blx = bl.x + sNext + parent.random(15) * randAmt;
				float brx = br.x + sNext + parent.random(15) * randAmt;

				// tlx = clamp(tlx, 17, width - 22);
				// trx = clamp(trx, 17, width - 22);
				// blx = clamp(blx, 17, width - 22);
				// brx = clamp(brx, 17, width - 22);
				warpGrid.stroke(0, 0, PApplet.map(PApplet.sin((float) ((parent
						.millis() / 250.0f) + (x * 0.1))), -1.0f, 1.0f, 120,
						255));

				warpGrid.quad(tlx, tl.y, trx, tr.y, brx, br.y, blx, bl.y);
			}
		}
		warpGrid.endDraw();
		parent.image(warpGrid, gridX, gridY);
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		if (theOscMessage.checkAddrPattern("/scene/warp/updatestats") == true) {
			lastTimeRemaining = timeRemaining;
			timeRemaining = theOscMessage.get(1).floatValue();
			thisIsFail = theOscMessage.get(2).intValue() == 1 ? true : false;
			lastUpdate = parent.millis();
		} else if (theOscMessage.checkAddrPattern("/scene/warp/failjump") == true) {
			haveFailed = true;
			failStart = parent.millis();
			failDelay = theOscMessage.get(0).intValue() * 1000;
			bannerSystem.setSize(700, 300);
			bannerSystem.setTitle("WARNING");
			bannerSystem
					.setText("GRAVITATIONAL BODY DETECTED, TUNNEL COLLAPSING, PREPARE FOR UNPLANNED REENTRY");
			bannerSystem.displayFor(5000);
		}
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
	}

	@Override
	public void start() {

		sceneStart = parent.millis();
		timeRemaining = 30;
		warpingIn = true;
		warpingOut = false;
		planetX = parent.width;
		thisIsFail = false;
		planetScale = 0.6f;
		exitStartTime = -1;

	}

	@Override
	public void stop() {
		haveFailed = false;
		warpAmount = 0.0f;
		warpingIn = false;

	}
}
