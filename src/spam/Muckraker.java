package spam;

import battlecode.common.*;

public class Muckraker extends Robot {

	Direction targetDir;
	MapLocation targetLocation;
	MapLocation outOfBounds;
	FlagObj[] flagQueue;
	int flagQueueIdx;
	MapLocation targetECLoc;

	public Muckraker (RobotController rc) throws GameActionException {
		super(rc);
		flagQueue = new FlagObj[20];
		for(int i = 0; i < flagQueue.length; i++){
			flagQueue[i] = new FlagObj();
		}
		flagQueueIdx = 0;
		targetECLoc = null;
	}

	public void run() throws GameActionException {
		super.run();
		RobotInfo[] nearby = rc.senseNearbyRobots();
		exposeSlanderers(nearby);
		relayEnemyLocations(nearby);
		if(outOfBounds == null){
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
			Util.setFlag(flagobj.flag);
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
			if(info.getTeam() == myTeam.opponent() && info.getType() == RobotType.ENLIGHTENMENT_CENTER){
				int purpose = 2;
				int robot_type = 0;
				int[] xy = Util.mapLocationToXY(info.getLocation());
				int x = xy[0];
				int y = xy[1];
				int[] flagArray = {purpose, 4, robot_type, 2, x, 7, y, 7};
				int flag = Util.concatFlag(flagArray);
				System.out.println("Setting flag: " + Util.printFlag(flag));
				addFlagToQueue(flag, 2);
			}
		}
	}

	// METHODS FOR SCOUT BOT (which find the boundary of the map)

	public void runScout() throws GameActionException {
		if(turnCount == 1){
//			assert(creatorLoc != null);
			targetDir = creatorLoc.directionTo(myLoc);
			targetLocation = new MapLocation(myLoc.x + targetDir.dx * 100, myLoc.y + targetDir.dy * 100); // 100 is arbitrary
		}
		// Check if you can sense the out of bounds area
		if(outOfBounds == null) {
			MapLocation curr = Util.copyLoc(myLoc).add(targetDir);
			while (curr.distanceSquaredTo(myLoc) <= myType.sensorRadiusSquared) {
				if (!rc.onTheMap(curr)) {
					// Save the out of bounds location
					System.out.println("Found the out of bounds location!");
					outOfBounds = Util.copyLoc(curr);
					break;
				}
				curr = curr.add(targetDir);
			}
		}
		// If I haven't found the out of bounds location yet, keep going in that direction
		if(outOfBounds == null){
			Util.setFlag(0);
			nav.goTo(targetLocation);
		}
		else{
			// I've found the OB location, so set my flag to correspond with that
			int purpose = 1;
			int directionCode = 0;
			int borderValue = 0;
			if(targetDir == Direction.WEST) {
				directionCode = 0;
				borderValue = outOfBounds.x + 1;
			}
			if(targetDir == Direction.EAST) {
				directionCode = 1;
				borderValue = outOfBounds.x - 1;
			}
			if(targetDir == Direction.SOUTH) {
				directionCode = 2;
				borderValue = outOfBounds.y + 1;
			}
			if(targetDir == Direction.NORTH) {
				directionCode = 3;
				borderValue = outOfBounds.y - 1;
			}
			assert(borderValue > 0);

			int[] flagArray = {purpose, 4, directionCode, 2, borderValue, 15};
			int flag = Util.concatFlag(flagArray);
			System.out.println("Purpose: " + purpose);
			System.out.println("Direction code: " + directionCode);
			System.out.println("Border value: " + borderValue);
			System.out.println("Setting flag: " + Util.printFlag(flag));
			addFlagToQueue(flag, 1);


			// Also since I'm useless now, just go venture out to random places
//			nav.goTo(creatorLoc);
		}

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
