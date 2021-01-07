package spam;

import battlecode.common.*;

public class Muckraker extends Robot {

	Direction targetDir;
	MapLocation targetLocation;
	MapLocation outOfBounds;

	public Muckraker (RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		runScout();
	}

	public void runRandom() throws GameActionException {
		Team enemy = rc.getTeam().opponent();
		int actionRadius = rc.getType().actionRadiusSquared;
		for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
			if (robot.type.canBeExposed()) {
				// It's a slanderer... go get them!
				if (rc.canExpose(robot.location)) {
					System.out.println("e x p o s e d");
					rc.expose(robot.location);
					return;
				}
			}
		}
		if (nav.tryMove(nav.randomDirection())) {
			System.out.println("I moved!");
		}
	}

	public void runScout() throws GameActionException {
		if(creatorLoc == null){
			return;
		}
		if(turnCount == 1){
//			assert(creatorLoc != null);
			targetDir = creatorLoc.directionTo(myLoc);
			targetLocation = new MapLocation(myLoc.x + targetDir.dx * 100, myLoc.y + targetDir.dy * 100); // 100 is arbitrary
		}
		// Check if you can sense the out of bounds area
		if(outOfBounds == null) {
			MapLocation curr = Util.copyLoc(myLoc);
			boolean found = false;
			while (curr.distanceSquaredTo(myLoc) <= myType.sensorRadiusSquared) {
				curr.add(targetDir);
				if (!rc.onTheMap(curr)) {
					outOfBounds = Util.copyLoc(curr);
				}
				break;
			}
		}
		// If I haven't found the out of bounds location yet, keep going in that direction
		if(outOfBounds == null){
			nav.goTo(targetLocation);
		}
		else{
			// I've found the OB location, so report back to home base
			nav.goTo(creatorLoc);
		}


	}



}
