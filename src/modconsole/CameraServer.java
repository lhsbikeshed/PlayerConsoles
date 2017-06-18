package modconsole;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import common.ConsoleLogger;
import processing.core.PApplet;
import processing.core.PImage;
import processing.video.Capture;

public class CameraServer {

	PApplet parent;
	Capture camCapture;
	
	byte[] lastImageBytes;

	BufferedImage buffImg;
	WritableRaster raster;
	
	Object lock = new Object();
	
	public CameraServer(PApplet p){
		parent = p;
		buffImg = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB);
		
		
		String[] cameras = Capture.list();

		  if (cameras == null) {
		    ConsoleLogger.log(this, "Failed to retrieve the list of available cameras, will try the default...");
		    camCapture = new Capture(parent, 320, 240);
		  } if (cameras.length == 0) {
		    ConsoleLogger.log(this, "There are no cameras available for capture.");
		    
		  } else {
		    ConsoleLogger.log(this, "Available cameras:");
		    for (int i = 0; i < cameras.length; i++) {
		      ConsoleLogger.log(this, cameras[i]);
		    }

		    // The camera can be initialized directly using an element
		    // from the array returned by list():
		    camCapture = new Capture(parent, 320,240, "Microsoft LifeCam VX-5000", 30);
		    // Or, the settings can be defined based on the text in the list
		    //cam = new Capture(this, 640, 480, "Built-in iSight", 30);
		    
		    
		    // Start capturing the images from the camera
		    camCapture.start();
		  }
		  startServer();
	}
	
	public byte[] getImageBytes(){
		synchronized (lock) {
			
			return lastImageBytes;
			
		}
	}
	
	 public void startServer() {
	        final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(10);

	        Runnable serverTask = new Runnable() {
	            @Override
	            public void run() {
	                try {
	                    ServerSocket serverSocket = new ServerSocket(8000);
	                    System.out.println("Waiting for clients to connect...");
	                    while (true) {
	                        Socket clientSocket = serverSocket.accept();
	                        clientProcessingPool.submit(new ClientTask(clientSocket));
	                    }
	                } catch (IOException e) {
	                    System.err.println("Unable to process client request");
	                    e.printStackTrace();
	                }
	            }
	        };
	        Thread serverThread = new Thread(serverTask);
	        serverThread.start();

	    }

	    private class ClientTask implements Runnable {
	        private final Socket clientSocket;
	        private boolean running = false;
	        private ClientTask(Socket clientSocket) {
	            this.clientSocket = clientSocket;
	            running = true;
	        }

	        @Override
	        public void run() {
	        	ConsoleLogger.log(this, "> webcam server accepted client");
	            // Do whatever required to process the client's request
	        	
	            try {	            	
	            	while(running){
	            		byte[] imgData = getImageBytes();
	            		// header = 0XFF 0XFF imgData.len.high imgData.len.low 0xFF
	            		byte[] header = new byte[] {(byte)0xFF, (byte)0xFF, (byte)((imgData.length >> 8) & 255), (byte)(imgData.length & 255), (byte)0xFF};
	            		
	            		clientSocket.getOutputStream().write(header);
	            		clientSocket.getOutputStream().write(imgData);
	            		clientSocket.getOutputStream().flush();
	            		
	            		Thread.sleep(100);
	            	}
	               // clientSocket.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e){
					e.printStackTrace();
				} finally {
					running = false;
					ConsoleLogger.log(this, "Camera end");
					
				}
	        }
	    }
	    
	public void update(){
		
		if(camCapture != null){
			if(camCapture.available()){
				camCapture.read();
				PImage temp = camCapture.get();
				temp.resize(320, 240);
				//read into raster and get jpg bytes
				
				ByteArrayOutputStream bas = new ByteArrayOutputStream();
								
				try {
					ImageIO.write((RenderedImage) temp.getImage(), "JPG", bas );
					//ImageIO.write(buffImg, "JPG", new File("test.jpg"));
					lastImageBytes = bas.toByteArray();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		parent.image(camCapture, 100, 100, 320,240);
	}



}
