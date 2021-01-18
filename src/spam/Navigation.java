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

	public void goToGrid(int minDist) throws GameActionException {
		MapLocation myLoc = robot.myLoc; MapLocation creatorLoc = robot.creatorLoc;
		if(rc.getCooldownTurns() > 1){
			return;
		}
		// If you didn't move away from the EC to your proper distance, then do so
		if(Util.getGridSquareDist(myLoc, creatorLoc) < minDist){
			Direction awayDir = creatorLoc.directionTo(myLoc);
			// Move away from the EC
			goTo(myLoc.add(awayDir).add(awayDir).add(awayDir).add(awayDir));
			return;
		}

		// Move to the best spot
		Direction bestCard = Direction.CENTER;
		Direction bestNonCard = Direction.CENTER;
		int bestCardDist = Integer.MAX_VALUE;
		for(Direction dir : directions){
			MapLocation nextLoc = myLoc.add(dir);
			int newDist = Util.getGridSquareDist(nextLoc, creatorLoc);
			if(!rc.canMove(dir)){
				continue;
			}
			if(isCardinal(dir)){
				// Move onto a grid square if you can
				if(newDist > minDist && newDist < bestCardDist){
					bestCardDist = newDist;
					bestCard = dir;
				}
			}
			else{
				// Go CCW
				if(newDist > minDist && Util.isCCW(myLoc.add(bestNonCard), myLoc.add(dir), creatorLoc)){
					bestNonCard = dir;
				}
			}
		}
		if(bestCard != Direction.CENTER){
			tryMove(bestCard);
			return;
		}
		if(bestNonCard != Direction.CENTER){
			tryMove(bestNonCard);
		}


//		Direction[] tryOrder = {spawnDir, spawnDir.rotateLeft(), spawnDir.rotateRight(), spawnDir.rotateLeft().rotateLeft(), spawnDir.rotateRight().rotateRight()};
//		for(Direction dir : tryOrder){
//			if(Util.isGridSquare(myLoc.add(dir), creatorLoc)){
//				if(tryMove(dir)){
//					return;
//				}
//			}
//		}
//		for(Direction dir : tryOrder){
//			if(tryMove(dir)){
//				return;
//			}
//		}
	}


	public void maintainGrid(int minDist) throws GameActionException {
		MapLocation myLoc = robot.myLoc; MapLocation creatorLoc = robot.creatorLoc;
		int dist = Util.getGridSquareDist(myLoc, creatorLoc);
		MapLocation[] closerLocs = new MapLocation[2];
		MapLocation[] sameDistLocs = new MapLocation[2];
		boolean foundCloser = false; boolean foundSameDist = false;
		// See if you can go towards an inner circle
		for(Direction dir : Navigation.directions){
			MapLocation newLoc = myLoc.add(dir);
			if(Navigation.isCardinal(dir)){
				newLoc = myLoc.add(dir).add(dir);
			}
			if(!rc.onTheMap(newLoc) || rc.isLocationOccupied(newLoc) || !rc.canMove(myLoc.directionTo(newLoc))){
				continue;
			}
			int newDist = Util.getGridSquareDist(newLoc, creatorLoc);
			if(newDist == dist - 1 && newDist >= minDist){ // Go closer to the EC if you can, but avoid going right next to it (so it still has place to spawn other troops)
				int idx = foundCloser ? 1 : 0;
				closerLocs[idx] = newLoc;
				foundCloser = true;
			}
			else if(newDist == dist){
				int idx = foundSameDist ? 1 : 0;
				sameDistLocs[idx] = newLoc;
				foundSameDist = true;
			}
		}
		// If you can, go to the more counterclockwise one
		if(foundCloser){
			MapLocation option1 = closerLocs[0]; MapLocation option2 = closerLocs[1];
			if(Util.isCCW(myLoc, option1, creatorLoc)) {
				tryMove(myLoc.directionTo(option1));
			}
			else if(option2 != null && Util.isCCW(myLoc, option2, creatorLoc)){
				tryMove(myLoc.directionTo(option2));
			}
			return;
		}
		else if(foundSameDist){
			MapLocation option1 = sameDistLocs[0]; MapLocation option2 = sameDistLocs[1];
			if(Util.isCCW(myLoc, option1, creatorLoc)) {
				if(tryMove(myLoc.directionTo(option1).rotateRight())){ return; }
				if(tryMove(myLoc.directionTo(option1))){ return; }
			}
			else if(option2 != null && Util.isCCW(myLoc, option2, creatorLoc)){
				if(tryMove(myLoc.directionTo(option2).rotateRight())){ return; }
				if(tryMove(myLoc.directionTo(option2))){ return; }
			}
			return;
		}

		// TODO: If you've hit an edge, then try going clockwise
		// TODO: Some kind of expansion code
	}


}
