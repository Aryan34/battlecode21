package spam;

import battlecode.common.*;

public class Muckraker extends Robot {

	Direction createdDir = null;
	FlagObj[] flagQueue = new FlagObj[20];
	int flagQueueIdx = 0;
	MapLocation targetECLoc = null;

	public Muckraker (RobotController rc) throws GameActionException {
		super(rc);
		for(int i = 0; i < flagQueue.length; i++){
			flagQueue[i] = new FlagObj();
		}
	}

	public void run() throws GameActionException {
		super.run();
		// If you see any slanderers, kill them
		RobotInfo[] nearby = rc.senseNearbyRobots();
		exposeSlanderers(nearby);
		Comms.checkFlag(creatorID);
		relayEnemyLocations(nearby);
		// TODO: Change this to an actual if statement (maybe when you hit some round number?)
		if(true){
			runScout();
		}
		else{
			runAttack();
		}
		int minIdx = 0;
		for(int i = 0; i < flagQueue.length; i++){
			if(flagQueue[i].priority < flagQueue[minIdx].priority && !flagQueue[i].added){
				minIdx = i;
			}
		}
		FlagObj flagobj = flagQueue[minIdx];
		if(flagobj.priority != Integer.MAX_VALUE || flagobj.added){
			Comms.setFlag(flagobj.flag);
			flagobj.added = true;
		}
	}

	public void addFlagToQueue(int flag, int priority){
		flagQueue[flagQueueIdx].flag = flag;
		flagQueue[flagQueueIdx].priority = priority;
		flagQueue[flagQueueIdx].added = false;
	}

	public void exposeSlanderers(RobotInfo[] nearby) throws GameActionException {
		for(RobotInfo info : nearby){
			if(rc.canExpose(info.getLocation())){
				rc.expose(info.getLocation());
			}
		}
	}

	public void relayEnemyLocations(RobotInfo[] nearby) throws GameActionException {
		for(RobotInfo info : nearby){
			int purpose = 2;
			int[] xy = Comms.mapLocationToXY(info.getLocation());
			int x = xy[0];
			int y = xy[1];
			// 0: Enemy EC, 1: Friendly EC, 2: Neutral EC, 3: Enemy robot
			if(info.getTeam() == myTeam.opponent()){
				int robot_type = 3; // Detected random enemy robot
				if(info.getType() == RobotType.ENLIGHTENMENT_CENTER){
					robot_type = 0; // Detected enemy EC
				}
				int[] flagArray = {purpose, 4, robot_type, 2, x, 7, y, 7};
				int flag = Comms.concatFlag(flagArray);
				System.out.println("Setting flag: " + Comms.printFlag(flag));
				addFlagToQueue(flag, 2);
			}
			else if(info.getTeam() == Team.NEUTRAL){
				int[] flagArray = {purpose, 4, 2, 2, x, 7, y, 7};
				int flag = Comms.concatFlag(flagArray);
				System.out.println("Setting flag: " + Comms.printFlag(flag));
				addFlagToQueue(flag, 2);
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
					System.out.println("Found the out of bounds location!");
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
		RobotInfo[] nearby = rc.senseNearbyRobots();
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
			System.out.println("Direction: " + dir.toString() + "; heuristic: " + heuristic);
		}
		if(bestDir != Direction.CENTER){
			nav.tryMove(bestDir);
		}

		// TODO: IF YOU'RE CLOSE TO ENEMY EC, ATTACK IT

	}

	// Heuristic used to spread out when searching
	public double calcSpreadHeuristic(RobotInfo[] nearby, MapLocation nextLoc) throws GameActionException {
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
				System.out.println("Edge case: " + sensorDist + ", " + dist1 + ", " + dist2);
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

	public int addBoundaryFlag(Direction boundaryDir, MapLocation outOfBounds) {
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
			addFlagToQueue(flag, 1);
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
