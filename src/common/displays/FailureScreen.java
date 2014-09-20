package common.displays;

import oscP5.OscMessage;
import processing.core.*;
import common.Display;
import common.PlayerConsole;


public class FailureScreen extends Display {

  PImage background;
  PImage warningImage;
  PImage leftIcon, rightIcon;
  
  int animTime = 0;
  float leftIconPos = 309f;
  float rightIconPos = 435f;
    
  
  public FailureScreen(PlayerConsole parent){
super(parent);
background = parent.loadImage("common/failureScreen/bg.png");
warningImage = parent.loadImage("common/failureScreen/warning.png");

leftIcon = parent.loadImage("common/failureScreen/leftPart.png");
rightIcon = parent.loadImage("common/failureScreen/rightPart.png");
  }
  
  public void start(){
    animTime = 0;
    parent.getConsoleAudio().playClip("structuralFailure");
  }
  public void stop(){}
  
  public void draw(){
	  parent.image(background,0,0,parent.width,parent.height);
    animTime++;
    leftIconPos-=0.2f;
    rightIconPos+=0.2f;
    
    
    parent.pushMatrix();    
    parent.translate(leftIconPos + leftIcon.width, 328 + 328);
    parent.rotate(parent.radians(-animTime / 10.0f));
    parent.image(leftIcon, -leftIcon.width, -328);
    parent.popMatrix();
    
    parent.pushMatrix();    
    parent.translate(rightIconPos + 10, 229 + 229);
    parent.rotate(parent.radians(animTime / 10.0f));
    parent.image(rightIcon, -10, -229);
    parent.popMatrix();
    
    for(int i = 0; i < 5; i++){
      if(parent.random(10) > 7){
    	  parent.fill(220 + parent.random(40),204,0);
        float rad = 50 + parent.random(50);
        parent.ellipse(435 + parent.random(40), 350 + parent.random(100), rad, rad);
      }
    }
      
  
  
  
    if(parent.globalBlinker){
      parent.image(warningImage,44, 28);
    }
  }
  public void oscMessage(OscMessage theOscMessage){}
 
  public void serialEvent(String content){}
}
  
