package engineer.reactorsim;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;

import common.ConsoleLogger;
import common.HardwareEvent;
import processing.core.PApplet;
import processing.core.PVector;


/* simulate one "system" in a reactor model
 * produces and stores a "resource"
 */
public abstract class ReactorSystem {
	
	protected PVector screenPosition;
	protected Rectangle bounds;	//this will most likely be removed it prod as its for hit tests with mouse
	protected String name = "Test";
	
	long lastTick = 0;
	
	public enum PowerState{
		STATE_OFF, 				//system is off
		STATE_ON, 				//system is on 
		STATE_COOLING;			//system is in cooldown state. Will not work until cooled
	};
	
	protected PowerState[] runningState = {PowerState.STATE_OFF};
	protected float cooldownTimer = 0f;
	

	public String getName() {
		return name;
	}

	public PVector getScreenPosition() {
		return screenPosition;
	}

	public void setScreenPosition(PVector screenPosition) {
		this.screenPosition = screenPosition;
	}

	protected HashMap<String, ReactorResource> resourceStore = new HashMap<String, ReactorResource>();
	
	protected HashMap<String, ReactorSystem> inboundConnections = new HashMap<String, ReactorSystem>();
	
	
	public ReactorSystem() {
		// TODO Auto-generated constructor stub
	}

	public void tick(){
		if(System.currentTimeMillis() - lastTick > 250){
			lastTick = System.currentTimeMillis();
			for(String key : resourceStore.keySet()){
				ReactorResource r = resourceStore.get(key);
				r.amount += r.amountPerTick;
				if(r.amount < 0){
					r.amount = 0;
					
				} else if (r.amount > r.maxAmount){
					r.amount = r.maxAmount;
				}
			}
		}
	}
		
	
	
	public float consumeResource(String resName, float amount){
			ReactorResource res = resourceStore.get(resName);
			if(res == null){
				return 0;
			}
			
			float amt = res.getAmount() - amount;
			if(amt < 0 ){
				amt = amount + amt;
				//in this case may want to punish this system for trying to draw too much
				//of a resource
			} else {
				amt = amount;
			}
			res.change(-amount);
			

			return amt;

	}
	
	public abstract void controlSignal(HardwareEvent e);
	
	public abstract void draw(PApplet context);
	
	public abstract void applyDamage(float amount);
	
	public void addInboundConnection(ReactorSystem sys){
		ConsoleLogger.log(this, "adding inbound from " + sys.name);
		inboundConnections.put(sys.name, sys);
		
	}
	
	protected void drawResources(PApplet context){
		int p = 0;
		for(String k : resourceStore.keySet()){
			ReactorResource r = resourceStore.get(k);
			
			context.fill(255,255,0);
			float w = PApplet.map(r.getAmount(), 0, r.maxAmount, 0, 100);
			context.rect(50,75+p,w,10);
			context.noFill();
			context.rect(50,75+p,100,10);
			context.fill(255);
			context.text(r.typeTag, 0, 85+p);
			p+= 20;
		}
	}
	
	public boolean hitTest(int mouseX, int mouseY){
		return bounds.contains(mouseX, mouseY);
	}
	
	public class ReactorResource{
		public String typeTag = "NONE";
		private float amount = 0f;
		public float amountPerTick = 0.0f;
		public float maxAmount = 100f;
		
		public float getAmount(){
			return amount;
		}
		public void change(float in){
			amount += in;
			if(amount <= 0){
				amount = 0;
			} else if (amount >= maxAmount){
				amount = maxAmount;
			}
		}
		public void setAmount(float in){
			amount = in;
		}
	}
	
	
	
	
	
}
