package spam;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public strictfp class RobotPlayer {
	/**
	 * run() is the method that is called when a robot is instantiated in the Battlecode world.
	 * If this method returns, the robot dies!
	 **/
	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {
		// This is the RobotController object. You use it to perform actions from this robot,
		// and to get information on its current status.

		// System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
		Robot myRobot;
		switch (rc.getType()) {
			case ENLIGHTENMENT_CENTER:
				myRobot = new EnlightenmentCenter(rc);
				break;
			case POLITICIAN:
				myRobot = new Politician(rc);
				break;
			case SLANDERER:
				myRobot = new Slanderer(rc);
				break;
			case MUCKRAKER:
				myRobot = new Muckraker(rc);
				break;
			default:
				myRobot = new Robot(rc);
				break;
		}

		// System.out.println("I'm a " + rc.getType() + " and I just got created!");
		while (true) {
			// Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
			try {
				myRobot.run();
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();
			} catch (Exception e) {
				// System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			}
		}
	}
}
