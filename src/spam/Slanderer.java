package spam;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Slanderer extends Robot {
	public Slanderer (RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		if (nav.tryMove(nav.randomDirection()))
			System.out.println("I moved!");
	}
}
