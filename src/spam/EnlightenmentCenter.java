package spam;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class EnlightenmentCenter extends Robot {

	int[] scoutSpawnedIn;

	public EnlightenmentCenter(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		spawnScouts();
	}

	public void spawnRandom() throws GameActionException {
		RobotType toBuild = RobotType.MUCKRAKER;
		int influence = 50;
		for (Direction dir : Util.directions) {
			if (rc.canBuildRobot(toBuild, dir, influence)) {
				rc.buildRobot(toBuild, dir, influence);
			}
			else {
				break;
			}
		}
	}

	public void spawnScouts() throws GameActionException {
		Direction[] spawnDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
		int influence = 1;
		for (int i = 0; i < spawnDirections.length; i++) {
			if(scoutSpawnedIn[i] != 0){
				continue;
			}
			Direction spawnDir = spawnDirections[i];
			if(Util.tryBuild(RobotType.MUCKRAKER, spawnDir, influence)){
				scoutSpawnedIn[i] = turnCount;
			}
		}
	}
}
