package spam;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashSet;

public class EnlightenmentCenter extends Robot {

	int[] spawnedAllies = new int[3000];
	HashSet<Integer> spawnedAlliesSet = new HashSet<Integer>();
	int numSpawned = 0;
	int lastBid;
	int slanderersSpawned = 0;
	int EC_MIN_INFLUENCE = 50;
	CornerInfo nearestCorner = null;


	public EnlightenmentCenter(RobotController rc) throws GameActionException {
		super(rc);
		lastBid = 5;
	}

	public void run() throws GameActionException {
		super.run();
//		bid();
		saveSpawnedAlliesIDs();
		checkRobotFlags();
		if(numSpawned < 10 && !enemySpotted){
			System.out.println("Spawning scouts");
			spawnScouts();
		}
		else if(numSpawned < 40 && !enemySpotted){
			System.out.println("Spawning everything");
			// Spawn rotation: sland then pol then scout
			if(numSpawned % 3 == 0){
				System.out.println("Spawning sland");
				spawnSlanderers();
			}
			else if(numSpawned % 3 == 1){
				System.out.println("Spawning pol");
//				spawnPoliticians();
			}
			else{
				System.out.println("Spawning scout");
				spawnScouts();
			}
		}
		if(slanderersSpawned < 10000){
			System.out.println("Spawning ratio");
			// 2:1 sland to pol ratio
			// TODO: Change this to 1
			if(numSpawned % 3 < 0){
				spawnPoliticians();
			}
			else{
				spawnSlanderers();
			}
		}
		else{
			System.out.println("Spawning politicians");
//			spawnPoliticians();
		}

	}

	public void checkRobotFlags() throws GameActionException {
		for (int i = 0; i < numSpawned; i++) {
			int robotID = spawnedAllies[i];
			Comms.checkFlag(robotID);
		}
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


	public void spawnScouts() throws GameActionException {
		// Find a list of directions that I could spawn the robot in (list of unsearched directions)
		Direction[] spawnDirections = new Direction[8];
		int tempIdx = 0;
		if(mapBoundaries[0] == 0){ spawnDirections[tempIdx] = Direction.WEST; tempIdx++; }
		if(mapBoundaries[1] == 0){ spawnDirections[tempIdx] = Direction.EAST; tempIdx++; }
		if(mapBoundaries[2] == 0){ spawnDirections[tempIdx] = Direction.SOUTH; tempIdx++; }
		if(mapBoundaries[3] == 0){ spawnDirections[tempIdx] = Direction.NORTH; tempIdx++; }
		if(mapBoundaries[0] == 0 && mapBoundaries[2] == 0){ spawnDirections[tempIdx] = Direction.SOUTHWEST; tempIdx++; }
		if(mapBoundaries[0] == 0 && mapBoundaries[3] == 0){ spawnDirections[tempIdx] = Direction.NORTHWEST; tempIdx++; }
		if(mapBoundaries[1] == 0 && mapBoundaries[2] == 0){ spawnDirections[tempIdx] = Direction.SOUTHEAST; tempIdx++; }
		if(mapBoundaries[1] == 0 && mapBoundaries[3] == 0){ spawnDirections[tempIdx] = Direction.NORTHEAST; tempIdx++; }

		System.out.println(mapBoundaries[0] + " " + mapBoundaries[1] + " " + mapBoundaries[2] + " " + mapBoundaries[3]);
		for (int i = numSpawned; i < numSpawned + tempIdx; i++) {
			Direction spawnDir = spawnDirections[numSpawned % tempIdx];
			if(Util.tryBuild(RobotType.MUCKRAKER, spawnDir, 1)){
				break;
			}
		}
	}


	public void spawnSlanderers() throws GameActionException {
		// Figure out spawn influence
		System.out.println("spawnSlands -- Cooldown left: " + rc.getCooldownTurns());
		if(rc.getInfluence() < EC_MIN_INFLUENCE){
			return;
		}
		int spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, rc.getInfluence() / 5);

		// Spawn in random direction
		boolean spawned = false;
//		if(Util.tryBuild(RobotType.SLANDERER, Direction.NORTH, spawnInfluence)){
//			spawned = true;
//		}
		for(Direction dir : Navigation.directions){
			Util.tryBuild(RobotType.SLANDERER, dir, spawnInfluence);
		}
		if(spawned){
			slanderersSpawned++;
		}

	}

	public void spawnPoliticians() throws GameActionException {
		System.out.println("spawnPoliticians -- Cooldown left: " + rc.getCooldownTurns());
		// Figure out spawn influence
		if(rc.getInfluence() < EC_MIN_INFLUENCE){
			return;
		}
		int spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, rc.getInfluence() / 5);
		// Figure out spawn direction

		for(Direction dir : Navigation.directions){
			Util.tryBuild(RobotType.POLITICIAN, dir, spawnInfluence);
		}
	}

	public CornerInfo findNearestCorner() throws GameActionException {
		int diffX1 = myLoc.x - mapBoundaries[0];
		int diffX2 = mapBoundaries[1] - myLoc.x;
		int diffY1 = myLoc.y - mapBoundaries[2];
		int diffY2 = mapBoundaries[3] - myLoc.y;
		int cornerX = mapBoundaries[0];
		int xoff = 1;
		if(diffX2 < diffX1){
			cornerX = mapBoundaries[1];
			xoff = -1;
		}
		int cornerY = mapBoundaries[2];
		int yoff = 1;
		if(diffY2 < diffY1){
			cornerY = mapBoundaries[3];
			yoff = -1;
		}
		MapLocation cornerLoc = new MapLocation(cornerX, cornerY);
		return new CornerInfo(cornerLoc, xoff, yoff);
	}

	public void broadcastNearestCorner() throws GameActionException {
		if(nearestCorner == null){
			return;
		}
		System.out.println("Broadcasting that nearest corner is: " + nearestCorner.toString());
		int purpose = 3;
		int[] xy = Comms.mapLocationToXY(nearestCorner.loc);
		int[] flagArray = {purpose, 4, xy[0], 7, xy[1], 7, nearestCorner.getCornerDirection(), 2};
		int flag = Comms.concatFlag(flagArray);
		Comms.setFlag(flag);
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
