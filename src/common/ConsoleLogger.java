package common;

import java.util.Date;
import java.text.SimpleDateFormat;

public class ConsoleLogger {
	
	
	public static void log(Object obj, String text) {
		String t = new SimpleDateFormat("HH:mm:ss").format(new Date());
		System.out.println(t + " - " + obj.getClass().getName() + ": " + text);
	}
}
