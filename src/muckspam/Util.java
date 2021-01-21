package muckspam;

import battlecode.common.*;

import java.util.Arrays;

public class Util {

	static final Direction[] directions = {
			Direction.NORTH,
			Direction.NORTHEAST,
			Direction.EAST,
			Direction.SOUTHEAST,
			Direction.SOUTH,
			Direction.SOUTHWEST,
			Direction.WEST,
			Direction.NORTHWEST,
	};

	static RobotController rc;
	static Robot robot;

	static MapLocation copyLoc(MapLocation loc){ return loc.add(Direction.CENTER); }

	// Finds the EC that spawned you
	static MapLocation findAdjacentEC() throws GameActionException{
		MapLocation myLoc = rc.getLocation();
		for(Direction dir : directions){
			MapLocation adjLoc = myLoc.add(dir);
			if(!rc.canSenseLocation(adjLoc)){
				continue;
			}
			RobotInfo info = rc.senseRobotAtLocation(adjLoc);
			if(info == null){
				continue;
			}
			if(info.team == robot.myTeam && info.type == RobotType.ENLIGHTENMENT_CENTER){
				return info.location;
			}
		}
		System.out.println("Could not find adjacent EC!");
		return null;
	}

	static boolean tryBuild(RobotType type, Direction dir, int influence) throws GameActionException {
		if (rc.canBuildRobot(type, dir, influence)) {
			System.out.println("Built robot of type: " + type.toString() + ", with influence: " + influence);
			rc.buildRobot(type, dir, influence);
			return true;
		}
		System.out.println("Failed to build robot of type: " + type.toString() + ", with influence: " + influence);
		return false;
	}

	static DetectedInfo getClosestEnemyEC(){
		int min_dist = Integer.MAX_VALUE;
		DetectedInfo closest = null;
		for(int i = 0; i < robot.robotLocationsIdx; i++){
			// Find the closest enemy EC
			DetectedInfo detected = robot.robotLocations[i];
			if(detected.team != robot.myTeam.opponent() || detected.type != RobotType.ENLIGHTENMENT_CENTER){
				continue;
			}
			int distance = robot.myLoc.distanceSquaredTo(detected.loc);
			if(distance < min_dist){
				closest = detected;
			}
		}
		return closest;
	}

	static DetectedInfo[] getCorrespondingRobots(Team team, RobotType type, MapLocation loc){

		DetectedInfo[] copy = new DetectedInfo[robot.robotLocationsIdx];
		int count = 0;

		for(int i = 0; i < robot.robotLocationsIdx; i++){
			DetectedInfo info = robot.robotLocations[i];
			if(team != null && info.team != team){
				continue;
			}
			if(type != null && info.type != type){
				continue;
			}
			if(loc != null && !info.loc.equals(loc)){
				continue;
			}
			copy[count] = info;
			count++;
		}
		DetectedInfo[] copy2 = Arrays.copyOfRange(copy, 0, count);
		return copy2;
	}

	static boolean isGridSquare(MapLocation loc, MapLocation ECLoc){
		if(getGridSquareDist(loc, ECLoc) == 1){
			return false;
		}
		int xdiff = Math.abs(loc.x - ECLoc.x);
		int ydiff = Math.abs(loc.y - ECLoc.y);
		return xdiff % 2 == ydiff % 2;
	}

	// returns true if loc2 is CCW to loc1
	static boolean isCCW(MapLocation loc1, MapLocation loc2, MapLocation center){
		// https://gamedev.stackexchange.com/questions/22133/how-to-detect-if-object-is-moving-in-clockwise-or-counterclockwise-direction
		return ((loc1.x - center.x)*(loc2.y - center.y) - (loc1.y - center.y)*(loc2.x - center.x)) > 0;
	}

	static int getGridSquareDist(MapLocation loc, MapLocation ECLoc){
		int diffX = loc.x - ECLoc.x;
		int diffY = loc.y - ECLoc.y;
		return Math.max(Math.abs(diffX), Math.abs(diffY));
	}





}