package pilot;
import oscP5.OscMessage;
import common.*;
import processing.core.*;

public class LaunchDisplay extends Display {

  PImage bgImage;
  int beamPower = 2;
  int sensorPower = 2;
  int propulsionPower = 2;

  PImage shipImg, feetImg;
  //22

  boolean landed = false;
  boolean clamped = true;
  boolean bayGravity = true;
  int undercarriageState = 1;
  private String[] undercarriageStrings = {
    "up", "down", "Lowering..", "Raising.."
  };

  PVector shipPos = new PVector(0, 0, 0);
  PVector shipRot = new PVector(0, 0, 0);

  PVector lastShipPos = new PVector(0, 0, 0);
  long lastPosUpdate = 0;
  float lastRotation = 0;
  //bay img is 600x300, 95,370

    public LaunchDisplay(PlayerConsole parent) {
    	super(parent);
    bgImage = parent.loadImage("pilotconsole/launchdisplay.png");
    shipImg = parent.loadImage("pilotconsole/shipbehind.png");
    feetImg = parent.loadImage("pilotconsole/shipfeet.png");
  }


  public void oscMessage(OscMessage theOscMessage) {

    if (theOscMessage.checkAddrPattern("/system/subsystemstate") == true) {
      beamPower = theOscMessage.get(3).intValue() + 1;
      sensorPower = theOscMessage.get(2).intValue() + 1;
      propulsionPower = theOscMessage.get(0).intValue() + 1;
    } 
    else if (theOscMessage.checkAddrPattern("/ship/undercarriage/contact") == true) {
      landed = theOscMessage.get(0).intValue() == 1 ? true : false;
    } 
    else if (theOscMessage.checkAddrPattern("/ship/undercarriage")) {
      undercarriageState = theOscMessage.get(0).intValue();
    } 
    else if (theOscMessage.checkAddrPattern("/system/misc/clampState")) {
      clamped = theOscMessage.get(0).intValue() == 1 ? true : false;
    } else if(theOscMessage.checkAddrPattern("/scene/launchland/bayGravity")){
      bayGravity = theOscMessage.get(0).intValue() == 1 ? true : false;
    }
    else if (theOscMessage.checkAddrPattern("/scene/launchland/dockingPosition")) {
      lastShipPos.x = shipPos.x; 
      lastShipPos.y = shipPos.y; 
      lastShipPos.z = shipPos.z;
      lastPosUpdate = parent.millis();
      
      
      lastRotation = shipRot.z;
      
      
      shipPos.x = theOscMessage.get(0).floatValue();
      shipPos.y = theOscMessage.get(1).floatValue();
      shipPos.z = theOscMessage.get(2).floatValue();
      shipRot.x = theOscMessage.get(3).floatValue();
      shipRot.y = theOscMessage.get(4).floatValue();
      shipRot.z = theOscMessage.get(5).floatValue();
     // println(shipPos.y + " : " + shipPos.z);
    }
  }


  public void start() {
  }
  public void stop()
  {
  }
  public void draw() {

    parent.background(0, 0, 0);


    parent.image(bgImage, 0, 0, parent.width, parent.height);
    parent.textFont(font, 15);
    parent.fill(255, 255, 0);

    parent.text("Docking Clamp: " + (clamped == true ? "Engaged" : "Disengaged"), 60, 110);
    parent.text("Undercarriage: " + undercarriageStrings[undercarriageState], 60, 130);
    parent.text("Bay Gravity: " + (bayGravity == true ? "On" : "Off"), 60, 150);
    if (landed) {
      parent.text("Floor Contact", 60, 170);
    }

    parent.pushMatrix();

    PVector lerpPos = new PVector(0, 0, 0);
    lerpPos.x = parent.lerp(lastShipPos.z, shipPos.z, (parent.millis() - lastPosUpdate) / 200.0f);
    lerpPos.y = parent.lerp(lastShipPos.y, shipPos.y, (parent.millis() - lastPosUpdate) / 200.0f);

    int screenX = (int)parent.map(lerpPos.x, .19f, -.225f, 210f, 790f);
    int screenY = (int)parent.map(lerpPos.y, .06f, -0.13f, 390f, 575f);
    parent.translate(screenX, screenY);
    //scale(0.5,0.5);

    parent.rotate(-CurveAngle(lastRotation, shipRot.z ,(parent.millis() - lastPosUpdate) / 200.0f));
    parent.translate(-shipImg.width / 4, -shipImg.height/4);
    
    
    if(undercarriageState == 3){
      parent.tint(255,0,0);
      parent.image(feetImg, -12, 140, feetImg.width/2, feetImg.height/2);
    } else if (undercarriageState == 2){
      parent.tint(0,255,0);
      parent.image(feetImg, -12, 140, feetImg.width/2, feetImg.height/2);
      
    } else if (undercarriageState == 1){
      parent.noTint();
      parent.image(feetImg, -12, 140, feetImg.width/2, feetImg.height/2);
    }
   
    parent.noTint();
    parent.image(shipImg, 0, 0, shipImg.width/2, shipImg.height/2);
    
    parent.popMatrix();
  }


  public void serialEvent(String evt) {
  }
  
  public float CurveAngle(float start, float end, float step) 
{ 
  float from = parent.radians(start);
  float to = parent.radians(end);
  // Ensure that 0 <= angle < 2pi for both "from" and "to" 
  while(from < 0) 
    from += PApplet.TWO_PI; 
  while(from >= PApplet.TWO_PI) 
    from -= PApplet.TWO_PI; 
 
  while(to < 0) 
    to += PApplet.TWO_PI; 
  while(to >= PApplet.TWO_PI) 
    to -= PApplet.TWO_PI; 
   
  if(Math.abs(from-to) < PApplet.PI) 
  { 
    // The simple case - a straight lerp will do. 
    return parent.lerp(from, to, step); 
  } 
 
  // If we get here we have the more complex case. 
  // First, increment the lesser value to be greater. 
  if(from < to) 
    from += PApplet.TWO_PI; 
  else 
    to += PApplet.TWO_PI; 
 
  float retVal = parent.lerp(from, to, step); 
   
  // Now ensure the return value is between 0 and 2pi 
  if(retVal >= PApplet.TWO_PI) 
    retVal -= PApplet.TWO_PI; 
  return retVal; 
} 
  
  
  
}

