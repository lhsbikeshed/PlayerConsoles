package common;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.imageio.ImageIO;

import processing.core.PImage;

public class MJPEGImagethread implements Runnable{
	Socket socket;
	boolean running = false;
	byte[] currentFrame = new byte[512000];
	int bufPtr = 0;
	int imgStart = -1;
	int imgEnd = -1;
	
	boolean connected = false;
	Object lock = new Object();
	BufferedImage frame;
	PImage lastFrame;
	
	boolean frameDirty = false;
	PlayerConsole parent;
	public MJPEGImagethread(PlayerConsole parent) {
		this.parent = parent;
	}
	
	public void stop(){
		running = false;
		ConsoleLogger.log(this, "Cam stopped");
	}
	
	public void run(){
		running = true;
		try{
			Socket socket = new Socket(parent.getServerIP(), 8000);
			DataInputStream dIn = new DataInputStream(socket.getInputStream());
			connected = true;
			while(running){
				
				// if the imgEnd is before the current bufPtr then strip image
				// data out
				if(imgEnd > 0 && imgEnd <= bufPtr ){
					byte[] temp = new byte[imgEnd - imgStart];
					System.arraycopy(currentFrame, imgStart, temp, 0, temp.length);
					processFrame(temp);
					// remove the imagedata from the array
					System.arraycopy(currentFrame, imgEnd, currentFrame, 0, currentFrame.length - imgEnd);
					bufPtr = 0;
					imgEnd = -1;
					imgStart = -1;
					frameDirty = true;
					//ConsoleLogger.log(this, "FRAME DONE");
				}
				
				
				byte[] message = new byte[100];
				int length = dIn.read(message);
				
				if(length>0) {
					if(bufPtr + length > currentFrame.length){
						bufPtr = 0; //fuck
					}
					System.arraycopy(message, 0, currentFrame, bufPtr, length);
					bufPtr += length;					
				}
				
				// lets have a look for a header
				for(int i = bufPtr - message.length - 10; i < bufPtr; i++){
					if(i > 0){					
						if (currentFrame[i] == (byte)0xff && currentFrame[i+1] == (byte)0xff){
							//length
							int len = ((int)currentFrame[i+2] << 8 ) | (int)currentFrame[i+3];
							if(len > 0){
								if(currentFrame[i+4] == (byte)0xFF){
									imgStart = i + 5;
									imgEnd = imgStart + len;
									//ConsoleLogger.log(this, "found image header at " + imgStart + " length  " + len);
									break;
								}			
							}
						}				
					}
				}
				
				
				
			}
			socket.close();
		} catch (UnknownHostException e) {
			ConsoleLogger.log(this, "cant connect to webcam server");
	   
		} catch  (IOException e) {
			e.printStackTrace();
		} finally {
			connected = false;
			running = false;
		}
		stop();
	}
	
	public PImage getImage(){
		synchronized (lock) {
			if(frameDirty){
				frameDirty = false;
				lastFrame = new PImage (frame);
			}
			return lastFrame;
		}
	}

	private void processFrame(byte[] currentFrame) {
		InputStream in = new ByteArrayInputStream(currentFrame);
		try {
			synchronized (lock) {				
				frame = ImageIO.read(in);
				
			}
			
			//ConsoleLogger.log(this, "image processed");
		} catch (IOException e) {
			ConsoleLogger.log(this, "image exception");
			e.printStackTrace();
		}
	}

}
