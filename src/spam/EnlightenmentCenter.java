package spam;

import battlecode.common.*;

import java.util.HashSet;

class SpawnInfo {
	RobotType type;
	Direction spawnDir;
	int id;

	public SpawnInfo(RobotType type, Direction spawnDir, int id){
		this.type = type;
		this.spawnDir = spawnDir;
		this.id = id;
	}
}


public class EnlightenmentCenter extends Robot {

	SpawnInfo[] spawnedAllies = new SpawnInfo[3000];
	HashSet<Integer> spawnedAlliesIDs = new HashSet<Integer>();
	int numSpawned = 0;
	int lastBid;
	int slanderersSpawned = 0;
	int EC_MIN_INFLUENCE = 15;
	final int DEF_POLI_MIN_COST = 20;
	final int ATK_POLI_MIN_COST = 50;
	final int SLAND_MIN_COST = 40;


	public EnlightenmentCenter(RobotController rc) throws GameActionException {
		super(rc);
		lastBid = 5;
	}

	public void run() throws GameActionException {
		super.run();
//		bid();
		saveSpawnedAlliesIDs();
		checkRobotFlags();
		if(attackTarget != null){
			for(int i = 0; i < robotLocationsIdx; i++){
				DetectedInfo detected = robotLocations[i];
				if(detected.loc.equals(attackTarget) && detected.team == myTeam){
					attackTarget = null;
				}
			}
		}
		System.out.println("Leftover bytecode: " + Clock.getBytecodesLeft());
		if(!enemySpotted){
			if(numSpawned < 30){
				System.out.println("Spawning A");
				spawnRatio(2, 2, 0, 1);
			}
			else{
				System.out.println("Spawning B");
				spawnRatio(1, 2, 1, 1);
			}
		}
		else if(attackTarget != null){
			System.out.println("Spawning C");
			spawnRatio(1, 1, 2, 2);
		}
		else{
			if(numSpawned < 300){
				System.out.println("Spawning D");
				spawnRatio(1, 2, 1, 1);
			}
			else{
				System.out.println("Spawning E");
				spawnRatio(1, 1, 2, 1);
			}
		}
	}

	public void spawnRatio(int slands, int defensePols, int attackPols, int mucks) throws GameActionException {
		int total = slands + defensePols + attackPols + mucks;
		int mod = numSpawned % total;
		if(mod < slands){
			spawnSlanderers();
		}
		else if(mod < slands + defensePols) {
			spawnPoliticians(true);
		}
		else if(mod < slands + defensePols + attackPols) {
			spawnPoliticians(false);
		}
		else{
			spawnMucks();
		}
	}

	public void checkRobotFlags() throws GameActionException {
		for (int i = 0; i < numSpawned; i++) {
			int robotID = spawnedAllies[i].id;
			Comms.checkFlag(robotID);
		}
	}

	public void saveSpawnedAlliesIDs() throws GameActionException {
		for (Direction dir : Util.directions) {
			MapLocation loc = myLoc.add(dir);
			if (rc.canSenseLocation(loc)) {
				RobotInfo info = rc.senseRobotAtLocation(loc);
				if (info == null) {
					continue;
				}
				// NOTE: It only saves muck's ids rn
//				if(info.getType() != RobotType.MUCKRAKER){
//					continue;
//				}
				int id = info.getID();
				if (!spawnedAlliesIDs.contains(id)) {
					spawnedAllies[numSpawned] = new SpawnInfo(info.getType(), myLoc.directionTo(info.getLocation()), id);
					spawnedAlliesIDs.add(id);
					numSpawned += 1;
				}
			}
		}
	}


	public void spawnMucks() throws GameActionException {
		// Find a list of directions that I could spawn the robot in (list of unsearched directions)
		System.out.println("spawnMucks -- Cooldown left: " + rc.getCooldownTurns());
		Direction[] spawnDirections = new Direction[8];
		int tempIdx = 0;
		if (mapBoundaries[0] == 0) {
			spawnDirections[tempIdx] = Direction.WEST;
			tempIdx++;
		}
		if (mapBoundaries[1] == 0) {
			spawnDirections[tempIdx] = Direction.EAST;
			tempIdx++;
		}
		if (mapBoundaries[2] == 0) {
			spawnDirections[tempIdx] = Direction.SOUTH;
			tempIdx++;
		}
		if (mapBoundaries[3] == 0) {
			spawnDirections[tempIdx] = Direction.NORTH;
			tempIdx++;
		}
		if (mapBoundaries[0] == 0 && mapBoundaries[2] == 0) {
			spawnDirections[tempIdx] = Direction.SOUTHWEST;
			tempIdx++;
		}
		if (mapBoundaries[0] == 0 && mapBoundaries[3] == 0) {
			spawnDirections[tempIdx] = Direction.NORTHWEST;
			tempIdx++;
		}
		if (mapBoundaries[1] == 0 && mapBoundaries[2] == 0) {
			spawnDirections[tempIdx] = Direction.SOUTHEAST;
			tempIdx++;
		}
		if (mapBoundaries[1] == 0 && mapBoundaries[3] == 0) {
			spawnDirections[tempIdx] = Direction.NORTHEAST;
			tempIdx++;
		}

		System.out.println(mapBoundaries[0] + " " + mapBoundaries[1] + " " + mapBoundaries[2] + " " + mapBoundaries[3]);
		for (int i = numSpawned; i < numSpawned + tempIdx; i++) {
			Direction spawnDir = spawnDirections[numSpawned % tempIdx];
			if (Util.tryBuild(RobotType.MUCKRAKER, spawnDir, 1)) {
				break;
			}
		}
	}


	public void spawnSlanderers() throws GameActionException {
		// Figure out spawn influence
		System.out.println("spawnSlands -- Cooldown left: " + rc.getCooldownTurns());
		if (rc.getInfluence() < EC_MIN_INFLUENCE) {
			return;
		}
		int spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, rc.getInfluence() / 3);
		if(spawnInfluence < SLAND_MIN_COST){
			return;
		}

		// Spawn in random direction
		boolean spawned = false;
//		if(Util.tryBuild(RobotType.SLANDERER, Direction.NORTH, spawnInfluence)){
//			spawned = true;
//		}
		for (Direction dir : Navigation.directions) {
			Util.tryBuild(RobotType.SLANDERER, dir, spawnInfluence);
		}
		if (spawned) {
			slanderersSpawned++;
		}

	}

	public void spawnPoliticians(boolean defense) throws GameActionException {
		System.out.println("spawnPoliticians -- Cooldown left: " + rc.getCooldownTurns());
		// Figure out spawn influence
		if (rc.getInfluence() < EC_MIN_INFLUENCE) {
			return;
		}

		Direction lastSpawned = spawnedAllies[numSpawned - 1].spawnDir;
		Direction[] spawnDirs = Navigation.getCCWFromStart(lastSpawned.rotateRight());

		// Defense politicians have odd influence, attack politicians have even influence
		int spawnInfluence;
		if (defense) {
			spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, rc.getInfluence() / 6);
			if(spawnInfluence < DEF_POLI_MIN_COST){ return; }
			if (spawnInfluence % 2 == 0) {
				spawnInfluence -= 1;
			}
			for(RobotInfo info : nearby){
				if(info.type == RobotType.MUCKRAKER && info.team == myTeam.opponent()){
					// If you sense an enemy muck nearby, spawn it in the direction of the muck
					spawnDirs = Navigation.closeDirections(myLoc.directionTo(info.location));
				}
			}
		}
		else {
			spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, rc.getInfluence() / 3);
			if(spawnInfluence < ATK_POLI_MIN_COST){ return; }
			if (spawnInfluence % 2 == 1) {
				spawnInfluence -= 1;
			}
			if(attackTarget != null){
				spawnDirs = Navigation.closeDirections(myLoc.directionTo(attackTarget));
			}
		}

		// Figure out spawn direction
		for (Direction dir : spawnDirs) {
			Util.tryBuild(RobotType.POLITICIAN, dir, spawnInfluence);
		}
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
