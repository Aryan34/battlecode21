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

	// METHODS FOR SCOUT BOT (which find the boundary of the map)

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
			MapLocation curr = Util.copyLoc(myLoc).add(targetDir);
			while (curr.distanceSquaredTo(myLoc) <= myType.sensorRadiusSquared) {
				if (!rc.onTheMap(curr)) {
					// Save the out of bounds location
					System.out.println("Found the out of bounds location!");
					outOfBounds = Util.copyLoc(curr);
					break;
				}
				curr = curr.add(targetDir);
			}
		}
		// If I haven't found the out of bounds location yet, keep going in that direction
		if(outOfBounds == null){
			Util.setFlag(0);
			nav.goTo(targetLocation);
		}
		else{
			// I've found the OB location, so set my flag to correspond with that
			int purpose = 1;
			int directionCode = 0;
			int borderValue = 0;
			if(targetDir == Direction.WEST) {
				directionCode = 0;
				borderValue = outOfBounds.x + 1;
			}
			if(targetDir == Direction.EAST) {
				directionCode = 1;
				borderValue = outOfBounds.x - 1;
			}
			if(targetDir == Direction.SOUTH) {
				directionCode = 2;
				borderValue = outOfBounds.y + 1;
			}
			if(targetDir == Direction.NORTH) {
				directionCode = 3;
				borderValue = outOfBounds.y - 1;
			}
			assert(borderValue > 0);

			int[] flagArray = {purpose, 4, directionCode, 2, borderValue, 15};
			int flag = Util.concatFlag(flagArray);
			System.out.println("Purpose: " + purpose);
			System.out.println("Direction code: " + directionCode);
			System.out.println("Border value: " + borderValue);
			System.out.println("Setting flag: " + Util.printFlag(flag));
			Util.setFlag(flag);

			// Also since I'm useless now, just go venture out to random places
//			nav.goTo(creatorLoc);
		}

	}




}
