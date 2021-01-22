package spam2;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class Log {

    static boolean toggle_on = false;
    static RobotController rc = null;

    public static void log(String str) {
        if (toggle_on) {
            System.out.println(str);
        }
        else if(rc.getType() == RobotType.POLITICIAN){
            System.out.println(str);
        }
    }
}
