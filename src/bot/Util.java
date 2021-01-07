package bot;
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




}
