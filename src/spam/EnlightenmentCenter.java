package spam;

import battlecode.common.*;

import java.util.HashSet;

public class EnlightenmentCenter extends Robot {

	int[] scoutSpawnedIn;
	int[] spawnedAllies;
	HashSet<Integer> spawnedAlliesSet;
	int numSpawned;
	int[] mapBoundaries; // Format for this is [minX, maxX, minY, maxY], which is also [West, East, South, North]
	int mapWidth;
	int mapHeight;
	int lastBid;
	boolean doneScouting;
	int EC_MIN_INFLUENCE = 50;
	MapLocation nearestCorner;


	public EnlightenmentCenter(RobotController rc) throws GameActionException {
		super(rc);
		scoutSpawnedIn = new int[4];
		spawnedAllies = new int[3000];
		spawnedAlliesSet = new HashSet<Integer>();
		numSpawned = 0;
		mapBoundaries = new int[4];
		mapWidth = 0;
		mapHeight = 0;
		lastBid = 5;
		doneScouting = false;
		nearestCorner = null;
	}

	public void run() throws GameActionException {
		super.run();
//		bid();
		saveSpawnedAlliesIDs();
		checkRobotFlags();
		spawnScouts();
		if(doneScouting){
			if(nearestCorner == null){
				nearestCorner = findNearestCorner();
			}
			broadcastNearestCorner();
		}
		spawnSlanderers();
	}

	public void saveSpawnedAlliesIDs() throws GameActionException {
		for(Direction dir : Util.directions){
			MapLocation loc = myLoc.add(dir);
			if(rc.canSenseLocation(loc)){
				RobotInfo info = rc.senseRobotAtLocation(loc);
				if(info == null){
					continue;
				}
				int id = info.getID();
				if(!spawnedAlliesSet.contains(id)){
					spawnedAllies[numSpawned] = id;
					spawnedAlliesSet.add(id);
					numSpawned += 1;
				}
			}
		}
	}

	public void checkRobotFlags() throws GameActionException {
		for(int i = 0; i < numSpawned; i++){
			int robotID = spawnedAllies[i];
			if(rc.canGetFlag(robotID)){
				int flag = rc.getFlag(robotID);
				int[] splits = Util.parseFlag(flag);
				if(splits.length == 0){
					continue;
				}
				switch(splits[0]){
					case 1: // Scouting
						if(doneScouting){
							continue;
						}
						int dirIdx = splits[1];
						if(mapBoundaries[dirIdx] == 0){
							mapBoundaries[dirIdx] = splits[2];
						}
						if(mapBoundaries[0] != 0 && mapBoundaries[1] != 0){
							mapWidth = mapBoundaries[1] - mapBoundaries[0] + 1;
							System.out.println("Map width: " + mapWidth);
						}
						if(mapBoundaries[2] != 0 && mapBoundaries[3] != 0){
							mapHeight = mapBoundaries[3] - mapBoundaries[2] + 1;
							System.out.println("Map height: " + mapHeight);
						}
						if(mapWidth != 0 && mapHeight != 0){
							doneScouting = true;
						}
						break;
					case 2:
						int idx = splits[1];
						// 0: Enemy EC, 1: Friendly EC, 2: Enemy slanderer
						RobotType[] robotTypes = {RobotType.ENLIGHTENMENT_CENTER, RobotType.ENLIGHTENMENT_CENTER, RobotType.SLANDERER};
						Team[] robotTeams = {myTeam.opponent(), myTeam, myTeam.opponent()};
						RobotType detectedType = robotTypes[idx];
						Team detectedTeam = robotTeams[idx];
						int x = splits[2];
						int y = splits[3];
						MapLocation detectedLoc = Util.xyToMapLocation(x, y);
						boolean alreadySaved = false;
						for(int j = 0; j < robotLocationsIdx; j++){
							if(robotLocations[j].loc == detectedLoc){
								robotLocations[j].team = detectedTeam;
								robotLocations[j].type = detectedType;
								alreadySaved = true;
								break;
							}
						}
						if(!alreadySaved) {
							robotLocations[robotLocationsIdx] = new DetectedInfo(detectedTeam, detectedType, detectedLoc);
							robotLocationsIdx++;
							System.out.println("Detected new robot of type: " + detectedType.toString() + " and of team: " + detectedTeam.toString() + " at: " + detectedLoc.toString());
						}
						break;
				}
			}
		}
	}

	public void spawnScouts() throws GameActionException {
		Direction[] spawnDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
		int influence = 1;
		for (int i = 0; i < spawnDirections.length; i++) {
			if(scoutSpawnedIn[i] != 0){
				continue;
			}
			Direction spawnDir = spawnDirections[i];
			if(Util.tryBuild(RobotType.MUCKRAKER, spawnDir, influence)){
				scoutSpawnedIn[i] = turnCount;
			}
		}
	}


	public void spawnSlanderers() throws GameActionException {
		System.out.println("spawnSlanderers -- Cooldown left: " + rc.getCooldownTurns());
		// Figure out spawn influence
		if(rc.getInfluence() < EC_MIN_INFLUENCE){
			return;
		}
		int spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, rc.getInfluence() / 5);
		// Figure out spawn direction
		Direction spawnDir = nav.randomDirection();
		/*
		DetectedInfo closestEnemyEC = Util.getClosestEnemyEC();
		if(closestEnemyEC != null){
			System.out.println("CLOSEST ENEMY LOC: " + closestEnemyEC.loc.toString());
			System.out.println("CLOSEST ENEMY DIRECTION: " + myLoc.directionTo(closestEnemyEC.loc).toString());
			spawnDir = myLoc.directionTo(closestEnemyEC.loc).opposite();
		}
		*/
		if(nearestCorner != null){
			spawnDir = myLoc.directionTo(nearestCorner);
		}

		System.out.println("Spawning Slanderers in " + spawnDir.toString() + " direction");

		Util.tryBuild(RobotType.SLANDERER, spawnDir, spawnInfluence);
		Util.tryBuild(RobotType.SLANDERER, spawnDir.rotateLeft(), spawnInfluence);
		Util.tryBuild(RobotType.SLANDERER, spawnDir.rotateRight(), spawnInfluence);

	}

	public MapLocation findNearestCorner() throws GameActionException {
		int diffX1 = myLoc.x - mapBoundaries[0];
		int diffX2 = mapBoundaries[1] - myLoc.x;
		int diffY1 = myLoc.y - mapBoundaries[2];
		int diffY2 = mapBoundaries[3] - myLoc.y;
		int cornerX = mapBoundaries[0];
		if(diffX2 < diffX1){
			cornerX = mapBoundaries[1];
		}
		int cornerY = mapBoundaries[2];
		if(diffY2 < diffY1){
			cornerY = mapBoundaries[3];
		}
		return new MapLocation(cornerX, cornerY);
	}

	public void broadcastNearestCorner() throws GameActionException {
		if(nearestCorner == null){
			return;
		}
		System.out.println("Broadcasting that nearest corner is: " + nearestCorner.toString());
		int purpose = 3;
		int[] xy = Util.mapLocationToXY(nearestCorner);
		int x = xy[0];
		int y = xy[1];
		int[] flagArray = {purpose, 4, x, 7, y, 7};
		int flag = Util.concatFlag(flagArray);
		Util.setFlag(flag);
	}


	/*
	public void bid() throws GameActionException {
		System.out.println("Got to bid method");
		int myInfluence = rc.getInfluence();
		int newBid;
		if(this.turnCount == 0) {
			newBid = myInfluence / 4;
			System.out.println("New Bid 1: " + newBid);
		}
		else if(this.wonPrevVote) {
			newBid = (int)(lastBid * .90);
			System.out.println("New Bid 2: " + newBid);
		}
		else {
			newBid = (int)(lastBid * 1.50);
			System.out.println("New Bid 3: " + newBid);
		}
		if(rc.canBid(newBid)) {
			rc.bid(lastBid);
			lastBid = newBid;
			System.out.println("Spent influence on bid: " + lastBid);
		}
		else {
			System.out.println("Cannot bid");
		}
	}
	*/
}
