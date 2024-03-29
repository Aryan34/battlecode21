package spam6;

import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class Log {

    static boolean toggle_on = false;
    static RobotController rc = null;
    static boolean debug = false;

    public static void log(String str) {
        if(rc.getTeam() != Team.A){
            return;
        }
        if (toggle_on) {
            System.out.println(str);
        }
//        RobotType[] showLogs = {RobotType.POLITICIAN, RobotType.ENLIGHTENMENT_CENTER};
        RobotType[] showLogs = {RobotType.POLITICIAN};
//        RobotType[] showLogs = {};
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
