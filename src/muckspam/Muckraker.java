package muckspam;

import battlecode.common.*;

// NOTE: Attacking muckrackers have odd influence, scout muckrackers have even influence

public class Muckraker extends Robot {

	Direction createdDir = null;
	MapLocation targetECLoc = null;
	MapLocation scoutTarget = null;
	boolean isAttacking;

	public Muckraker (RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		// If you see any slanderers, kill them
		moveAwayFromCenter();
		int[] dxDy = getDxDy(muckNum);
		isAttacking = dxDy[0] == 0 && dxDy[1] == 0;
		exposeSlanderers(nearby);
		Comms.checkFlag(creatorID);
		checkEnemiesNearby(nearby);
		// TODO: Change this to an actual if statement (maybe when you hit some round number?)
		if(isAttacking){
			Log.log("Yam attacking");
			runAttack();
		}
		else{
			Log.log("Yam scouting");
//			Log.log("My muck num is: " + muckNum);
			runScout();
		}
		Comms.setFlag(0); // Reset flag to 0 so you don't send stuff multiple times
	}

	public void exposeSlanderers(RobotInfo[] nearby) throws GameActionException {
		for(RobotInfo info : nearby){
			if(rc.canExpose(info.getLocation())){
				rc.expose(info.getLocation());
			}
		}
	}

	public void moveAwayFromCenter() throws GameActionException {
		if(Util.getGridSquareDist(myLoc, creatorLoc) <= 2){
			nav.tryMove(Navigation.closeDirections(myLoc.directionTo(creatorLoc).opposite()));
		}
	}

	public void checkEnemiesNearby(RobotInfo[] nearby) throws GameActionException {
		if(enemySpotted){
			return;
		}
		if(setFlagThisRound){
			return;
		}
		for(int i = 0; i < nearby.length; i++){
			RobotInfo info = nearby[i];
			if(info.getTeam() == myTeam.opponent()){
				enemySpotted = true;
				int purpose = 2;
				int robotType = 3; // The type corresponding to an unknown enemy robot
				int[] xy = Comms.mapLocationToXY(info.getLocation());
				int inf = Math.min((info.getConviction() / 100), 15);
				int[] flagArray = {purpose, 4, robotType, 2, xy[0], 7, xy[1], 7, inf, 4};
				int flag = Comms.concatFlag(flagArray);
				Log.log("Setting flag to: " + Comms.printFlag(flag));
				Comms.setFlag(flag);
				return;
			}
		}
	}


	// METHODS FOR SCOUT BOT (which find the boundary of the map)

	public void runScout() throws GameActionException {
		if(turnCount == 1){
//			assert(creatorLoc != null);
			createdDir = creatorLoc.directionTo(myLoc);
		}

		if (scoutTarget == null) {
			int[] dp = getDxDy(muckNum);
			scoutTarget = creatorLoc.translate(dp[0] * 64, dp[1] * 64);
		}
		Log.log("My scout target: " + scoutTarget.toString());

		// Check if you can sense an out of bounds area
		for(int i = 0; i < 4; i++){
			Direction dir = Navigation.cardinalDirections[i];
			MapLocation curr = Util.copyLoc(myLoc).add(dir);
			while (curr.distanceSquaredTo(myLoc) <= myType.sensorRadiusSquared) {
				if (!rc.onTheMap(curr)) {
					if(mapBoundaries[i] == 0) {
						// Save the out of bounds location
						Log.log("Found the out of bounds location!");
						// Set flag
						int borderValue = addBoundaryFlag(dir, curr);
						mapBoundaries[i] = borderValue;
						break;
					}

					int rand = (int)Math.round(Math.random());
					Direction scoutDir = myLoc.directionTo(scoutTarget);
					if (scoutDir == Direction.EAST && scoutDir == dir) {
						// Go NORTH or SOUTH
						if (rand == 1) {
							scoutTarget = myLoc.translate(0, 64);
						}
						else {
							scoutTarget = myLoc.translate(0, -64);
						}
					}
					else if (scoutDir == Direction.NORTH && scoutDir == dir) {
						// Go EAST or WEST
						if (rand == 1) {
							scoutTarget = myLoc.translate(64, 0);
						}
						else {
							scoutTarget = myLoc.translate(-64, 0);
						}
					}
					else if (scoutDir == Direction.WEST && scoutDir == dir) {
						// Go NORTH or SOUTH
						if (rand == 1) {
							scoutTarget = myLoc.translate(0, 64);
						}
						else {
							scoutTarget = myLoc.translate(0, -64);
						}
					}
					else if (scoutDir == Direction.SOUTH && scoutDir == dir) {
						// Go EAST or WEST
						if (rand == 1) {
							scoutTarget = myLoc.translate(64, 0);
						}
						else {
							scoutTarget = myLoc.translate(-64, 0);
						}
					}
					else if (scoutDir.dx == dir.dx || scoutDir.dy == dir.dy) {
						int oldDX = scoutDir.dx;
						int oldDY = scoutDir.dy;
						Log.log("OLD: " + oldDX + ", " + oldDY);
						Log.log("Direction of wall: " + dir.toString());
						scoutTarget = myLoc.translate((oldDX + dir.opposite().dx) * 64, (oldDY + dir.opposite().dy) * 64);
						Log.log("NEW: " + myLoc.directionTo(scoutTarget).dx + ", " + myLoc.directionTo(scoutTarget).dy);
					}
				}
				curr = curr.add(dir);
			}
		}

		if(rc.getCooldownTurns() > 1){
			return;
		}

		if(Util.getGridSquareDist(myLoc.add(myLoc.directionTo(scoutTarget)), creatorLoc) <= 2){
			// Go around EC loc
			boolean ccw = true;
			// If there's a wall to ur left, then circle CW
			Direction[] testDirs = {myLoc.directionTo(creatorLoc).rotateLeft(), myLoc.directionTo(creatorLoc).rotateLeft().rotateLeft(), myLoc.directionTo(creatorLoc).rotateLeft().rotateLeft().rotateLeft()};
			for(Direction dir : testDirs){
				if(hitsWall(dir)){
					ccw = false;
				}
			}
			nav.circle(ccw, 3);
		}
		nav.goTo(scoutTarget);
	}

	public boolean hitsWall(Direction dir) throws GameActionException {
		MapLocation senseLoc = myLoc.translate(0, 0);
		while(myLoc.distanceSquaredTo(senseLoc) <= myType.sensorRadiusSquared){
			if(!rc.onTheMap(senseLoc)){
				return true;
			}
			senseLoc = senseLoc.add(dir);
		}
		return false;
	}

	// Heuristic used to spread out when searching
	public double calcSpreadHeuristic(RobotInfo[] nearby, MapLocation nextLoc) throws GameActionException {
		//TOOD: add passability to this heuristic
		double passability = rc.sensePassability(nextLoc);
		double heuristic = 0.0;
		int closestEdge = Integer.MAX_VALUE;
		int nextClosestEdge = Integer.MAX_VALUE;
		// Don't move right next to an edge
		int sensorDist = (int)Math.floor(Math.sqrt(myType.sensorRadiusSquared));
		for(int i = 0; i < mapBoundaries.length; i++){
			if(mapBoundaries[i] != 0){
				int dist1 = distanceToEdge(i, myLoc);
				int dist2 = distanceToEdge(i, nextLoc);
				Log.log("Edge case: " + sensorDist + ", " + dist1 + ", " + dist2);
				if(dist1 < sensorDist && dist2 < dist1){
					return Integer.MIN_VALUE + 1;
				}
			}
		}

		// Move away from friendly robots who are also searching
		for(RobotInfo info : nearby){
			if(info.getTeam() == myTeam && info.getType() == RobotType.MUCKRAKER){
				double dist = Math.sqrt(Math.sqrt(nextLoc.distanceSquaredTo(info.location)));
				heuristic += dist;
			}
		}
		// Move away from HQ
		heuristic += Math.sqrt(Math.sqrt(nextLoc.distanceSquaredTo(creatorLoc)));

		// Move away from visited squares
		int minDist = 0;
		for(int i = 0; i < visited.length; i++){
			if(visited[i] == null){ continue; }
			if((i < visitedIdx && i >= visitedIdx - 5) || (visitedIdx < 5 && i > (visitedIdx - 5) % visited.length)) { continue; }
			int dist = visited[i].distanceSquaredTo(nextLoc);
			minDist = Math.min(minDist, dist);
		}

		heuristic += Math.sqrt(Math.sqrt((double)minDist)) * 2;

		// Prefer higher passability squares
//		heuristic += (1 - passability);
		return heuristic;
	}

	public int addBoundaryFlag(Direction boundaryDir, MapLocation outOfBounds) throws GameActionException {
		// I've found the OB location, so set my flag to correspond with that
		int purpose = 1;
		int directionCode = 0;
		int borderValue = 0;
		if(boundaryDir == Direction.WEST) { directionCode = 0; borderValue = outOfBounds.x + 1; }
		if(boundaryDir == Direction.EAST) { directionCode = 1; borderValue = outOfBounds.x - 1; }
		if(boundaryDir == Direction.SOUTH) { directionCode = 2; borderValue = outOfBounds.y + 1; }
		if(boundaryDir == Direction.NORTH) { directionCode = 3; borderValue = outOfBounds.y - 1; }
		assert(borderValue > 0);

		int[] flagArray = {purpose, 4, directionCode, 2, borderValue, 15};
		int flag = Comms.concatFlag(flagArray);
		Comms.setFlag(flag);
		return borderValue;
	}

	public void runAttack() throws GameActionException {
		// Go towards closest enemy EC
		if(attackTarget != null){
			nav.goTo(attackTarget);
		}
		else{
			nav.brownian();
		}
	}

	public int[] getDxDy(int muckNum) throws GameActionException {
		int dx = 0; int dy = 0;
		switch (muckNum) {
			// NORTH
			case 0:
				dx = 0;
				dy = 1;
				break;
			// EAST
			case 1:
				dx = 1;
				dy = 0;
				break;
			// SOUTH
			case 2:
				dx = 0;
				dy = -1;
				break;
			// WEST
			case 3:
				dx = -1;
				dy = 0;
				break;
			// NORTHEAST
			case 4:
				dx = 1;
				dy = 1;
				break;
			// SOUTHEAST
			case 5:
				dx = 1;
				dy = -1;
				break;
			// SOUTHWEST
			case 6:
				dx = -1;
				dy = -1;
				break;
			// NORTHWEST
			case 7:
				dx = -1;
				dy = 1;
				break;
			// SUM OF: EAST, NORTHEAST
			case 8:
				dx = 2;
				dy = 1;
				break;
			// SUM OF: NORTHEAST, NORTH
			case 9:
				dx = 1;
				dy = 2;
				break;
			// SUM OF: NORTH, NORTHWEST
			case 10:
				dx = -1;
				dy = 2;
				break;
			// SUM OF: NORTHWEST, WEST
			case 11:
				dx = -2;
				dy = 1;
				break;
			// SUM OF: WEST, SOUTHWEST
			case 12:
				dx = -2;
				dy = -1;
				break;
			// SUM OF: SOUTHWEST, SOUTH
			case 13:
				dx = -1;
				dy = -2;
				break;
			// SUM OF: SOUTH, SOUTHEAST
			case 14:
				dx = 1;
				dy = -2;
				break;
			// SUM OF: SOUTHEAST, EAST
			case 15:
				dx = 2;
				dy = -1;
				break;
			case 16:
				dx = 3;
				dy = 1;
				break;
			case 17:
				dx = 1;
				dy = 3;
				break;
			case 18:
				dx = -1;
				dy = 3;
				break;
			case 19:
				dx = -3;
				dy = 1;
				break;
			case 20:
				dx = -3;
				dy = -1;
				break;
			case 21:
				dx = -1;
				dy = -3;
				break;
			case 22:
				dx = 1;
				dy = -3;
				break;
			case 23:
				dx = 3;
				dy = -1;
				break;
			default:
				dx = 0;
				dy = 0;
				break;
		}
		int[] arr = {dx, dy};
		return arr;
	}

}
