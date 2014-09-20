

public class RestrictedAreaScreen implements Display {
 
  
  
  PImage bgImage;
   public RestrictedAreaScreen() {
     bgImage = loadImage("restrictedscreen/bg.png");
   }
   
   
  public void start() {
     consoleAudio.playClip("structuralFailure");
  }
  public void stop() {
  }
  
  public void draw() {
    
    //signalStrength = map(mouseY, 0, height, 0, 1.0f);
    background(0, 0, 0);
    image(bgImage, 0, 0, width, height);
    
  }

  public void oscMessage(OscMessage theOscMessage) {
   
  }

  public void serialEvent(String evt) {
  }

  public void keyPressed() {
  }
  public void keyReleased() {
  }
  
}
