package spam;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

enum Rotation {
	Clockwise, Counterclockwise;
}

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
		System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		} else return false;
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

	public boolean goTo2(MapLocation target) throws GameActionException {
		rc.setIndicatorLine(robot.myLoc, target, 0, 255, 0);
		Direction targetDir = robot.myLoc.directionTo(target);
		Direction[] toTry = {targetDir, targetDir.rotateLeft(), targetDir.rotateRight(), targetDir.rotateLeft().rotateLeft(), targetDir.rotateRight().rotateRight(), targetDir.opposite().rotateLeft(), targetDir.opposite().rotateRight(), targetDir.opposite()};
		for (Direction dir : toTry) {
			if (tryMove(dir)) {
				return true;
			}
		}
		return false;
	}

	public Direction fuzzyNav(MapLocation target) throws GameActionException {

		double[] distances = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
		MapLocation myLoc = robot.myLoc;
		Direction targetDir = myLoc.directionTo(target);
		MapLocation[] testLocs = {myLoc.add(targetDir),
				myLoc.add(targetDir.rotateLeft()),
				myLoc.add(targetDir.rotateRight()),
				myLoc.add(targetDir.rotateLeft().rotateLeft()).add(targetDir.rotateLeft()),
				myLoc.add(targetDir.rotateRight().rotateRight()).add(targetDir.rotateRight())};

		Direction[] correspondingDirections = {targetDir,
				targetDir.rotateLeft(),
				targetDir.rotateRight(),
				targetDir.rotateLeft().rotateLeft(),
				targetDir.rotateRight().rotateRight()};
		for (int i = 0; i < testLocs.length; i++) {
			MapLocation testLoc = testLocs[i];
			Direction correspondingDir = correspondingDirections[i];
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

		double minVal = min(distances);
		if (minVal == Double.MAX_VALUE) {
			System.out.println(" Ran into a massive obstacle, need to turn around!! ");
			return null;
		}

		int minIdx = indexOf(distances, minVal);
		return correspondingDirections[minIdx];
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

}
