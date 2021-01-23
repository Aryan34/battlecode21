package spam3;

import battlecode.common.*;

// NOTE: Attacking muckrackers have odd influence, scout muckrackers have even influence

public class Muckraker extends Robot {

	Direction createdDir = null;
	MapLocation targetECLoc = null;
	boolean isAttacking;

	public Muckraker (RobotController rc) throws GameActionException {
		super(rc);
		isAttacking = rc.getInfluence() % 2 == 1;
	}

	public void run() throws GameActionException {
		super.run();
		// If you see any slanderers, kill them
		RobotInfo[] nearby = rc.senseNearbyRobots();
		exposeSlanderers(nearby);
		Comms.checkFlag(creatorID);
		checkEnemiesNearby(nearby);
		// TODO: Change this to an actual if statement (maybe when you hit some round number?)
		if(isAttacking && attackTarget != null){
			Log.log("Yam attacking: " + attackTarget.toString());
			runAttack();
		}
		else{
			Log.log("Yam scouting");
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

	public void checkEnemiesNearby(RobotInfo[] nearby) throws GameActionException {
		if(enemySpotted){
			return;
		}
		if(setFlagThisRound){
			return;
		}
		for(RobotInfo info : nearby){
			if(info.getTeam() != myTeam){
				enemySpotted = true;
				int purpose = 2;
				int robotType = 3; // The type corresponding to an unknown enemy robot
				int[] xy = Comms.mapLocationToXY(info.getLocation());
				int inf = Math.min((info.getInfluence() / 100), 15);
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
		// Check if you can sense an out of bounds area
		for(int i = 0; i < 4; i++){
			if(mapBoundaries[i] != 0){ continue; }
			Direction dir = Navigation.cardinalDirections[i];
			MapLocation curr = Util.copyLoc(myLoc).add(dir);
			while (curr.distanceSquaredTo(myLoc) <= myType.sensorRadiusSquared) {
				if (!rc.onTheMap(curr)) {
					// Save the out of bounds location
					Log.log("Found the out of bounds location!");
					// Set flag
					int borderValue = addBoundaryFlag(dir, curr);
					mapBoundaries[i] = borderValue;
					break;
				}
				curr = curr.add(dir);
			}
		}
		// Move strategically / space yourself out compared to other mucks
		if(rc.getCooldownTurns() > 1){
			return;
		}
		double maxHeuristic = Integer.MIN_VALUE;
		Direction bestDir = Direction.CENTER;
		for(Direction dir : Navigation.directions){
			MapLocation nextLoc = myLoc.add(dir);
			if(!rc.canMove(dir) || !rc.canSenseLocation(nextLoc)){
				continue;
			}
			// Don't revisit squares
			if(haveVisited(nextLoc)){
				continue;
			}
			double heuristic = calcSpreadHeuristic(nearby, nextLoc);
			if(heuristic > maxHeuristic){
				maxHeuristic = heuristic;
				bestDir = dir;
			}
			Log.log("Direction: " + dir.toString() + "; heuristic: " + heuristic);
		}
		if(bestDir != Direction.CENTER){
			nav.tryMove(bestDir);
		}
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
		if(targetECLoc == null){
			DetectedInfo detected = Util.getClosestEnemyEC();
			if(detected == null){
				// TODO: Search for closest enemy EC instead of just moving randomly
				nav.tryMove(nav.randomDirection());
			}
			else{
				targetECLoc = detected.loc;
				nav.fuzzyNav(targetECLoc);
			}
		}
		else{
			nav.fuzzyNav(targetECLoc);
		}
	}

}
