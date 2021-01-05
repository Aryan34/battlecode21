package bot;
import battlecode.common.*;

public class Robot {
	RobotController rc;
	Navigation nav;
	int turnCount;

	public Robot (RobotController rc) throws GameActionException {
		this.rc = rc;
		Util.rc = rc;
		Util.robot = this;
		nav = new Navigation(rc, this);
	}

	public void run() throws GameActionException {
		turnCount += 1;
	}
}
