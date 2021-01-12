package spam;

import battlecode.common.*;

public class Politician extends Robot {

	final int WALL_LENGTH = 6;
	MapLocation[] wallCheckLocs;
	int wallCheckIdx;

	public Politician (RobotController rc) throws GameActionException {
		super(rc);
		wallCheckIdx = 0;
		wallCheckLocs = new MapLocation[4];
	}

	public void run() throws GameActionException {
		super.run();
		Util.checkECFlags();
		runDefense();
	}

	public void moveRandom() throws GameActionException {
		System.out.println("Moving randomly");
		nav.tryMove(nav.randomDirection());
	}

	public void runDefense() throws GameActionException {
		if(cornerLoc == null){
			moveRandom();
		}
		else{
			if(myLoc.distanceSquaredTo(cornerLoc) > WALL_LENGTH * WALL_LENGTH * 2) {
				// Go towards the corner if you're not alr there
				System.out.println("Going towards corner");
				nav.goTo(cornerLoc);
			}
			else{
				// Make a wall (half square) around the corner

				boolean standingOnWall = false;
				// Get all the wall locs
				MapLocation[] wallLocs = getWallLocs();
				// Find closest empty location
				int closestDist = Integer.MAX_VALUE;
				MapLocation closest = null;
				for(MapLocation loc : wallLocs){
					int dist = myLoc.distanceSquaredTo(loc);
					if(dist == 0){
						standingOnWall = true;
					}
					if(rc.canSenseLocation(loc) && rc.isLocationOccupied(loc)){
						continue;
					}
					if(dist < closestDist){
						closestDist = dist;
						closest = loc;
					}
				}

				if(!standingOnWall) {
					// Go towards the closest one
					if (closestDist == Integer.MAX_VALUE) {
						// Couldn't find an empty wall location, circle around to look for one
						runAlongWall();
					} else {
						nav.goTo(closest);
					}
				}

			}
		}
	}

	public MapLocation[] getWallLocs(){
		int xoff = cornerLoc.x + WALL_LENGTH;
		if(isCornerXMax){
			xoff = cornerLoc.x - WALL_LENGTH;
		}
		int yoff = cornerLoc.y + WALL_LENGTH;
		if(isCornerYMax){
			yoff = cornerLoc.y - WALL_LENGTH;
		}

		MapLocation[] wallLocs = new MapLocation[WALL_LENGTH * 2 + 1];
		int idx = 0;
		for(int x = Math.min(cornerLoc.x, xoff); x <= Math.max(cornerLoc.x, xoff); x++){
			wallLocs[idx] = new MapLocation(x, yoff);
			idx++;
		}
		for(int y = Math.min(cornerLoc.y, yoff); y <= Math.max(cornerLoc.y, yoff); y++){
			if(y == yoff){
				continue;
			}
			wallLocs[idx] = new MapLocation(xoff, y);
			idx++;
		}

		System.out.println("IDX: " + idx);
		System.out.println("WALL LOCS LENGTH: " + wallLocs.length);
		assert(idx == wallLocs.length);

		wallCheckLocs[0] = new MapLocation(xoff, yoff);
		wallCheckLocs[1] = new MapLocation(cornerLoc.x, yoff);
		wallCheckLocs[2] = new MapLocation(xoff, yoff);
		wallCheckLocs[3] = new MapLocation(xoff, cornerLoc.y);

		return wallLocs;
	}

	public void runAlongWall() throws GameActionException {
		MapLocation currTarget = wallCheckLocs[wallCheckIdx];
		if(myLoc.distanceSquaredTo(currTarget) < 4){
			wallCheckIdx += 1;
			wallCheckIdx %= wallCheckLocs.length;
			runAlongWall();
		}
		else{
			nav.goTo(currTarget);
		}
	}

	// Example bot attacking code
//		Team enemy = rc.getTeam().opponent();
//		int actionRadius = rc.getType().actionRadiusSquared;
//		RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
//		if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
//			System.out.println("empowering...");
//			rc.empower(actionRadius);
//			System.out.println("empowered");
//			return;
//		}
}
