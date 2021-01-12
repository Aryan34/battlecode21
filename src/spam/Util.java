package spam;

import battlecode.common.*;

public class Util {
	static final RobotType[] spawnableRobot = {
			RobotType.POLITICIAN,
			RobotType.SLANDERER,
			RobotType.MUCKRAKER,
	};

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

	/**
	 * Returns a random spawnable RobotType
	 *
	 * @return a random RobotType
	 */
	static RobotType randomSpawnableRobotType() {
		return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
	}

	static MapLocation copyLoc(MapLocation loc){ return loc.add(Direction.CENTER); }

	static boolean setFlag(int flag) throws GameActionException {
		if(robot.myFlag == flag){
			// Flag is already set to that
			return true;
		}
		if(rc.canSetFlag(flag)){
			rc.setFlag(flag);
			robot.myFlag = flag;
			return true;
		}
		return false;
	}

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
			rc.buildRobot(type, dir, influence);
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
			case 1: // Scouting
				int[] splits  = {4, 2, 15};
				return splitFlag(flag, splits);
			case 2:
				int[] splits2 = {4, 2, 7, 7};
				return splitFlag(flag, splits2);
			case 3:
				int[] splits3 = {4, 7, 7};
				return splitFlag(flag, splits3);
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


}
