package bot;
import battlecode.common.*;

public class EnlightenmentCenter extends Robot {
	public EnlightenmentCenter(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		RobotType toBuild = Util.randomSpawnableRobotType();
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
}
