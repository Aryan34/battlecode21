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
		if(rc.canSetFlag(flag)){
			rc.setFlag(flag);
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


}
