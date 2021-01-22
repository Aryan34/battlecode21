package spam2;

public class Log {
	static boolean toggle_on = false;

	public static void log(String str) {
		if (toggle_on) {
			System.out.println(str);
		}
	}
}
