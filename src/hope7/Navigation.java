package hope7;

import battlecode.common.*;

public class Navigation {
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

	static final Direction[] cardinalDirections = {
			Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH
	};

	static final Direction[] nonCardinalDirections = {
			Direction.NORTHWEST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTHWEST
	};

	static boolean isCardinal(Direction dir){
		for(Direction card : cardinalDirections){
			if(card.equals(dir)){
				return true;
			}
		}
		return false;
	}

	static Direction[] closeDirections(Direction dir){
		Direction[] close = {
				dir,
				dir.rotateLeft(),
				dir.rotateRight(),
				dir.rotateLeft().rotateLeft(),
				dir.rotateRight().rotateRight(),
				dir.rotateLeft().rotateLeft().rotateLeft(),
				dir.rotateRight().rotateRight().rotateRight(),
				dir.opposite()
		};
		return close;
	}

	static Direction[] getCCWFromStart(Direction dir) throws GameActionException {
		Direction[] dirs = new Direction[8];
		Direction temp = dir;
		for(int i = 0; i < 8; i++){
			dirs[i] = temp;
			temp = temp.rotateRight();
		}
		return dirs;
	}

	static Direction[] randomizedDirs(){
		return Util.shuffleArr(directions);
	}

	static double getAngleDiff(MapLocation center, MapLocation loc1, MapLocation loc2){
		double angle1 = Math.atan2(loc1.y - center.y, loc1.x - center.x);
		double angle2 = Math.atan2(loc2.y - center.y, loc2.x - center.x);
		double diff = angle1 - angle2;
		diff = diff * 180 / Math.PI;
		if(diff < 0){
			diff += 360;
		}
		if(diff > 180){
			diff = 360 - diff;
		}
		return diff;
	}

	RobotController rc;
	Robot robot;

	public Navigation(RobotController rc, Robot robot) {
		this.rc = rc;
		this.robot = robot;
	}

	/**
	 * Returns a random Direction.
	 *
	 * @return a random Direction
	 */
	public Direction randomDirection() {
		return directions[(int) (Math.random() * directions.length)];
	}

	/**
	 * Attempts to move in a given direction.
	 *
	 * @param dir The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */

	public boolean tryMove(Direction dir) throws GameActionException {
		Log.debug("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		} else return false;
	}

	public boolean tryMove(Direction[] dirs) throws GameActionException {
		for(Direction dir : dirs){
			if(tryMove(dir)){
				return true;
			}
		}
		return false;
	}

	public boolean goTo(MapLocation target) throws GameActionException {
		if (robot.myLoc.equals(target)) {
			return true;
		}
		rc.setIndicatorLine(robot.myLoc, target, 0, 255, 0);
		if (!rc.isReady()) {
			return false;
		}
		Direction toGo = fuzzyNav(target);
		if (toGo == null) {
			return false;
		}
		if (tryMove(toGo)) {
			goTo(target);
			return true;
		}
		return false;
	}

	public Direction fuzzyNav(MapLocation target) throws GameActionException {
		if (rc.getType() == RobotType.MUCKRAKER && rc.canMove(rc.getLocation().directionTo(target))) {
			return rc.getLocation().directionTo(target);
		}
		double[] distances = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
		MapLocation myLoc = robot.myLoc;
		Direction targetDir = myLoc.directionTo(target);
		MapLocation[] testLocs = {myLoc.add(targetDir), myLoc.add(targetDir.rotateLeft()), myLoc.add(targetDir.rotateRight()),
				myLoc.add(targetDir.rotateLeft().rotateLeft()).add(targetDir.rotateLeft()), myLoc.add(targetDir.rotateRight().rotateRight()).add(targetDir.rotateRight())};

		Direction[] correspondingDirections = {targetDir, targetDir.rotateLeft(), targetDir.rotateRight(), targetDir.rotateLeft().rotateLeft(), targetDir.rotateRight().rotateRight()};
		for (int i = 0; i < testLocs.length; i++) {
			MapLocation testLoc = testLocs[i];
			if (!rc.canSenseLocation(testLoc) || rc.isLocationOccupied(testLoc)) {
				continue;
			}
			distances[i] = 1 / rc.sensePassability(testLoc);
			if (i == 3) {
				MapLocation testLoc2 = myLoc.add(targetDir.rotateLeft().rotateLeft());
				if (!rc.canSenseLocation(testLoc2) || rc.isLocationOccupied(testLoc2)) {
					distances[i] = Double.MAX_VALUE;
					continue;
				}
				distances[i] += 1 / rc.sensePassability(testLoc2);
			}
			if (i == 4) {
				MapLocation testLoc2 = myLoc.add(targetDir.rotateRight().rotateRight());
				if (!rc.canSenseLocation(testLoc2) || rc.isLocationOccupied(testLoc2)) {
					distances[i] = Double.MAX_VALUE;
					continue;
				}
				distances[i] += 1 / rc.sensePassability(testLoc2);
			}
		}

		// TODO: Don't revisit squares

		double minVal = min(distances);
		if (minVal == Double.MAX_VALUE) {
			Log.log(" Ran into a massive obstacle, need to turn around!! ");
			return null;
		}

		int minIdx = indexOf(distances, minVal);
		return correspondingDirections[minIdx];
	}

	public void brownian() throws GameActionException {
		double netX = 0;
		double netY = 0;
		double robotCharge = 100;

		for (RobotInfo info : rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, rc.getTeam())) {
			if (info.getType() == rc.getType() || info.getType() == RobotType.SLANDERER || rc.getType() == RobotType.MUCKRAKER) {
				double force = robotCharge / rc.getLocation().distanceSquaredTo(info.getLocation());
				double magnitude = Math.sqrt(rc.getLocation().distanceSquaredTo(info.getLocation()));
				double dx = (rc.getLocation().x - info.getLocation().x) * force / magnitude;
				double dy = (rc.getLocation().y - info.getLocation().y) * force / magnitude;
				netX += dx;
				netY += dy;
			}
		}
		Log.log("After repelling off teammates, my dp is: " + netX + ", " + netY);
		// Bounce off walls
		double wallForce = 500;
		for(Direction dir : Navigation.cardinalDirections){
			MapLocation reachLoc = robot.myLoc.add(Direction.CENTER);
			reachLoc = reachLoc.add(dir);
			while(robot.myLoc.distanceSquaredTo(reachLoc) <= robot.myType.sensorRadiusSquared){
				if(robot.myLoc.distanceSquaredTo(reachLoc) < robot.myType.sensorRadiusSquared && !rc.onTheMap(reachLoc)){
					Log.log("Reach loc: " + reachLoc.toString());
					double force = wallForce / rc.getLocation().distanceSquaredTo(reachLoc);
					double magnitude = robot.myLoc.distanceSquaredTo(reachLoc);
					double dx = (rc.getLocation().x - reachLoc.x) * force / magnitude;
					double dy = (rc.getLocation().y - reachLoc.y) * force / magnitude;
					netX += dx;
					netY += dy;
					break;
				}
				reachLoc = reachLoc.add(dir);
			}
		}

		Log.log("After bouncing off walls, my dp is: " + netX + ", " + netY);

		double ECForce = -500;
		if(rc.getLocation().distanceSquaredTo(robot.creatorLoc) < RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared){
			ECForce = -ECForce;
		}
		double force = ECForce / rc.getLocation().distanceSquaredTo(robot.creatorLoc);
		double magnitude = robot.myLoc.distanceSquaredTo(robot.creatorLoc);
		double dx = (rc.getLocation().x - robot.creatorLoc.x) * force / magnitude;
		double dy = (rc.getLocation().y - robot.creatorLoc.y) * force / magnitude;
		netX += dx;
		netY += dy;


		int x = (int)Math.round(netX);
		int y = (int)Math.round(netY);
		MapLocation destination = robot.myLoc.translate(x, y);
		if(destination.equals(robot.myLoc)){
			destination = robot.creatorLoc;
		}
		if(!robot.myLoc.equals(destination)){
			goTo(destination);
		}
	}

	public int indexOf(double[] arr, double val) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == val) {
				return i;
			}
		}
		return -1;
	}

	public double min(double[] arr) {
		double min = arr[0];
		for (double m : arr) {
			if (m < min) {
				min = m;
			}
		}
		return min;
	}

	public boolean tryCCWFromStart(Direction dir) throws GameActionException {
		Direction[] order = getCCWFromStart(dir);
		for(Direction temp : order){
			if(tryMove(temp)){
				return true;
			}
		}
		return false;
	}

	public Direction rotateCW(Direction dir){ return dir.rotateLeft().rotateLeft(); }
	public Direction rotateCCW(Direction dir){ return dir.rotateRight().rotateRight(); }

	public void circle(boolean ccw, int minDist) throws GameActionException {
		MapLocation myLoc = robot.myLoc; MapLocation center = robot.creatorLoc;
		int dx = myLoc.x - center.x;
		int dy = myLoc.y - center.y;
		double cs = Math.cos(ccw ? 0.5 : -0.5);
		double sn = Math.sin(ccw ? 0.5 : -0.5);
		int x = (int) (dx * cs - dy * sn);
		int y = (int) (dx * sn + dy * cs);
		MapLocation target = center.translate(x, y);
//		goTo(target);
		Direction targetDir = myLoc.directionTo(target);
		Direction[] options = {targetDir, targetDir.rotateRight(), targetDir.rotateLeft(), targetDir.rotateRight().rotateRight(), targetDir.rotateLeft().rotateLeft()};
		tryMove(options);
	}
}
