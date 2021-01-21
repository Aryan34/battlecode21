package spam2;

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
	int spawnRound;
	boolean alive;

	public SpawnInfo(RobotType type, Direction spawnDir, int id, int spawnInfluence, int spawnRound){
		this.type = type;
		this.spawnDir = spawnDir;
		this.id = id;
		this.spawnInfluence = spawnInfluence;
		this.spawnRound = spawnRound;
		this.alive = true;
	}
}


public class EnlightenmentCenter extends Robot {

	SpawnInfo[] spawnedAllies = new SpawnInfo[3000];
	HashSet<Integer> spawnedAlliesIDs = new HashSet<Integer>();
	int numSpawned = 0;
	int lastBid = 5;
	boolean savingForSuicide = false;
	int EC_MIN_INFLUENCE = 10;
	final int DEF_POLI_MIN_COST = 20;
	final int ATK_POLI_MIN_COST = 50;
	final int SLAND_MIN_COST = 21;
	final int ATTACK_MIN_INFLUENCE = 1200;
	final int STOP_ATTACK_MIN_INFLUENCE = 300;

	public EnlightenmentCenter(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		// TODO: Comment this out, only here to make games shorter
		if(rc.getRoundNum() > 600){
			rc.resign();
		}
		saveSpawnedAlliesIDs();
		checkRobotFlags();
		setAttackTarget();
		updateFlag();

		// TODO: Also somehow check that you're safe for the time being (you have more defense polis than slanderers, and more than 5 defense polis? idk)
		// Check if, in the next 20 rounds, you'll be able to pull off a hot suicide
		for(int i = 0; i <= 20; i++){
			if(getExpectedInfluence(rc.getRoundNum() + i) > 700 && rc.getEmpowerFactor(myTeam, i + 10) > 1.1){
				savingForSuicide = true;
			}
			else{
				savingForSuicide = false;
			}
		}

		if(attackTarget != null){ System.out.println("ATTACKING: " + attackTarget.toString()); }
		System.out.println("Leftover bytecode: " + Clock.getBytecodesLeft());

		int numSlanderers = filterSpawnedRobots(RobotType.SLANDERER, null, -1).length;
		int numAttackPolis = filterSpawnedRobots(RobotType.POLITICIAN, null, 0).length;
		int numDefensePolis = filterSpawnedRobots(RobotType.POLITICIAN, null, 1).length;
		int numMucks = filterSpawnedRobots(RobotType.MUCKRAKER, null, -1).length;

		if(savingForSuicide){
			System.out.println("Saving for suicide!");
			if(rc.getInfluence() > 700 && rc.getEmpowerFactor(myTeam, 10) >= 1.1){
				spawnPoliticians(true, true);
			}
		}
		// When spawning: 0 = slanderer, 1 = defensive poli, 2 = attacking poli, 3 = muckraker
		else if(!enemySpotted){
			System.out.println("Spawning A");
//			int[] order = {0, 3, 3, 3, 1, 0, 3, 0, 3, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0};
			int[] order = {0, 3, 3, 3, 1, 0, 3, 1, 3, 0, 1, 3, 0, 0, 1, 1, 3, 1, 1, 0};
			spawnOrder(order);
		}
		else if(numDefensePolis < numSlanderers / 1.5){
			System.out.println("Spawning B");
			spawnPoliticians(true, false);
		}
		else if(attackTarget != null){
			System.out.println("Spawning C");
			spawnPoliticians(false, true);
			int[] order = {2, 3, 2, 3, 1, 0, 3};
			spawnOrder(order);
		}
		else if(numSpawned < 300){
			System.out.println("Spawning D");
			int[] order = {1, 0, 1, 2, 0, 3, 3, 1};
			spawnOrder(order);
		}
		else{
			System.out.println("Spawning E");
			int[] order = {1, 0, 3, 2, 3, 0, 1};
			spawnOrder(order);
		}
	}

	public void spawnOrder(int[] order) throws GameActionException {
		int type = order[numSpawned % order.length];
		if(type == 0){ spawnSlanderers(); }
		if(type == 1){ spawnPoliticians(true, false); }
		if(type == 2){ spawnPoliticians(false, false); }
		if(type == 3){ spawnMucks(); }
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
					spawnedAllies[numSpawned] = new SpawnInfo(info.getType(), myLoc.directionTo(info.getLocation()), id, info.getInfluence(), rc.getRoundNum());
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
			else if(info.type == RobotType.SLANDERER && rc.getRoundNum() >= info.spawnRound + GameConstants.CAMOUFLAGE_NUM_ROUNDS){
				// Check if the slanderer converted to a politician, and if it did, it must've converted to an attack politician
				info.type = RobotType.POLITICIAN;
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
			// Just try spawning in a random direction
			for( Direction spawnDir : Navigation.directions){
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
		// Spawn super high inf slanderers early on. Could make this a linear scale or smth
		int spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, (int)(rc.getInfluence() / 1.5));
		if(rc.getRoundNum() < 100){
			spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, (int)(rc.getInfluence() / 1.15));
		}
		if(spawnInfluence < SLAND_MIN_COST){
			return;
		}
		// Setup so that it becomes an attack poli instead of a defense poli
		if(spawnInfluence % 2 == 1){
			spawnInfluence -= 1;
		}

		// Spawn in random direction
		Direction[] spawnDirs = Navigation.randomizedDirs();
		for (Direction dir : spawnDirs) {
			Util.tryBuild(RobotType.SLANDERER, dir, spawnInfluence);
		}
	}

	public void spawnPoliticians(boolean defense, boolean sacrifice) throws GameActionException {
		System.out.println("spawnPoliticians -- Cooldown left: " + rc.getCooldownTurns());
		// Figure out spawn influence
		System.out.println(rc.getInfluence() + " " + EC_MIN_INFLUENCE);
		if (rc.getInfluence() < EC_MIN_INFLUENCE) {
			return;
		}
		System.out.println("My influence is greater than EC Min influence!");

		Direction[] spawnDirs = Navigation.randomizedDirs();
		// Defense politicians have odd influence, attack politicians have even influence
		int spawnInfluence;
		if (sacrifice){
			spawnInfluence = rc.getInfluence();
			if (spawnInfluence % 2 == 0) {
				spawnInfluence -= 1;
			}
			boolean spawned = false;
			int i = 0;
			while(!spawned && i < Navigation.cardinalDirections.length){
				spawned = Util.tryBuild(RobotType.POLITICIAN, Navigation.cardinalDirections[i], spawnInfluence);
				i++;
			}
			if(spawned){
				savingForSuicide = false;
			}
			return;
		}
		else if (defense) {
			spawnInfluence = Math.min(rc.getInfluence() - DEF_POLI_MIN_COST, Math.max(DEF_POLI_MIN_COST, rc.getInfluence() / 20));
			System.out.println(spawnInfluence + " " + DEF_POLI_MIN_COST);
			if(spawnInfluence < DEF_POLI_MIN_COST){ return; }
			if (spawnInfluence % 2 == 0) {
				spawnInfluence -= 1;
			}
			for(RobotInfo info : nearby){
				if(info.type == RobotType.MUCKRAKER && info.team == myTeam.opponent()){
					// If you sense an enemy muck nearby, spawn it in the direction of the muck
					spawnDirs = Navigation.closeDirections(myLoc.directionTo(info.location));
					break;
				}
			}
		}
		else {
			spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, (int)(rc.getInfluence() / 2.5));
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
			if(Util.tryBuild(RobotType.POLITICIAN, dir, spawnInfluence)){
				break;
			}
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
				if(attackInf < STOP_ATTACK_MIN_INFLUENCE){
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
//		System.out.println("Closest target: " + closestTarget == null ? "null" : closestTarget.toString());
		if(closestTarget != null){
			System.out.println("Closest target: " + closestTarget.toString());
			// Guess how much it costs to capture? Send waves of 200 maybe?
			if(attackInf > ATTACK_MIN_INFLUENCE){
				attackTarget = closestTarget;
			}
		}
		else{
			System.out.println("No closest target found");
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

	public int getExpectedInfluence(int roundNum) throws GameActionException {
		int ECBenefit = Util.getExpectedECBenefit(rc.getRoundNum(), roundNum);
		int slandBenefit = 0;
		SpawnInfo[] slanderers = filterSpawnedRobots(RobotType.SLANDERER, null, -1);
		for(SpawnInfo info : slanderers){
			slandBenefit += Util.getExpectedSlandererBenefit(info.spawnInfluence, info.spawnRound, rc.getRoundNum(), roundNum);
		}
		return rc.getInfluence() + ECBenefit + slandBenefit;
	}

}
