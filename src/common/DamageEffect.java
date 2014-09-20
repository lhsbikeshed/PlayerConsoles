package common;
import processing.core.*;

public class DamageEffect {
  //time we last got damaged
  long damageTimer = -1000;
  PImage noiseImage; //static image that flashes
  
  boolean running = false;
  
  int tileX = 5;
  int tileY = 5;
  
  PApplet parent;
  
  public DamageEffect(PApplet parent){
	  this.parent = parent;
    PApplet.println("generating damage images...");
    noiseImage = parent.createImage(parent.width / tileX, parent.height / tileY, PConstants.RGB);
    noiseImage.loadPixels();
    for (int i = 0; i < noiseImage.width * noiseImage.height; i++){
      noiseImage.pixels[i] = parent.color(parent.random(255));
    }
    noiseImage.updatePixels();
    PApplet.println("     ...done");
  } 

  public void startTransform(){
     parent.pushMatrix();
    if(running){
     
    	parent.translate(parent.random(-20, 20), parent.random(-20, 20));
    	parent.tint(parent.random(255));
    }
  }
  
  public void stopTransform(){
	  parent.popMatrix();
    if(running){
    	parent.noTint();
    }
  }

  public void draw(){
    //image(noiseImage, 100,100);
     if(running){
       if(damageTimer < parent.millis()){
         running = false;
       } else {
         
         for(int x = 0; x < tileX; x++){
           for(int y = 0; y < tileY; y++){
             if(parent.random(100) < 25){
            	 parent.image(noiseImage, x * noiseImage.width, y * noiseImage.height);
             }
           }
         }
       }
     }
             
  }
  
  
  
  public void startEffect(long ms){
    damageTimer = parent.millis() + ms;
    running = true;
  }

}  
