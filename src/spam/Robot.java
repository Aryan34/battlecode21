package spam;

import battlecode.common.*;

public class Robot {
	RobotController rc;
	Navigation nav;
	int turnCount;
	MapLocation myLoc;
	MapLocation creatorLoc;
	Team myTeam;
	RobotType myType;

	public Robot (RobotController rc) throws GameActionException {
		// Initialize classes
		this.rc = rc;
		Util.rc = rc;
		Util.robot = this;
		nav = new Navigation(rc, this);
		myTeam = rc.getTeam();
		myType = rc.getType();
		// Find the location of the EC that spawned you
		creatorLoc = Util.findAdjacentEC();
	}

	public void run() throws GameActionException {
		turnCount += 1;
		myLoc = rc.getLocation();
	}
}
