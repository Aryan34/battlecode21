package newScoutingOldBest2;

import battlecode.common.*;

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
        else{
            Log.log("CAN'T SET FLAG TO: " + flag);
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
                int[] splits2 = {4, 2, 7, 7, 4};
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
                Log.log("Unknown flag purpose detected!");
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
        int diffX = (x - myX);
        if(diffX < 0){
            diffX += 128;
        }
        int diffY = (y - myY);
        if(diffY < 0){
            diffY += 128;
        }
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
        if(flag == 0){
            return;
        }
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
                    Log.log("Map width: " + robot.mapWidth);
                }
                if(robot.mapBoundaries[2] != 0 && robot.mapBoundaries[3] != 0){
                    robot.mapHeight = robot.mapBoundaries[3] - robot.mapBoundaries[2] + 1;
                    Log.log("Map height: " + robot.mapHeight);
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
                int detectedInfluence = (splits[4] + 1) * 100;
                DetectedInfo[] savedLocations = Util.getCorrespondingRobots(null, null, detectedLoc);

                if(detectedTeam != robot.myTeam && !robot.enemySpotted){
                    robot.enemySpotted = true;
                    Log.log("ENEMY SPOTTED !!!!!");
                }

                // Don't save it if its not an enlightenment center
                if(detectedType == RobotType.ENLIGHTENMENT_CENTER){
                    if(savedLocations.length == 0){
                        robot.robotLocations[robot.robotLocationsIdx] = new DetectedInfo(detectedTeam, detectedType, detectedLoc, detectedInfluence);
                        robot.robotLocationsIdx++;
                        Log.log("Detected new robot of type: " + (detectedType == null ? "Unknown" : detectedType.toString()) + " and of team: " + detectedTeam.toString() + " at: " + detectedLoc.toString());
                    }
                    else{
                        // Update previously saved robot
                        savedLocations[0].team = detectedTeam;
                        savedLocations[0].type = detectedType;
                    }
                }
                break;
            case 3: // Corner location to hide in
                Log.log("GETTING CORNER LOC FROM EC");
                break;
            case 4:
                if(robot.myType != RobotType.POLITICIAN && robot.myType != RobotType.MUCKRAKER){
                    return;
                }
                int x = splits[1];
                int y = splits[2];
                MapLocation attackLoc = xyToMapLocation(x, y);
                if(robot.myType == RobotType.POLITICIAN && (splits[3] == 0 || splits[3] == 2)){
                    robot.attackTarget = attackLoc;
                }
                if(robot.myType == RobotType.MUCKRAKER && (splits[3] == 1 || splits[3] == 2)){
                    robot.attackTarget = attackLoc;
                }
                if(splits[3] == 3){
                    robot.attackTarget = null;
                }
                if(robot.attackTarget != null){ Log.log("Found an attack target!: " + robot.attackTarget.toString()); }
                else{ Log.log("Not attacking anything :(("); }
                break;
            case 5:
//                Log.log("Reading troop type");
                int typeNum = splits[1];
                if(typeNum == 0){ robot.typeInQuestion = RobotType.SLANDERER; }
                else if(typeNum == 1){ robot.typeInQuestion = RobotType.POLITICIAN; }
                else if(typeNum == 2){ robot.typeInQuestion = RobotType.MUCKRAKER; }
                else if(typeNum == 3){ robot.typeInQuestion = RobotType.ENLIGHTENMENT_CENTER; }
        }
    }
}
