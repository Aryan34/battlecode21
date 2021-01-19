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
	int EC_MIN_INFLUENCE = 20;
	final int DEF_POLI_MIN_COST = 15;
	final int ATK_POLI_MIN_COST = 50;
	final int SLAND_MIN_COST = 40;

	boolean attacking = false;


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
				spawnRatio(3, 1, 0, 1);
			}
			else{
				spawnRatio(1, 2, 1, 1);
			}
		}
		else if(attacking){
			spawnRatio(1, 1, 2, 2);
		}
		else{
			if(numSpawned < 300){
				spawnRatio(1, 2, 1, 1);
			}
			else{
				spawnRatio(2, 1, 2, 1);
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
			int robotID = spawnedAllies[i];
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
				if (!spawnedAlliesSet.contains(id)) {
					spawnedAllies[numSpawned] = id;
					spawnedAlliesSet.add(id);
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

		// Defense politicians have odd influence, attack politicians have even influence
		int spawnInfluence;
		if (defense) {
			spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, rc.getInfluence() / 6);
			if(spawnInfluence < DEF_POLI_MIN_COST){
				return;
			}
			if (spawnInfluence % 2 == 0) {
				spawnInfluence -= 1;
			}
		}
		else {
			spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, rc.getInfluence() / 3);
			if(spawnInfluence < ATK_POLI_MIN_COST){
				return;
			}
			if (spawnInfluence % 2 == 1) {
				spawnInfluence -= 1;
			}
		}

		// Figure out spawn direction
		for (Direction dir : Navigation.directions) {
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
