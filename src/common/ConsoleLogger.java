package common;

public class ConsoleLogger {
	public static void log(Object obj, String text) {

		System.out.println(obj.getClass().getName() + " - " + text);
	}
}
