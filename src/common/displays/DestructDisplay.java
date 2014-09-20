package common.displays;

import oscP5.OscMessage;
import processing.core.*;
import common.Display;
import common.PlayerConsole;


public class DestructDisplay extends Display {
  
  PImage bgImage, criticalImg;
  int time;
    
  int blinkTime = 0;
  boolean blinker = false;
  
  
  public DestructDisplay(PlayerConsole parent){
	  super(parent);
    bgImage = parent.loadImage("common/selfDestructScreen/destruct.png");
    criticalImg = parent.loadImage("common/selfDestructScreen/critical.png");
  }
  
  
  public void start(){
    time =60;
  }
  public void stop(){
    
  }
  
  public void draw(){
    if(blinkTime + 1000 < parent.millis()){
      blinker = !blinker;
      blinkTime = parent.millis();
    }
    parent.background(0,0,0);
    parent.image(bgImage, 0,0,parent.width,parent.height);
    if(blinker){
    	parent.image(criticalImg, 0, 90);
    }
    parent.textFont(font, 50);
    
    parent.fill(255,255,255);
    int x = 625 - (int)parent.textWidth("" + time) / 2 ;
    parent.text(time, x,440);
  }
  
  public void oscMessage(OscMessage msg){
    if(msg.checkAddrPattern("/system/reactor/overloadstate")){
      time = msg.get(0).intValue();
      if(time < 0 ) time = 0;
    }
  }

  public void serialEvent(String evt){}

  public void keyPressed(){}
  public void keyReleased(){}
}
