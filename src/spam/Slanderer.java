package spam;

import battlecode.common.*;

public class Slanderer extends Robot {

	boolean inGrid = false;
	Direction spawnDir;

	public Slanderer (RobotController rc) throws GameActionException {
		super(rc);
		spawnDir = creatorLoc.directionTo(myLoc);
	}

	public void run() throws GameActionException {
		super.run();
		Comms.checkFlag(creatorID);
		runEco();
	}

	public void moveRandom() throws GameActionException {
		System.out.println("Moving randomly");
		nav.tryMove(nav.randomDirection());
	}

	public void runEco() throws GameActionException {
		System.out.println("Am I on the grid? " + Util.isGridSquare(myLoc, creatorLoc));
		if(!inGrid && Util.isGridSquare(myLoc, creatorLoc)){
			inGrid = true;
			return;
		}
		if(!inGrid){
			goToGrid();
		}
		else{
			maintainGrid();
		}

//		if(targetCorner == null){
//			moveRandom();
//		}
//		else{
//			System.out.println("Going towards corner");
//			nav.goTo(targetCorner.loc);
//		}
	}

	void goToGrid() throws GameActionException {
		if(rc.getCooldownTurns() > 1){
			return;
		}
		Direction left = spawnDir.rotateLeft().rotateLeft();
		Direction right = left.opposite();
		if(nav.tryMove(left)){
			return;
		}
		if(nav.tryMove(right)){
			return;
		}
		if(nav.tryMove(spawnDir)){
			return;
		}
		if(nav.tryMove(spawnDir.rotateLeft())){
			return;
		}
		if(nav.tryMove(spawnDir.rotateRight())){
			return;
		}
	}

	void maintainGrid() throws GameActionException {
		int dist = getGridSquareDist(myLoc);
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
			int newDist = getGridSquareDist(newLoc);
			if(newDist == dist - 1){
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
			if(isCCW(myLoc, option1, creatorLoc)) {
				nav.tryMove(myLoc.directionTo(option1));
			}
			else if(option2 != null && isCCW(myLoc, option2, creatorLoc)){
				nav.tryMove(myLoc.directionTo(option2));
			}
			return;
		}
		else if(foundSameDist){
			MapLocation option1 = sameDistLocs[0]; MapLocation option2 = sameDistLocs[1];
			if(isCCW(myLoc, option1, creatorLoc)) {
				// If you've gone in a full circle from when you were spawned, then stop.
//				if(){
//
//				}
				nav.tryMove(myLoc.directionTo(option1).rotateRight());
			}
			else if(option2 != null && isCCW(myLoc, option2, creatorLoc)){
				nav.tryMove(myLoc.directionTo(option2).rotateRight());
			}
			return;
		}
	}

	boolean isCCW(MapLocation loc1, MapLocation loc2, MapLocation center)
	{
		// https://gamedev.stackexchange.com/questions/22133/how-to-detect-if-object-is-moving-in-clockwise-or-counterclockwise-direction
		return ((loc1.x - center.x)*(loc2.y - center.y) - (loc1.y - center.y)*(loc2.x - center.x)) > 0;
	}

	int getGridSquareDist(MapLocation loc) throws GameActionException {
		int diffX = loc.x - creatorLoc.x;
		int diffY = loc.y - creatorLoc.y;
		return Math.max(Math.abs(diffX), Math.abs(diffY));
	}

}
