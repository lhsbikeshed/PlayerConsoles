package common;

import processing.core.PApplet;

public  class DrawUtilities {

	public static void drawPilotDamageGrid(PlayerConsole parent, int xpos, int ypos){
		parent.pushStyle();
		parent.pushMatrix();
		parent.translate(xpos, ypos);
		parent.textFont(parent.getGlobalFont(), 16);
		parent.text("SYSTEM DAMAGE", 0,-10);
		
		parent.noFill();
		
		int w = 300;
		int h = 100;
		parent.rect(0,0,w,h);
		parent.textFont(parent.getGlobalFont(), 10);
		String[] names = {"ENGINE", "  AFTER\r\nBURNER", "LANDING\r\n    GEAR", "ROT/PITCH", "ROT/YAW", "ROT/ROLL"};
		
		boolean[] states = {true, false, true, true, true, false};
		for(int x = 0; x < w / 100; x++){
			for(int y = 0; y < h / 50; y++){
				int i = x + y * (w/100);
				
				if(states[i]){
					parent.fill(0,255,0);
				} else {
					if(parent.globalBlinker){
						parent.fill(255,0,0);
					} else {
						parent.fill(255,255,0);
					}
				}
				parent.rect(x*100, y*50, 100, 50);
				parent.fill(0);
				if(i < names.length){
				
					
					parent.text(names[i], x*100+10, y * 50 + 25);
				
				
				}
				
			}	
		}
		
		parent.popMatrix();
		parent.popStyle();
		
	}
}
