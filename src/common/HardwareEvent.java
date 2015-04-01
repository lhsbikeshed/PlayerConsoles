package common;

public class HardwareEvent {
	
	
	
	public enum EventSource {
		KEYBOARD, SERIAL
	}

	public String event;		//event type eg: KEY, SWITCH, CONNECTION
	public int id;				//id of the event source eg: 1, DIALB
	public int value;		//value of the event type, for buttons this is 1 for push and 0 for release
	
	//public Object data;			
	public String originalData;
	public String fromDevice;

	public EventSource eventSource;;

}
