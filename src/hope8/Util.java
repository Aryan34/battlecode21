package hope8;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Random;

public class Util {

	static final int SLAND_FLAG = 5242880;

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

	static int slandBenefitPerRound(int spawnInfluence){
		return (int)Math.floor(((1/50) + 0.03 * Math.exp(-0.001 * spawnInfluence)) * spawnInfluence);
	}

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
		Log.log("Could not find adjacent EC!");
		return null;
	}

	static boolean tryBuild(RobotType type, Direction dir, int influence) throws GameActionException {
		for(Direction dir2 : robot.avoidDirs){
			if(dir2 != null && dir.equals(dir2)){
				return false;
			}
		}
		if (rc.canBuildRobot(type, dir, influence)) {
			Log.debug("Built robot of type: " + type.toString() + ", with influence: " + influence);
			rc.buildRobot(type, dir, influence);
			robot.lastBuilt = rc.getLocation().add(dir);
			return true;
		}
		Log.debug("Failed to build robot of type: " + type.toString() + ", with influence: " + influence + ", in direction: " + dir.toString());
		return false;
	}

	static boolean tryBuild(RobotType type, Direction[] dirs, int influence) throws GameActionException {
		for(Direction dir : dirs){
			if(tryBuild(type, dir, influence)){
				return true;
			}
		}
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

	static Direction[] shuffleArr(Direction[] arr){
		Random rand = new Random();
		Direction[] copy = new Direction[arr.length];
		for(int i = 0; i < arr.length; i++){
			copy[i] = arr[i];
		}
		for (int i = 0; i < copy.length; i++) {
			int randomIndexToSwap = rand.nextInt(copy.length);
			Direction temp = copy[randomIndexToSwap];
			copy[randomIndexToSwap] = copy[i];
			copy[i] = temp;
		}
		return copy;
	}

	static boolean isSlanderer(int id) throws GameActionException {
//		robot.typeInQuestion = null;
//		Comms.checkFlag(id);
//		return robot.typeInQuestion == RobotType.SLANDERER;
		if(rc.canGetFlag(id)){
			if(rc.getFlag(id) == SLAND_FLAG){
				return true;
			}
		}
		return false;
	}

	// Helper function to figure out how much influence to spawn a troop with
	static int getSpawnInfluence(int troop_min, int troop_max, double div_factor, boolean make_odd, boolean make_even){
		if(troop_min > troop_max){
			return -1;
		}
		System.out.println("SPAWNING");
		System.out.println(rc.getInfluence() / div_factor);
		System.out.println(rc.getInfluence() - troop_min);
		int spawnInfluence = (int)Math.max(troop_min, rc.getInfluence() / div_factor);
		System.out.println(spawnInfluence);
		spawnInfluence = Math.min(spawnInfluence, troop_max);
		System.out.println(spawnInfluence);
		if(spawnInfluence % 2 == 0 && make_odd){
			spawnInfluence -= 1;
		}
		if(spawnInfluence % 2 == 1 && make_even){
			spawnInfluence += 1;
		}
		return spawnInfluence;
	}

	// Scales val from the range (currMin, currMax) to the range (scaledMin, scaledMax)
	static double scaleValue(double currMin, double currMax, double scaledMin, double scaledMax, double val){
		double ratio = (scaledMax - scaledMin) / (currMax - currMin);
		double baseVal = val - currMin;
		double scaledVal = (baseVal * ratio) + scaledMin;
		return scaledVal;
	}



}
