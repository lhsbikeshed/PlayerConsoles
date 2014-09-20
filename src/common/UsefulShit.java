package common;

public class UsefulShit {

	public static int makeColor(int r, int g, int b){
		return makeColor(r,g,b,255);
		
	}
	
	public static int makeColor(int r, int g, int b, int a){
		int c = a << 24 | r << 16 | g << 8 | b;
		return c;
	}
	
}
