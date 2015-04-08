package pilot;

import com.jogamp.opengl.math.Quaternion;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PVector;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.util.LerpedRot;
import common.util.LerpedVector;
import common.util.Rot;
import common.util.RotOrder;
import common.util.UsefulShit;

public class LandingDisplay extends Display {
	
	LerpedVector shipOffset = new LerpedVector(new PVector(), 0, 250);
	LerpedRot localRotation = new LerpedRot(Rot.IDENTITY, 0, 250);
	long lastQuaternionTime = 0;
	
	public LandingDisplay(PlayerConsole parent) {
		super(parent);
		
	}

	@Override
	public void draw() {
		long now = parent.millis();
		parent.textFont(parent.getGlobalFont(), 20);
		parent.text("LANDING", 20, 20);
		parent.text("" + shipOffset.getValue(now),20,50);
		float[] ang = localRotation.getValue(now).getAngles(RotOrder.XYZ);
		parent.text("" + (int)parent.degrees(ang[0]), 10, 120);
		parent.text("" + (int)parent.degrees(ang[1]), 10, 150);
		parent.text("" + (int)parent.degrees(ang[2]), 10, 170);
		
		parent.pushMatrix();
		
		parent.noStroke();
		parent.translate(parent.width / 2, parent.height / 2);
		parent.rotateX(PApplet.radians(55)); // 326
		
		
		//parent.rotateY(PApplet.radians(-180));
		
		parent.fill(128);
		parent.rect(-400,-400,800,800);
		parent.noFill();
		parent.strokeWeight(3);
		for(int i = 0; i < 5; i++){
			parent.stroke(0,128,0);
			if((parent.millis() % 1000 ) / 200 == (4 - i)){
				parent.stroke(0,255,0);	//pulse inward
			}
			parent.ellipse(0, 0, 150 + i * 120, 150 + i * 120);
		}
		
		parent.stroke(0,255,0);
		parent.fill(0,128,0);
		PVector pos = shipOffset.getValue(now);
		pos.mult(10f);	//slight fudge
		parent.pushMatrix();
		parent.translate(pos.x, pos.y, pos.z);
		parent.line(0,0,0, 0,0, -pos.z);
		
		if (ang != null) {
			parent.rotateX(ang[0]);
			parent.rotateY(ang[1]);
			parent.rotateZ(ang[2]);
		}
		
		parent.stroke(255,0,0);
		parent.line(0,0,0,-100,0,0);
		parent.stroke(0,255,0);
		parent.line(0,0,0,0,-100,0);
		parent.stroke(0,0,100);
		parent.line(0,0,0,0,0,100);
		
		
		parent.strokeWeight(1);
		parent.box(50);
		parent.translate(0,0,30);
		parent.box(10);
		
		parent.popMatrix();
		parent.popMatrix();
	}

	@Override
	public void oscMessage(OscMessage msg) {
		if(msg.checkAddrPattern("/screen/landingDisplay/shipTransform")){
			long now = parent.millis();
			PVector r = new PVector(msg.get(0).floatValue(),
									msg.get(1).floatValue(),
									msg.get(2).floatValue());
			shipOffset.update(r,now);
			
			//rotation as a quaternion
			float w = msg.get(3).floatValue();
			float x = msg.get(4).floatValue();
			float y = msg.get(5).floatValue();
			float z = msg.get(6).floatValue();
		
			Rot rot = new Rot(w, x, y, z, false);
			localRotation.update(rot, now);
			
		}

	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		// TODO Auto-generated method stub

	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

}
