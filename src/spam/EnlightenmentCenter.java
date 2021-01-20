package spam;

import battlecode.common.*;

import java.util.Arrays;
import java.util.HashSet;

// NOTE: Defense politicians have odd influence, attack politicians have even influence
// NOTE: Attacking muckrackers have odd influence, scout muckrackers have even influence

class SpawnInfo {
	RobotType type;
	Direction spawnDir;
	int id;
	int spawnInfluence;
	boolean alive;

	public SpawnInfo(RobotType type, Direction spawnDir, int id, int spawnInfluence){
		this.type = type;
		this.spawnDir = spawnDir;
		this.id = id;
		this.spawnInfluence = spawnInfluence;
		this.alive = true;
	}
}


public class EnlightenmentCenter extends Robot {

	SpawnInfo[] spawnedAllies = new SpawnInfo[3000];
	HashSet<Integer> spawnedAlliesIDs = new HashSet<Integer>();
	int numSpawned = 0;
	int lastBid;
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
		// TODO: Uncomment this, only here to make games shorter
		if(rc.getRoundNum() > 800){
			rc.resign();
		}
		saveSpawnedAlliesIDs();
		checkRobotFlags();
		setAttackTarget();
		updateFlag();
		if(attackTarget != null){
			System.out.println("ATTACKING: " + attackTarget.toString());
		}
		System.out.println("Leftover bytecode: " + Clock.getBytecodesLeft());
		if(!enemySpotted){
			if(numSpawned < 30){
				System.out.println("Spawning A");
				spawnRatio(1, 1, 0, 2);
			}
			else{
				System.out.println("Spawning B");
				spawnRatio(2, 2, 1, 1);
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
				int id = info.getID();
				if (!spawnedAlliesIDs.contains(id)) {
					spawnedAllies[numSpawned] = new SpawnInfo(info.getType(), myLoc.directionTo(info.getLocation()), id, info.getInfluence());
					spawnedAlliesIDs.add(id);
					numSpawned += 1;
				}
			}
		}
		// Filter out dead troops
		for(int i = 0; i < numSpawned; i++){
			SpawnInfo info = spawnedAllies[i];
			if(!rc.canGetFlag(info.id)){
				// Check if the robot is still alive
				if(info.alive){
					spawnedAlliesIDs.remove(info.id);
				}
				info.alive = false;
			}
			else if(info.type == RobotType.SLANDERER || info.type == RobotType.POLITICIAN){
				// Check if the slanderer converted to a politician, and if it did, it must've converted to an attack politician
				typeInQuestion = null;
				Comms.checkFlag(info.id);
				if(typeInQuestion == RobotType.SLANDERER) {
					info.type = RobotType.SLANDERER;
				}
				else {
					info.type = RobotType.POLITICIAN;
				}
			}
		}
		System.out.println("Troop count:" + filterSpawnedRobots(null, null, -1).length);
		System.out.println("Slanderers: " + filterSpawnedRobots(RobotType.SLANDERER, null, -1).length);
		System.out.println("Attack polis: " + filterSpawnedRobots(RobotType.POLITICIAN, null, 0).length);
		System.out.println("Defense polis: " + filterSpawnedRobots(RobotType.POLITICIAN, null, 1).length);
		System.out.println("Muckrackers: " + filterSpawnedRobots(RobotType.MUCKRAKER, null, -1).length);

	}


	public void spawnMucks() throws GameActionException {
		System.out.println("spawnMucks -- Cooldown left: " + rc.getCooldownTurns());
		if(!doneScouting){
			// Find a list of unsearched directions to spawn the scout in
			System.out.println(mapBoundaries[0] + " " + mapBoundaries[1] + " " + mapBoundaries[2] + " " + mapBoundaries[3]);
			Direction[] spawnDirections = new Direction[8];
			int tempIdx = 0;
			if (mapBoundaries[0] == 0) { spawnDirections[tempIdx] = Direction.WEST; tempIdx++; }
			if (mapBoundaries[1] == 0) { spawnDirections[tempIdx] = Direction.EAST; tempIdx++; }
			if (mapBoundaries[2] == 0) { spawnDirections[tempIdx] = Direction.SOUTH; tempIdx++; }
			if (mapBoundaries[3] == 0) { spawnDirections[tempIdx] = Direction.NORTH; tempIdx++; }
			if (mapBoundaries[0] == 0 && mapBoundaries[2] == 0) { spawnDirections[tempIdx] = Direction.SOUTHWEST; tempIdx++; }
			if (mapBoundaries[0] == 0 && mapBoundaries[3] == 0) { spawnDirections[tempIdx] = Direction.NORTHWEST; tempIdx++; }
			if (mapBoundaries[1] == 0 && mapBoundaries[2] == 0) { spawnDirections[tempIdx] = Direction.SOUTHEAST; tempIdx++; }
			if (mapBoundaries[1] == 0 && mapBoundaries[3] == 0) { spawnDirections[tempIdx] = Direction.NORTHEAST; tempIdx++; }

			// Build scouting muck - influence of 2
			spawnDirections = Arrays.copyOfRange(spawnDirections, 0, tempIdx);
			Direction[] shuffledSpawnDirs = Util.shuffleArr(spawnDirections);
			for (Direction spawnDir : shuffledSpawnDirs) {
				if (Util.tryBuild(RobotType.MUCKRAKER, spawnDir, 2)) {
					return;
				}
			}
		}
		else{
			Direction[] spawnDirs = Navigation.randomizedDirs();
			if(attackTarget != null){
				spawnDirs = Navigation.closeDirections(myLoc.directionTo(attackTarget));
			}
			// Build attacking muck - influence of 1
			for (Direction spawnDir : spawnDirs) {
				if (Util.tryBuild(RobotType.MUCKRAKER, spawnDir, 1)) {
					return;
				}
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
		if(spawnInfluence % 2 == 1){
			spawnInfluence -= 1;
		}

		// Spawn in random direction
		Direction[] spawnDirs = Navigation.randomizedDirs();
		for (Direction dir : spawnDirs) {
			Util.tryBuild(RobotType.SLANDERER, dir, spawnInfluence);
		}
	}

	public void spawnPoliticians(boolean defense) throws GameActionException {
		System.out.println("spawnPoliticians -- Cooldown left: " + rc.getCooldownTurns());
		// Figure out spawn influence
		if (rc.getInfluence() < EC_MIN_INFLUENCE) {
			return;
		}

		Direction[] spawnDirs = Navigation.randomizedDirs();

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

	public void setAttackTarget() throws GameActionException {
		// Check if ready to attack
		SpawnInfo[] attackPolis = filterSpawnedRobots(RobotType.POLITICIAN, null, 0);
		int attackInf = 0;
		for(int i = 0; i < attackPolis.length; i++){
			attackInf += attackPolis[i].spawnInfluence - 10;
		}
		System.out.println("Attack influence: " + attackInf);
		if(attackTarget != null){
			DetectedInfo[] targetInfo = Util.getCorrespondingRobots(null, null, attackTarget);
			assert(targetInfo.length > 0);
			if(targetInfo[0].team == myTeam){
				// Captured the target, lets go attack a diff target
				attackTarget = null;
			}
			else{
				if(attackInf < 100){
					attackTarget = null;
				}
				return;
			}
		}
		DetectedInfo[] allECInfo = Util.getCorrespondingRobots(null, RobotType.ENLIGHTENMENT_CENTER, null);
		System.out.println("Num of ECs known: " + allECInfo.length);
		// Attack the closest EC
		MapLocation closestTarget = null; int closestDist = Integer.MAX_VALUE;
		for(DetectedInfo info : allECInfo){
			if(info.team == myTeam){
				continue;
			}
			int dist = info.loc.distanceSquaredTo(myLoc);
			if(dist < closestDist){
				closestDist = dist;
				closestTarget = info.loc;
			}
		}
		if(closestTarget != null){
			// Guess how much it costs to capture? Send waves of 200 maybe?
			if(attackInf > 400){
				attackTarget = closestTarget;
			}
		}
	}

	public void updateFlag() throws  GameActionException{
		int purpose = 4;
		int[] xy = {0, 0};
		int attackType = 3; // Stop attacking
		if(attackTarget != null){
			xy = Comms.mapLocationToXY(attackTarget);
			attackType = 2; // Everyone attack!
		}
		int[] flagArray = {purpose, 4, xy[0], 7, xy[1], 7, attackType, 2};
		int flag = Comms.concatFlag(flagArray);
		Comms.setFlag(flag);
	}

	public SpawnInfo[] filterSpawnedRobots(RobotType type, Direction spawnDir, int infMod){
		SpawnInfo[] copy = new SpawnInfo[numSpawned];
		int count = 0;

		for(int i = 0; i < numSpawned; i++){
			SpawnInfo info = spawnedAllies[i];
			if(!info.alive){
				continue;
			}
			if(type != null && info.type != type){
				continue;
			}
			if(spawnDir != null && info.spawnDir != spawnDir){
				continue;
			}
			if(infMod != -1 && info.spawnInfluence % 2 != infMod) {
				continue;
			}
			copy[count] = info;
			count++;
		}
		SpawnInfo[] copy2 = Arrays.copyOfRange(copy, 0, count);
		return copy2;
	}
}
