package spam;

import battlecode.common.*;

class CornerInfo {
    MapLocation loc;
    int xoff;
    int yoff;

    public CornerInfo(MapLocation loc, int xoff, int yoff){
        this.loc = loc;
        this.xoff = xoff;
        this.yoff = yoff;
    }

    public int getCornerDirection(){
        // 0: bottomleft, 1: bottomright, 2: topleft, 3: topright
        int tempY = (yoff + 1) / 2;
        int tempX = (xoff + 1) / 2;
        return tempY * 2 + tempX;
    }

    public static CornerInfo createCornerInfo(int cornerDirection, MapLocation loc){
        // 0: bottomleft, 1: bottomright, 2: topleft, 3: topright
        System.out.println("Incoming cornerDir: " + cornerDirection);
        int xoff = cornerDirection & 1;
        int yoff = (cornerDirection / 2) & 1;
        System.out.println("Xoff: " + xoff);
        System.out.println("Yoff: " + yoff);
        xoff = xoff * 2 - 1;
        yoff = yoff * 2 - 1;
        return new CornerInfo(loc, xoff, yoff);
    }

    public String toString() {
        return "Location: " + loc.toString() + ", xoff: " + xoff + ", yoff:" + yoff + " cornerDir: " + getCornerDirection();
    }
}

public class Comms {

    static RobotController rc;
    static Robot robot;

    static boolean setFlag(int flag) throws GameActionException {
        if(robot.setFlagThisRound){
            return false;
        }
        if(robot.myFlag == flag){
            // Flag is already set to that
            robot.setFlagThisRound = true;
            return true;
        }
        if(rc.canSetFlag(flag)){
            rc.setFlag(flag);
            robot.myFlag = flag;
            robot.setFlagThisRound = true;
            return true;
        }
        return false;
    }

    static int concatFlag(int[] arr){
        assert(arr.length % 2 == 0);
        int flag = 0;
        int bits = 0;
        for(int i = 0; i < arr.length; i += 2){
            int val = arr[i]; // the number you want to send
            int bitlen = arr[i + 1]; // how many bits you want the number to occupy
            // Add the value to flag
            flag <<= bitlen;
            flag |= val;
            bits += bitlen;
        }
        assert(bits <= 24);
        flag <<= (24 - bits); // Add 0s at the end of the flag
        return flag;
    }

    static int[] parseFlag(int flag){
        int purpose = flag >> 20;
        switch(purpose){
            case 0:
                break;
            case 1: // Scouting
                int[] splits  = {4, 2, 15};
                return splitFlag(flag, splits);
            case 2:
                int[] splits2 = {4, 2, 7, 7};
                return splitFlag(flag, splits2);
            case 3:
                int[] splits3 = {4, 7, 7, 2};
                return splitFlag(flag, splits3);
            case 4:
                int[] splits4 = {4, 7, 7, 2};
                return splitFlag(flag, splits4);
            case 5:
                int[] splits5 = {4, 2};
                return splitFlag(flag, splits5);
            default:
                System.out.println("Unknown flag purpose detected!");
        }
        int[] empty = new int[0];
        return empty;

    }

    static int[] splitFlag(int flag, int[] splits){
        int[] ret = new int[splits.length];
        int before = 0;
        for(int i = 0; i < splits.length; i++){
            // Hopefully this works. It should basically split the flag bitstring by the given split values
            int temp = flag >> (24 - splits[i] - before);
            temp = temp & ((1 << splits[i]) - 1);
            ret[i] = temp;
            before += splits[i];
        }
        assert(before <= 24);
        return ret;
    }


    static String printFlag(int flag){
        String flagString = Integer.toBinaryString(flag);
        for(int i = flagString.length(); i < 24; i++){
            flagString = "0" + flagString;
        }
        return flagString;
    }

    static MapLocation xyToMapLocation(int x, int y){
        int myX = robot.myLoc.x % 128;
        int myY = robot.myLoc.y % 128;
        int diffX = (x - myX) % 128;
        int diffY = (y - myY) % 128;
        if(diffX > 64){
            diffX = diffX - 128;
        }
        if(diffY > 64){
            diffY = diffY - 128;
        }
        return new MapLocation(robot.myLoc.x + diffX, robot.myLoc.y + diffY);
    }

    static int[] mapLocationToXY(MapLocation loc){
        int[] arr = {loc.x % 128, loc.y % 128};
        return arr;
    }

    static void checkFlag(int ID) throws GameActionException {
        if(!rc.canGetFlag(ID)){
            return;
        }
        int flag = rc.getFlag(ID);
        int[] splits = parseFlag(flag);
        if(splits.length == 0){
            return;
        }
        switch(splits[0]){
            case 1: // Scouting
                if(robot.doneScouting){
                    return;
                }
                int dirIdx = splits[1];
                if(robot.mapBoundaries[dirIdx] == 0){
                    robot.mapBoundaries[dirIdx] = splits[2];
                }
                if(robot.mapBoundaries[0] != 0 && robot.mapBoundaries[1] != 0){
                    robot.mapWidth = robot.mapBoundaries[1] - robot.mapBoundaries[0] + 1;
                    System.out.println("Map width: " + robot.mapWidth);
                }
                if(robot.mapBoundaries[2] != 0 && robot.mapBoundaries[3] != 0){
                    robot.mapHeight = robot.mapBoundaries[3] - robot.mapBoundaries[2] + 1;
                    System.out.println("Map height: " + robot.mapHeight);
                }
                if(robot.mapWidth != 0 && robot.mapHeight != 0){
                    robot.doneScouting = true;
                }
                break;
            // Found object
            case 2:
                int idx = splits[1];
                // 0: Enemy EC, 1: Friendly EC, 2: Neutral EC, 3: Enemy robot
                RobotType[] robotTypes = {RobotType.ENLIGHTENMENT_CENTER, RobotType.ENLIGHTENMENT_CENTER, RobotType.ENLIGHTENMENT_CENTER, null}; // "null" means the robot can be of any type (unknown)
                Team[] robotTeams = {robot.myTeam.opponent(), robot.myTeam, Team.NEUTRAL, robot.myTeam.opponent()};
                RobotType detectedType = robotTypes[idx];
                Team detectedTeam = robotTeams[idx];

                MapLocation detectedLoc = xyToMapLocation(splits[2], splits[3]);
                DetectedInfo[] savedLocations = Util.getCorrespondingRobots(null, null, detectedLoc);
                if(detectedType == RobotType.ENLIGHTENMENT_CENTER && (detectedTeam == robot.myTeam.opponent() || detectedTeam == Team.NEUTRAL)) {
                    if(robot.myType == RobotType.ENLIGHTENMENT_CENTER){
                        if(robot.attackTarget == null){
                            System.out.println("BROADCASTING TARGET EC LOCATION");
                            rc.setFlag(flag);
                            robot.attackTarget = detectedLoc;
                        }
                    }
                    else{
                        System.out.println("FOUND A TARGET EC!");
                        robot.attackTarget = detectedLoc;
                    }
                }
                if(detectedTeam == robot.myTeam.opponent()){
                    robot.enemySpotted = true;
                }
                if(savedLocations.length == 0){
                    robot.robotLocations[robot.robotLocationsIdx] = new DetectedInfo(detectedTeam, detectedType, detectedLoc);
                    robot.robotLocationsIdx++;
                    System.out.println("Detected new robot of type: " + (detectedType == null ? "Unknown" : detectedType.toString()) + " and of team: " + detectedTeam.toString() + " at: " + detectedLoc.toString());
                }
                else{
                    // Update previously saved robot
                    savedLocations[0].team = detectedTeam;
                    savedLocations[0].type = detectedType;
                }
                break;
            case 3: // Corner location to hide in
                System.out.println("GETTING CORNER LOC FROM EC");
                robot.targetCorner = CornerInfo.createCornerInfo(splits[3], xyToMapLocation(splits[1], splits[2]));
                break;
            case 4:
                // TODO: Fill this out
                break;
            case 5:
//                System.out.println("Reading troop type");
                int typeNum = splits[1];
                if(typeNum == 0){ robot.typeInQuestion = RobotType.SLANDERER; }
                else if(typeNum == 1){ robot.typeInQuestion = RobotType.POLITICIAN; }
                else if(typeNum == 2){ robot.typeInQuestion = RobotType.MUCKRAKER; }
                else if(typeNum == 3){ robot.typeInQuestion = RobotType.ENLIGHTENMENT_CENTER; }
        }
    }
}
