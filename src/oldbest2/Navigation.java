package oldbest2;

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

	public void goToGrid(int minDist) throws GameActionException {
		MapLocation myLoc = robot.myLoc; MapLocation creatorLoc = robot.creatorLoc;
		if(rc.getCooldownTurns() > 1){
			return;
		}

		// If you can move into a non-occupied grid location, go for it
		for(Direction dir : cardinalDirections){
			MapLocation target = myLoc.add(dir);
			if(Util.isGridSquare(target, creatorLoc) && Util.getGridSquareDist(target, creatorLoc) >= minDist && rc.canMove(dir)){
				Log.log("Found a nearby cardinal location: " + target.toString());
				tryMove(dir);
				return;
			}
		}

		// Try moving away from the center
		if(Util.getGridSquareDist(myLoc, creatorLoc) < minDist){
			Log.log("Moving away from center");
			Direction targetDir = creatorLoc.directionTo(myLoc);
			goTo(myLoc.add(targetDir).add(targetDir).add(targetDir).add(targetDir));
		}

		// Find the closest location
		int bestDist = Integer.MAX_VALUE;
		MapLocation bestLoc = null;

		for(int dx = -4; dx <= 4; dx++){
			for(int dy = -4; dy <= 4; dy++){
				MapLocation loc = new MapLocation(myLoc.x + dx, myLoc.y + dy);
				if(!Util.isGridSquare(loc, creatorLoc)){
					continue;
				}
				if(!rc.canSenseLocation(loc) || rc.isLocationOccupied(loc) || !rc.onTheMap(loc)){
					continue;
				}
				int dist = myLoc.distanceSquaredTo(loc);
				if(dist < bestDist){
					bestDist = dist;
					bestLoc = loc;
				}
			}
		}
		if(bestLoc != null){
			Log.log("Going towards: " + bestLoc.toString());
			goTo(bestLoc);
		}
		else{
			// Move outwards?
			Log.log("Moving outwards");
			Direction targetDir = creatorLoc.directionTo(myLoc);
			goTo(myLoc.add(targetDir).add(targetDir).add(targetDir).add(targetDir));
		}
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
