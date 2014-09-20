package common;

import java.util.ArrayList;

import processing.core.*;


public class BannerOverlay {

  PImage cornerImg, edgeImg;

  private long startDisplayTime = 0;
  private long duration = 1000;

  private boolean visible = false;

  private PVector pos = new PVector(0, 0);
  private PVector size = new PVector(32, 32);

  private String title = "TITLE";
  private ArrayList<String> text = new ArrayList<String>();

  private PFont font;

  private int iconIndex = 0;
  private PImage[] icons = new PImage[1];
  String pathBase = "C:/Users/tom/Dropbox/starship/tacticalconsole/data/";
  //String pathBase = "c:/game/dev/pilotconsole/data/";    //LIVE

  PlayerConsole parent;

  public BannerOverlay (PlayerConsole parent) {
	  this.parent = parent;
    cornerImg = parent.loadImage(pathBase + "corner.png");
    edgeImg = parent.loadImage(pathBase + "edge.png");
    font = parent.loadFont(pathBase + "HanzelExtendedNormal-48.vlw");
    icons[0] = parent.loadImage(pathBase + "warningicon.png");
  }

  public void draw() {

    if (visible) {
      if (startDisplayTime + duration < parent.millis()) {
        visible = false;
      }

      //draw it
      drawBox();
      parent.fill(255, 0, 0);
      parent.textFont(font, 50);
      int textX = (int)((pos.x + size.x/2) - ( parent.textWidth(title) / 2));
      int textY = (int)(pos.y + 70) ;
      parent.text(title, textX, textY);

      parent.textFont(font, 20);
      parent.fill(255, 255, 0);
      textX = (int)pos.x + 180;
      textY = (int)pos.y + 120;
      for (String line : text) {
    	  parent.text(line, textX, textY);
        textY += 30;
      }
      parent.image(icons[0], pos.x + 30, pos.y + 80, 120, 120);
    }
  }

  public void setText(String text) {
    String[] words = text.split(" ");
    parent.textFont(font, 20);
    int maxWidth = (int)size.x - 170; //30 px border each size
    this.text = new ArrayList<String>();
    String curLine = "";
    for (String word : words) {
      if (parent.textWidth(curLine + " " + word) < maxWidth) {
        curLine += word + " ";
      } 
      else {
        this.text.add(curLine);
        curLine = word + " ";
      }
    }
    this.text.add(curLine);
  }

  public void setTitle(String title) {
    this.title = title;
  }

  private void drawBox() {

	  parent.pushMatrix();
	  parent.translate(pos.x, pos.y); 
	  parent.image(cornerImg, 0, 0);   //TL
	  parent.pushMatrix();
    //translate(32, pos.y);
	  parent.rotateZ(PConstants.PI/2);
	  parent.image(edgeImg, 0, -32, 32, -size.x + 32); //TOP
	  parent.popMatrix();

	  parent.pushMatrix(); //TR
	  parent.translate(size.x + 32, 0); 
	  parent.rotateZ(PConstants.PI/2);

	  parent.image(cornerImg, 0, 0 );
	  parent.popMatrix();

	  parent.pushMatrix();
	  parent.translate(size.x, 32);
	  parent.rotateZ(PConstants.PI);
    parent.image(edgeImg, -32, - size.y, 32, size.y );
    parent.popMatrix();

    parent.pushMatrix();
    parent.translate(0, size.y+ 64);
    parent.rotateZ(-PConstants.PI/2);
    parent.image(edgeImg, 0, 32, 32, size.x - 32);
    parent.popMatrix();

    parent.pushMatrix();
    parent.translate(0, 32);
    // rotateZ(PI);
    parent.image(edgeImg, 0, 0, 32, size.y );
    parent.popMatrix();


    parent.pushMatrix(); //BR
    parent.translate(size.x + 32, size.y + 64); 
    parent.rotateZ(-PConstants.PI);

    parent.image(cornerImg, 0, 0 );
    parent.popMatrix();

    parent.pushMatrix();
    parent.translate(0, size.y);
    parent.rotateZ(-PConstants.PI/2);
    parent.image(cornerImg, -64, 0, 32, 32); //TOP
    parent.popMatrix();

    parent.popMatrix();

    parent.fill(0);
    parent.noStroke();
    parent.rect(pos.x + 16, pos.y + 16, size.x - 16, size.y + 16);
  }

  public void cancel(){
    visible = false;
  }

  public void displayFor(int time) {
    if (!visible) {
      startDisplayTime = parent.millis();
      visible = true;
      duration = time;
      parent.getConsoleAudio().playClip("bannerPopup");
    }
  }


  public void setSize(int w, int h) {

    size.x = w;
    size.y = h;
    pos.x = (parent.width - w) / 2;
    pos.y = (parent.height - h) / 2;
  }
}

