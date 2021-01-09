package spam;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

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

	RobotController rc;
	Robot robot;

	public Navigation(RobotController rc, Robot robot){
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
		rc.setIndicatorLine(robot.myLoc, target, 0, 255, 0);
		Direction toGo = fuzzyNav(target);
		if(toGo == null){
			return false;
		}
		if(tryMove(toGo)){
			return true;
		}
		return false;
	}

	public boolean goTo2(MapLocation target) throws GameActionException {
		rc.setIndicatorLine(robot.myLoc, target, 0, 255, 0);
		Direction targetDir = robot.myLoc.directionTo(target);
		Direction[] toTry = {targetDir, targetDir.rotateLeft(), targetDir.rotateRight(), targetDir.rotateLeft().rotateLeft(), targetDir.rotateRight().rotateRight(), targetDir.opposite().rotateLeft(), targetDir.opposite().rotateRight(), targetDir.opposite()};
		for(Direction dir : toTry){
			if(tryMove(dir)){
				return true;
			}
		}
		return false;
	}

	// Returns the distance to the next row
	public double[] srikarRecur(MapLocation[] locs, double[] costs, Direction targetDir) throws GameActionException {
		double[][] nextPossibleCosts = new double[locs.length][3];
		Direction[] targetDirs = {targetDir.rotateLeft(), targetDir, targetDir.rotateRight()};

		// Create an array for the next row of locations
		MapLocation[] nextLocs = new MapLocation[locs.length + 2];
		nextLocs[0] = locs[0].add(targetDir.rotateLeft());
		nextLocs[nextLocs.length - 1] = locs[locs.length - 1].add(targetDir.rotateRight());
		for(int i = 0; i < locs.length; i++){
			nextLocs[i + 1] = locs[i].add(targetDir);
		}

		// Create an array for the costs and set them all to -1 to start (-1 means unreachable)
		double[] nextCosts = new double[nextLocs.length];
		for(int i = 0; i < nextCosts.length; i++){
			nextCosts[i] = -1;
		}

		// Find the smallest cost to get to a square (not including passability of that square)
		for(int idx = 0; idx < locs.length; idx++){
			// This place is unreachable, so you wouldn't be able to go anywhere from here in the first place.
			if(costs[idx] == -1){
				continue;
			}
			for(int nextIdx = idx; nextIdx < idx + 2; nextIdx += 1){
				if(nextCosts[nextIdx] == -1 || costs[idx] < nextCosts[nextIdx]){
					nextCosts[nextIdx] = costs[idx];
				}
			}
		}

		// Add in passability
		for(int i = 0; i < nextCosts.length; i++){
			if(nextCosts[i] == -1){
				continue;
			}
			if(!rc.canSenseLocation(nextLocs[i]) || rc.isLocationOccupied(nextLocs[i])){
				nextCosts[i] = -1;
				continue;
			}
			double passability = rc.sensePassability(nextLocs[i]);
			nextCosts[i] += (1 / passability);
		}
		return nextCosts;
	}

	public void srikarNav(){

		double[][] distArr = new double[10][5]; // x, y

		// My distance to middle
		double maxRadiusSquared = (double)robot.myType.sensorRadiusSquared;
		double sqrt = Math.floor(Math.sqrt(maxRadiusSquared)); // Max distance forward that I can see
		double half = Math.floor(sqrt / 2); // Distance to halfway line

		double[] distToMiddle = new double[11]; // Math.sqrt(30 - 5)

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
		for(int i = 0; i < testLocs.length; i++){
			MapLocation testLoc = testLocs[i];
			Direction correspondingDir = correspondingDirections[i];
			if(!rc.canSenseLocation(testLoc) || rc.isLocationOccupied(testLoc)){
				continue;
			}
			distances[i] = 1 / rc.sensePassability(testLoc);
			if(i == 3){
				MapLocation testLoc2 = myLoc.add(targetDir.rotateLeft().rotateLeft());
				if(!rc.canSenseLocation(testLoc2) || rc.isLocationOccupied(testLoc2)){
					distances[i] = Double.MAX_VALUE;
					continue;
				}
				distances[i] += 1 / rc.sensePassability(testLoc2);
			}
			if(i == 4){
				MapLocation testLoc2 = myLoc.add(targetDir.rotateRight().rotateRight());
				if(!rc.canSenseLocation(testLoc2) || rc.isLocationOccupied(testLoc2)){
					distances[i] = Double.MAX_VALUE;
					continue;
				}
				distances[i] += 1 / rc.sensePassability(testLoc2);
			}
		}

		double minVal = min(distances);
		if(minVal == Double.MAX_VALUE){
			System.out.println(" Ran into a massive obstacle, need to turn around!! ");
			return null;
		}

		int minIdx = indexOf(distances, minVal);
		return correspondingDirections[minIdx];
	}

	public Direction[] getFuzzyDirections(Direction targetDir){
		Direction[] toTry = {targetDir, targetDir.rotateLeft(), targetDir.rotateRight(), targetDir.rotateLeft().rotateLeft(), targetDir.rotateRight().rotateRight()};
		return toTry;
	}

	public int indexOf(double[] arr, double val){
		for(int i = 0; i < arr.length; i++){
			if(arr[i] == val){
				return i;
			}
		}
		return -1;
	}

	public double min(double[] arr){
		double min = arr[0];
		for(double m : arr){
			if(m < min){
				min = m;
			}
		}
		return min;
	}






}
