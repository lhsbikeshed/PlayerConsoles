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
	
	PVector shipScreenSpace = new PVector();
	
	
	public LandingDisplay(PlayerConsole parent) {
		super(parent);

		
	}

	@Override
	public void draw() {
		parent.hint(PApplet.ENABLE_DEPTH_TEST);

		long now = parent.millis();
		parent.textFont(parent.getGlobalFont(), 20);
		parent.text("LANDING", 20, 20);
	
		parent.pushMatrix();
		
		parent.noStroke();
		parent.translate(parent.width / 2, parent.height / 2);
		parent.rotateX(PApplet.radians(55)); // 326
		
		
		//parent.rotateY(PApplet.radians(-180));
		
		parent.fill(0,50,0);
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
		parent.line(-400,0,400,0);
		parent.line(0, -400,0,400);

		
		parent.stroke(0,255,0);
		parent.fill(0,128,0);
		PVector pos = shipOffset.getValue(now);
		pos.mult(10f);	//slight fudge
		
		//save ship screen space coords for drawing gui elements later
		shipScreenSpace.x = parent.screenX(pos.x, pos.y, pos.z);
		shipScreenSpace.y = parent.screenY(pos.x, pos.y, pos.z);


		parent.pushMatrix();
		parent.translate(pos.x, pos.y, pos.z);
		
		
		parent.line(0,0,0, 0,0, -pos.z);
		parent.pushMatrix();
		parent.translate(0,0,-pos.z +1);
		parent.ellipse(0,0,40,40);
		parent.popMatrix();
		
		
		float[] ang = localRotation.getValue(now).getAngles(RotOrder.XYZ);
		if (ang != null) {
			parent.rotateX(ang[0]);
			parent.rotateY(ang[1]);
			parent.rotateZ(ang[2]);
		}
		
		/*
		parent.stroke(255,0,0);
		parent.line(0,0,0,-100,0,0);
		parent.stroke(0,255,0);
		parent.line(0,0,0,0,100,0);
		parent.stroke(0,0,100);
		parent.line(0,0,0,0,0,100);
		*/
		
		//draw the ship body
		parent.strokeWeight(1);
		parent.pushMatrix();
		parent.scale(1, 1, 1.3f);
		parent.box(50);
		parent.popMatrix();
		
		//legs
		for(int x = -1; x < 1; x++){
			
			for(int y = -1; y < 1; y++){
				
				parent.pushMatrix();
				parent.translate(30+x*60,-10,25+ y*40);
				parent.box(10);
				parent.popMatrix();
			}
		}
		//tail
		parent.pushMatrix();
		parent.translate(0,31,-30);
		parent.scale(0.4f, 1.4f, 2.3f);
		parent.box(10);
		parent.popMatrix();
		
		parent.popMatrix();
		
		parent.popMatrix();
		parent.hint(PApplet.DISABLE_DEPTH_TEST);
		parent.textFont(font, 18);
		parent.fill(255);
		parent.translate(shipScreenSpace.x, shipScreenSpace.y);
		PVector sp = shipOffset.getValue(now);
		parent.text(String.format("X:%.2f\r\nY:%.2f\r\nZ:%.2f", sp.x, sp.y, sp.z),20,50);

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
