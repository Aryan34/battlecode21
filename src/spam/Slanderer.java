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
	}

	void goToGrid() throws GameActionException {
		if(rc.getCooldownTurns() > 1){
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
		if(nav.tryMove(spawnDir.rotateLeft().rotateLeft())){
			return;
		}
		if(nav.tryMove(spawnDir.rotateRight().rotateRight())){
			return;
		}
	}

	void maintainGrid() throws GameActionException {
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
			if(newDist == dist - 1 && newDist != 1){ // Go closer to the EC if you can, but avoid going right next to it (so it still has place to spawn other troops)
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
				nav.tryMove(myLoc.directionTo(option1));
			}
			else if(option2 != null && Util.isCCW(myLoc, option2, creatorLoc)){
				nav.tryMove(myLoc.directionTo(option2));
			}
			return;
		}
		else if(foundSameDist){
			MapLocation option1 = sameDistLocs[0]; MapLocation option2 = sameDistLocs[1];
			if(Util.isCCW(myLoc, option1, creatorLoc)) {
				nav.tryMove(myLoc.directionTo(option1).rotateRight());
			}
			else if(option2 != null && Util.isCCW(myLoc, option2, creatorLoc)){
				nav.tryMove(myLoc.directionTo(option2).rotateRight());
			}
			return;
		}
	}

}
