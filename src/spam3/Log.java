package spam3;

import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Log {

    static boolean toggle_on = false;
    static RobotController rc = null;
    static boolean debug = false;

    public static void log(String str) {
        if (toggle_on) {
            System.out.println(str);
        }
        else if(rc.getType() == RobotType.POLITICIAN){
            System.out.println(str);
        }
        else if(rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
            System.out.println(str);
        }
    }

    public static void debug(String str){
        if(debug){
            System.out.println(str);
        }
    }
}
