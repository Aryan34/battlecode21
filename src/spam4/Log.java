package spam4;

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
//        RobotType[] showLogs = {RobotType.POLITICIAN, RobotType.ENLIGHTENMENT_CENTER};
        RobotType[] showLogs = {};
        for(RobotType type : showLogs){
            if(rc.getType() == type){
                System.out.println(str);
            }
        }
    }

    public static void debug(String str){
        if(debug){
            System.out.println(str);
        }
    }
}
