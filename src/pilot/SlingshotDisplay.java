package pilot;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;
import common.Camera;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.util.LerpedVector;
import common.util.Rot;
import common.util.RotOrder;
import common.util.UsefulShit;

public class SlingshotDisplay extends Display {

	PGraphics planetGraphics;
	Camera cam;
	
	LerpedVector planetPosition = new LerpedVector(new PVector(0,0,0), 0, 250);
	
	public SlingshotDisplay(PlayerConsole parent) {
		super(parent);
		// TODO Auto-generated constructor stub
		planetGraphics = parent.createGraphics(1024, 768, PApplet.P3D);

		cam = new Camera(parent);
		//cam.jump(0,0,-10);

	}

	@Override
	public void draw() {
		planetGraphics.beginDraw();
		//set up camera pos at 0,0,0
		planetGraphics.camera(512,384,0, 512, 384, -10, 0,1,0);

		//clear to black and draw a wireframe planet
		planetGraphics.background(0);
		planetGraphics.sphereDetail(50);
		planetGraphics.pushMatrix();
		
		planetGraphics.fill(0);
		planetGraphics.stroke(255);;
		
		//position the planet
		planetGraphics.translate(512,384,0);		
		PVector pPos = planetPosition.getValue(parent.millis());
		Rot newRot = Rot.slerp(parent.getShipState().lastShipRot,
				parent.getShipState().shipRot,
				(parent.millis() - parent.getShipState().lastTransformUpdate) / 250.0f,
				false);
		float[] ang = newRot.getAngles(RotOrder.XYZ);
		if (ang != null) {
			planetGraphics.rotateX(ang[0]);
			planetGraphics.rotateY(-ang[1]);
			planetGraphics.rotateZ(-ang[2]);
			//ConsoleLogger.log(this, "" + parent.degrees(ang[2]));
		}
		//positon planet
		planetGraphics.translate(pPos.x, pPos.y, pPos.z);
		
		planetGraphics.sphere(1500);

		planetGraphics.popMatrix();
		
		
		planetGraphics.endDraw();
		
		
		parent.image(planetGraphics, 0,0);
		
		//--------------------- gui section --------------------
		
		parent.fill(255);
		
		//------altitude bar down the side
		float h = (int)parent.getShipState().altitude.getValue(parent.millis());
		
		int startY = 85;
		int startX = 40;
		parent.stroke(255);
		parent.line(startX, startY, startX, startY + 600);
		int len = 20;
		for(int i = 0; i <= 600 / 20; i++){
			if(i % 5 == 0){
				len = 40;
			} else {
				len = 20;
			}
			parent.line(startX, startY + i * 20, startX + len, startY + i * 20);
			
		}
		float markerY = PApplet.map(h, 35000, 0, startY, startY + 600);
		parent.line(startX, markerY, startX + 50, markerY);
		parent.textFont(parent.getGlobalFont(), 15);

		parent.text((int)(parent.getShipState().altitude.getValue(parent.millis()) * 10 )+ "m", 100, markerY);
		
		
		//-------- burn indicators
		parent.textFont(parent.getGlobalFont(), 20);
		parent.text("Time Until Next Burn: ", 706, 35);
		parent.textFont(parent.getGlobalFont(), 45);
		parent.text("20.00 ", 799, 78);
		
		parent.textFont(parent.getGlobalFont(), 20);
		parent.text("Estimated Throttle: ", 706, 126);
		parent.textFont(parent.getGlobalFont(), 45);
		parent.text("15% ", 799, 166);
		
		parent.textFont(parent.getGlobalFont(), 20);
		parent.text("Current Throttle: ", 706, 206);
		parent.textFont(parent.getGlobalFont(), 45);
		parent.text("0% ", 799, 246);
		
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		if(theOscMessage.checkAddrPattern("/system/slingshot/planetPosition")){
			//unity z coord is opposite facing to processing, invert it
			PVector pPos = new PVector( theOscMessage.get(0).floatValue(),
										-theOscMessage.get(1).floatValue(),
										-theOscMessage.get(2).floatValue());
			pPos.mult(15);	//scale it sensibly for this display
			planetPosition.update(pPos, parent.millis());
			
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
