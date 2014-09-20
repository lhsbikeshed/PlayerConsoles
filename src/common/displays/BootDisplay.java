package common.displays;


import oscP5.OscMessage;
import processing.core.*;
import common.*;


public class BootDisplay extends Display {
  
  PImage bgImage;
  public  int bootCount = 0;
 
  
  public boolean brokenBoot = false;
  
  int curFile = 0;
  int[] filesToReplace = {12,11,6};
  String[] fileNames = {"kernel32.sys", "PilotIOController.sys", "SplineReticulator.so"};
  int nextFail = 100;
  
  String[] bootText;
  
  BannerOverlay bannerSystem;
  
  public BootDisplay(PlayerConsole parent){
	  super(parent);
	  
	  bannerSystem = parent.getBannerSystem();
    bgImage = parent.loadImage("common/bootScreen/bootlogo.png");
    bootText = parent.loadStrings("common/bootScreen/boottext.txt");
  }
  
  
  public void start(){
    bootCount = 0;
    curFile = 0;
    nextFail = 100;
    
    
  }
  public void stop(){
    bootCount = 0;
  }
  
  public boolean isReady(){
    return bootCount > 400 ? true : false;
  }

  public void draw(){
    //image(bgImage, 0,0,width,height);
	  parent.background(0,0,0);
    
    if(bootCount < 100){
    	parent.textFont(font,15);
    	parent.fill(0,255,0);
      
      int bootLen= (int)PApplet.map(bootCount, 0, 100, 0, bootText.length);
      for(int i = 0; i < bootLen; i++){
    	  parent.text(bootText[i], 30, 30 + 20 * i);
      }
      
      bootCount += 10;
    } else {
    	parent.fill(0,0,255);
    	parent.rect(353, 454, PApplet.map(bootCount, 100, 400, 0, 330), 30);
    	parent.image(bgImage,0,0,parent.width,parent.height);;
      
      bootCount += 10;
      
    }
    
  }
  
  public void setDisks(int[] in){
    ConsoleLogger.log(this, "setting disks to: " + in);
    filesToReplace = in;
  }
  
  public void oscMessage(OscMessage theOscMessage){
    if(theOscMessage.checkAddrPattern("/system/boot/diskInsert")==true){
      boolean correct = theOscMessage.get(0).intValue() == 1 ? true : false;
      if(correct){
        curFile ++;
        if(curFile < 3){
          nextFail += 75;
        } else {
          brokenBoot = false;
          bannerSystem.cancel();
        }
      } else {
        ConsoleLogger.log(this,"Disk insert Failed");
        bannerSystem.cancel();
        bannerSystem.setText("INCORRECT DISK. PLEASE INSERT DISK " + filesToReplace[curFile]);
        bannerSystem.displayFor(360000);
      }
  
    } else if (theOscMessage.checkAddrPattern("/system/boot/justFuckingBoot")){
      //sometimes boots get stuck, this will skip it and power the ship on
      brokenBoot = false;
      bannerSystem.cancel();
      bootCount = 400;
    }
   
  }

  public void serialEvent(String evt){}

  public void keyPressed(){}
  public void keyReleased(){}



}
