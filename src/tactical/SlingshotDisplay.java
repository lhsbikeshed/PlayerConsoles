package tactical;

import java.awt.Rectangle;

import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PVector;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.util.LerpedVector;

public class SlingshotDisplay extends Display {
	
	//starting position of ship icon
	PVector shipStart = new PVector(120, 200);
	//points around the circle where players have to engage throttle
	BurnPoint[] burnPoints = new BurnPoint[4]; 
	float finalVelocity = 1f;

	int selected = 0;
	PVector finalPoint0, finalPoint1;
	float finalVel = 1f;
	
	public static final int COURSE_CONVERGES = 0;
	public static final int COURSE_DIVERGES = 1;
	public static final int COURSE_OK = 2;
	int courseTrajectory = COURSE_OK;
	
	
	LerpedVector planetPosition = new LerpedVector(new PVector(0,0,0), 0, 250);


	public SlingshotDisplay (PlayerConsole parent) {
		super(parent);
		
		burnPoints[0] = new BurnPoint((float) -Math.PI, 0.5f);
		burnPoints[1] = new BurnPoint((float) -Math.PI + 0.8f, 0.5f);
		burnPoints[2] = new BurnPoint((float) -Math.PI + 1.6f, 0.5f);
		burnPoints[3] = new BurnPoint((float) -Math.PI + 2.4f, 0.5f);
 
		updateRoute();
	}

	@Override
	public void draw() {
		parent.background(0);
		

		parent.fill(128, 20, 0);
		parent.ellipse(parent.width/2, parent.height/2, 375, 375);
		parent.fill(0);
		parent.stroke(255);
		parent.ellipse(parent.width/2, parent.height/2, 300, 300);
		parent.fill(255);
		parent.text("this is mars.\r\nDo not hit mars.\r\nPlease.", 400,400);
		parent.rect(shipStart.x, shipStart.y, 10, 10);
		parent.fill(255);
		
		//burn points
		   for (int i = 0; i < burnPoints.length; i++) {
		    BurnPoint p = burnPoints[i];
		    
		    PVector pos = new PVector(0,1);
		    pos.rotate(p.angle);
		    pos.mult(150 + p.calculatedBurn * 60);
		    pos.add(new PVector(parent.width/2, parent.height/2));
		    parent.ellipse(pos.x, pos.y, 2, 2);
		    if(selected == i){
		    	parent.fill(0,255,0);
		    } else {
		    	parent.fill(255);
		    }
		  
		    p.position = pos;
		    p.draw(parent);
		   
		    
		    if(i > 0){
		    
		      float angUnit = burnPoints[i].angle - burnPoints[i-1].angle;
		      float burnUnit = burnPoints[i].calculatedBurn - burnPoints[i-1].calculatedBurn;
		      PVector bz0 = new PVector(0, 1);
		      bz0.rotate(burnPoints[i-1].angle);
		      bz0.mult(150 + (burnPoints[i-1].calculatedBurn) * 60);
		      bz0.add(new PVector(parent.width/2, parent.height/2));
		      
		      PVector bz1 = new PVector(0, 1);
		      bz1.rotate(burnPoints[i-1].angle + angUnit * 0.33f);
		      bz1.mult(150 + (burnPoints[i-1].calculatedBurn + burnUnit * 0.33f) * 60);
		      bz1.add(new PVector(parent.width/2, parent.height/2));
		      parent.ellipse(bz1.x, bz1.y, 2, 2);
		      
		      PVector bz2 = new PVector(0, 1);
		      bz2.rotate(burnPoints[i-1].angle + angUnit * 0.66f);
		      bz2.mult(150 + (burnPoints[i-1].calculatedBurn + burnUnit * 0.66f) * 60);
		      bz2.add(new PVector(parent.width/2, parent.height/2));
		      parent.ellipse(bz2.x, bz2.y, 2, 2);
		       
		      parent.line(bz0.x, bz0.y, bz1.x, bz1.y);
		      parent.line(bz1.x, bz1.y, bz2.x, bz2.y);
		      parent.line(bz2.x, bz2.y, pos.x, pos.y);
		       if(i == burnPoints.length - 1){
		         finalPoint0 = pos;
		         finalPoint1 = bz2;
		       }
		    } else {
		    	parent.line(shipStart.x, shipStart.y, pos.x,pos.y);
		    }
		   
		  }
		  //calculate the escape vector
		  
		  PVector startPt = new PVector(burnPoints[3].position.x, burnPoints[3].position.y);
		  
		  float angDelta = PApplet.map(finalVel, 0.42f, 3f, 0.1f, 0.5f);
		  float posPower = PApplet.map(finalVel, 0.12f, 3f, 0.95f, 1.4f);
		  //generate a spiral based on the final velocity and point
		  float rad = 150 + burnPoints[3].calculatedBurn * 60;
		  int a = 255;
		  PVector lastPt = new PVector(burnPoints[3].position.x, burnPoints[3].position.y);
		  boolean hitPlanet = false;
		  for(int i = 0; i < 15; i++){
			 
			  rad *= posPower;
			  PVector pt = new PVector(0, 1);
			  pt.normalize();
			  pt.rotate(angDelta * (i + 1) + burnPoints[3].angle);
			  pt.mult(rad);
			  pt.x += parent.width /2 ;
			  pt.y +=  parent.height/2;
			  parent.fill(a);
			  a-=40;
			  parent.ellipse(pt.x, pt.y, 10, 10);
			  if(lastPt != null){
				  parent.line(pt.x, pt.y, lastPt.x, lastPt.y);
			  }
			  lastPt = new PVector(pt.x, pt.y);
			  
			  //test for lithobraking
			  float dist = PVector.dist(pt, new PVector(parent.width/2, parent.height/2));
			  if(dist < 375/2){
				  hitPlanet = true;
				  if(parent.globalBlinker){
					  parent.fill(255,0,0);
				  } else {
					  parent.fill(255,255,255);
				  }
				  parent.text("WARNING!", pt.x, pt.y);
				  break;
			  } if (dist < 150){
				  break;
			  }
			  
		  }
		  
		  if(hitPlanet){
			  courseTrajectory = COURSE_CONVERGES;
			  
		  }
		  
		  //final velocity
		  parent.text("v: " + angDelta, finalPoint0.x+20, finalPoint0.y+30);
		  
		  parent.text(selected, 100,100);
		  
		  PVector pos = ((TacticalConsole)parent).mousePosition;
			parent.pushMatrix();
			parent.translate(pos.x, pos.y);		
			parent.noFill();
			parent.line(-20,0, 20,0);
			parent.line(0, 20, 0, -20);
			float scale = parent.sin(parent.millis() / 200f) * 0.2f + 0.8f;
			parent.scale(scale);
			
			parent.ellipse(0, 0,  30, 30);
			
			
			parent.popMatrix();
			
			
			
			//draw the ship on 
			PVector pPos = planetPosition.getValue(parent.millis());
			parent.pushMatrix();
			
			parent.translate(pPos.z + parent.width/2, pPos.y + parent.height/2,0);
			parent.rect(0,0,10,10);
			
			parent.popMatrix();
			
		
	}

	@Override
	public void oscMessage(OscMessage msg) {
		if(msg.checkAddrPattern("/system/slingshot/planetPosition")){
			//unity z coord is opposite facing to processing, invert it
			PVector pPos = new PVector( msg.get(0).floatValue(),
										msg.get(1).floatValue(),
										msg.get(2).floatValue());
			pPos.mult(1.5f);
			planetPosition.update(pPos, parent.millis());
			
		}
		
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		
		if(evt.event.equals("MOUSECLICK")){
			PVector m = ((TacticalConsole)parent).mousePosition;
			
			for(BurnPoint p : burnPoints){
				PVector test = p.translatePoint(m);
				if(p.upButtonBounds.contains(test.x, test.y)){
					p.changeValue(0.05f);
					updateRoute();
					break;
				} else if(p.downButtonBounds.contains(test.x, test.y)){
					p.changeValue(-0.05f);
					updateRoute();
					break;
				}
			}
			
		}
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
	
	void updateRoute() {

	    
		  //assume ideal burn values are 0.5
		  //to calculate burn
		  
		  for (int i = 0; i < burnPoints.length; i++) {
		    BurnPoint p = burnPoints[i];
		    
		    if(i > 0){
		      p.calculatedBurn = burnPoints[i-1].calculatedBurn * (1.2f + ((0.1f - (1f-burnPoints[i-1].val)) * 0.5f));
		    } else {
		      p.calculatedBurn = 1f;
		    }
		    
		    
		    
		  }
		  BurnPoint last = burnPoints[burnPoints.length -1];
		  finalVel = last.calculatedBurn + last.val;
		  
		  
		  
		}

	class BurnPoint {

		 
		  public float val;
		  public float fuckValue = 0f;
		  public float calculatedBurn = 0f;
		  
		  public float angle = 0f;
		  
		  public Rectangle upButtonBounds = new Rectangle(0,-5,20,20);
		  public Rectangle downButtonBounds = new Rectangle(0,25,20,20);
		  public PVector position = new PVector(0,0);

		  /* angle = angle around the centrepoint of the planet
		   * val = thrust value at that point
		   */
		  public BurnPoint(float angle, float val) {
		    this.val = val;
		    this.angle = angle;
		    
		  }
		  
		  public void changeValue(float amt){
		    val += amt;
		    if(val < 0){
		      val = 0;
		    } else if (val > 1.0f){
		      val = 1.0f;
		    }
		  }
		  
		  public PVector translatePoint(PVector in){
			  PVector ret = new PVector(in.x - position.x - 60, in.y - position.y + 50);
			  return ret;
		  }
		  
		  public void draw(PlayerConsole p){
			  
			 
			  
			  p.pushMatrix();
			  p.translate(position.x + 60, position.y - 50);
			  p.fill(20);
			  p.stroke(255,255,0);
			  p.strokeWeight(2);
			  p.rect(-10,-10,200,60);
			  p.fill(0);
			  p.strokeWeight(1);
			  p.stroke(255);
			  p.rect(upButtonBounds.x, upButtonBounds.y, upButtonBounds.width, upButtonBounds.height);
			  p.rect(downButtonBounds.x, downButtonBounds.y, downButtonBounds.width, downButtonBounds.height);
			  
			  p.fill(255);
			  p.textFont(parent.getGlobalFont(), 12);
			  int throt = (int) (val * 100);
			  p.text("THROTTLE: " + throt + "%", 30, 20);
			  p.popMatrix();
			  p.line(position.x + 50, position.y - 30, position.x, position.y);
			  p.ellipse(position.x, position.y, 10, 10);
		  }
		    
		}
}
