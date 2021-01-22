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

	int lastBid = 5;
	boolean savingForSuicide = false;
	int EC_MIN_INFLUENCE = 10;
	final int DEF_POLI_MIN_COST = 20;
	final int ATK_POLI_MIN_COST = 50;
	final int SLAND_MIN_COST = 21;
	final int ATTACK_MIN_INFLUENCE = 1200;
	final int STOP_ATTACK_MIN_INFLUENCE = 300;
	int[] slandBenefitDP = new int[10];

	// Troop spawning variables
	SpawnInfo[] spawnedAllies = new SpawnInfo[1500];
	HashSet<Integer> spawnedAlliesIDs = new HashSet<Integer>();
	int numSpawned = 0;
	SpawnInfo[] slandererInfo = new SpawnInfo[1500]; int slanderersSpawned = 0;
	SpawnInfo[] attackPoliInfo = new SpawnInfo[1500]; int attackPolisSpawned = 0;
	SpawnInfo[] defensePoliInfo = new SpawnInfo[1500]; int defensePolisSpawned = 0;
	SpawnInfo[] muckInfo = new SpawnInfo[1500]; int mucksSpawned = 0;

	int slandsAlive = 0;
	int attackersAlive = 0;
	int defendersAlive = 0;
	int mucksAlive = 0;


	public EnlightenmentCenter(RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		// TODO: Comment this out, only here to make games shorter
//		if(currRound > 600){
//			rc.resign();
//		}
//		Log.log("Starting bytecode: " + Clock.getBytecodesLeft());
		saveSpawnedAlliesIDs();
//		Log.log("Leftover bytecode A: " + Clock.getBytecodesLeft()); // 5.7k
		checkRobotFlags();
//		Log.log("Leftover bytecode 1: " + Clock.getBytecodesLeft()); // 6k

		Log.log("Troop count:" + (slandsAlive + attackersAlive + defendersAlive + mucksAlive));
		Log.log("Slanderers: " + slandsAlive);
		Log.log("Attack polis: " + attackersAlive);
		Log.log("Defense polis: " + defendersAlive);
		Log.log("Mucks: " + mucksAlive);

		setAttackTarget(attackPoliInfo);
		updateFlag();

//		Log.log("Leftover bytecode 3: " + Clock.getBytecodesLeft()); // 0.5k

		checkSuicideViable(slandererInfo);

		if(attackTarget != null){ Log.log("ATTACKING: " + attackTarget.toString()); }
//		Log.log("Leftover bytecode 4: " + Clock.getBytecodesLeft()); // 2k


		// When spawning: 0 = slanderer, 1 = defensive poli, 2 = attacking poli, 3 = scout muck, 4 = attack muck
		if(savingForSuicide){
			Log.log("Saving for suicide!");
			if(rc.getInfluence() > 700 && rc.getEmpowerFactor(myTeam, 10) >= 1.1){
				spawnPoliticians(true, true);
			}
		}
		else if(rc.getRoundNum() - turnCount > 3 && turnCount < 25) {
			Log.log("Spawning neutral EC order");
			int[] order = {0, 3, 0, 3, 1, 1, 3, 3, 0, 1, 3, 1, 0, 3, 1, 1, 1, 3, 3, 0, 3, 1, 1, 0, 1};
			spawnOrder(order);
		}

		else if(!enemySpotted){
			Log.log("Spawning A");
//			int[] order = {0, 3, 3, 3, 1, 0, 3, 0, 3, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0};
			int[] order = {0, 3, 3, 3, 1, 0, 3, 1, 3, 0, 1, 3, 0, 0, 1, 1, 3, 1, 1, 0};
			spawnOrder(order);
		}
		else if(defendersAlive < slandsAlive / 1.5){
			Log.log("Spawning B");
			spawnPoliticians(true, false);
		}
		else if(attackTarget != null){
			Log.log("Spawning C");
			spawnPoliticians(false, true);
			int[] order = {2, 3, 2, 3, 1, 0, 3};
			spawnOrder(order);
		}
		else if(turnCount < 300){
			Log.log("Spawning D");
			int[] order = {1, 0, 1, 3, 2, 0, 1, 3, 0};
			spawnOrder(order);
		}
		else{
			Log.log("Spawning E");
			int[] order = {1, 0, 3, 2, 0, 3, 0, 0, 1};
			spawnOrder(order);
		}

//		Log.log("Leftover bytecode 5: " + Clock.getBytecodesLeft());

	}

	public void spawnOrder(int[] order) throws GameActionException {
		int type = order[numSpawned % order.length];
		if(type == 0){ spawnSlanderers(); }
		if(type == 1){ spawnPoliticians(true, false); }
		if(type == 2){ spawnPoliticians(false, false); }
		if(type == 3){ spawnMucks(true); }
		if(type == 4){ spawnMucks(false); }
	}

	public void checkRobotFlags() throws GameActionException {
		// Check the flags of our mucks and attacker polis
		if(attackTarget != null) {
			for (int i = 0; i < attackPolisSpawned; i++) {
				// Ignore slanderer comms to save bytecode
				if (!attackPoliInfo[i].alive) {
					continue;
				}
				int robotID = attackPoliInfo[i].id;
				Comms.checkFlag(robotID);
			}
		}
		for (int i = 0; i < mucksSpawned; i++) {
			// Ignore slanderer comms to save bytecode
			if(!muckInfo[i].alive){
				continue;
			}
			int robotID = muckInfo[i].id;
			Comms.checkFlag(robotID);
		}
	}

	// If you see a newly spawned troop, keep track of it and save its ID
	public void saveSpawnedAlliesIDs() throws GameActionException {
		for (Direction dir : Util.directions) {
			MapLocation loc = myLoc.add(dir);
			if (rc.canSenseLocation(loc)) {
				RobotInfo info = rc.senseRobotAtLocation(loc);
				if (info == null) {
					continue;
				}
				int id = info.getID();
				if (info.getTeam() == myTeam && !spawnedAlliesIDs.contains(id)) {
					Log.log("Found new troop of type: " + info.getType());
					SpawnInfo spawnInfo = new SpawnInfo(info.getType(), myLoc.directionTo(info.getLocation()), id, info.getInfluence(), currRound);
					spawnedAllies[numSpawned] = spawnInfo;
					spawnedAlliesIDs.add(id);
					numSpawned++;
					if(info.getType() == RobotType.SLANDERER){
						slandererInfo[slanderersSpawned] = spawnInfo;
						slanderersSpawned++;
						slandsAlive++;
					}
					else if(info.getType() == RobotType.POLITICIAN && info.getInfluence() % 2 == 0){
						attackPoliInfo[attackPolisSpawned] = spawnInfo;
						attackPolisSpawned++;
						attackersAlive++;
					}
					else if(info.getType() == RobotType.POLITICIAN && info.getInfluence() % 2 == 1){
						defensePoliInfo[defensePolisSpawned] = spawnInfo;
						defensePolisSpawned++;
						defendersAlive++;
					}
					else if(info.getType() == RobotType.MUCKRAKER){
						muckInfo[mucksSpawned] = spawnInfo;
						mucksSpawned++;
						mucksAlive++;
					}
				}
			}
		}

		// Filter out dead troops
		for(int i = 0; i < numSpawned; i++){
			SpawnInfo info = spawnedAllies[i];
			if(!info.alive){ continue; }
			if(!rc.canGetFlag(info.id)){
				// Check if the robot is still considered alive
				if(info.alive){
					spawnedAlliesIDs.remove(info.id);
					info.alive = false;
					if(info.type == RobotType.SLANDERER){ slandsAlive--; }
					else if(info.type == RobotType.POLITICIAN && info.spawnInfluence % 2 == 0){ attackersAlive--; }
					else if(info.type == RobotType.SLANDERER && info.spawnInfluence % 2 == 0){ defendersAlive--; }
					else if(info.type == RobotType.MUCKRAKER){ mucksAlive--; }
				}
			}
			else if(info.type == RobotType.SLANDERER && turnCount >= info.spawnRound + GameConstants.CAMOUFLAGE_NUM_ROUNDS){
				// Check if the slanderer converted to a politician, and if it did, it must've converted to an attack politician
				info.alive = false;
				SpawnInfo newInfo = new SpawnInfo(RobotType.POLITICIAN, info.spawnDir, info.id, info.spawnInfluence, info.spawnRound);
				attackPoliInfo[attackPolisSpawned] = newInfo;
				attackPolisSpawned++;
				attackersAlive++;
				slandsAlive--;
				spawnedAllies[i] = newInfo;
			}
		}
	}


	public void spawnMucks(boolean scout) throws GameActionException {
		Log.log("spawnMucks -- Cooldown left: " + rc.getCooldownTurns());
		if(scout){
			// Find a list of unsearched directions to spawn the scout in
			Log.log(mapBoundaries[0] + " " + mapBoundaries[1] + " " + mapBoundaries[2] + " " + mapBoundaries[3]);
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
		Log.log("spawnSlands -- Cooldown left: " + rc.getCooldownTurns());
		if (rc.getInfluence() < EC_MIN_INFLUENCE) {
			return;
		}
		// Spawn super high inf slanderers early on. Could make this a linear scale or smth
		int spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, (int)(rc.getInfluence() / 1.5));
		if(currRound < 100){
			spawnInfluence = Math.min(rc.getInfluence() - EC_MIN_INFLUENCE, (int)(rc.getInfluence() / 1.15));
		}
		if(spawnInfluence < SLAND_MIN_COST){
			return;
		}
		// Setup so that it becomes an attack poli instead of a defense poli
		if(spawnInfluence % 2 == 1){
			spawnInfluence -= 1;
		}

		// Try spawning in the direction opposite of the nearest enemy EC
		Direction[] spawnDirs = null;
		if(spawnDirs == null){
			DetectedInfo enemyECInfo = Util.getClosestEnemyEC();
			if(enemyECInfo != null){
				spawnDirs = Navigation.closeDirections(myLoc.directionTo(enemyECInfo.loc).opposite());
			}
		}
		// Just spawn in a random direction
		if(spawnDirs == null){
			spawnDirs = Navigation.randomizedDirs();
		}
		for (Direction dir : spawnDirs) {
			Util.tryBuild(RobotType.SLANDERER, dir, spawnInfluence);
		}
	}

	public void spawnPoliticians(boolean defense, boolean sacrifice) throws GameActionException {
		Log.log("spawnPoliticians -- Cooldown left: " + rc.getCooldownTurns());
		// Figure out spawn influence
		Log.log(rc.getInfluence() + " " + EC_MIN_INFLUENCE);
		if (rc.getInfluence() < EC_MIN_INFLUENCE) {
			return;
		}
		Log.log("My influence is greater than EC Min influence!");

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
			Log.log(spawnInfluence + " " + DEF_POLI_MIN_COST);
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

	public void setAttackTarget(SpawnInfo[] attackPolis) throws GameActionException {
		// Check if ready to attack
		int attackInf = 0;
		for(int i = 0; i < attackPolisSpawned; i++){
			if(!attackPolis[i].alive){ continue; }
			attackInf += attackPolis[i].spawnInfluence - 10;
		}
		Log.log("Attack influence: " + attackInf);
		if(attackTarget != null){
			DetectedInfo[] targetInfo = Util.getCorrespondingRobots(null, null, attackTarget);
			assert(targetInfo.length > 0);
			Log.log(targetInfo[0].team.toString());
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
		Log.log("Num of ECs known: " + allECInfo.length);
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
//		Log.log("Closest target: " + closestTarget == null ? "null" : closestTarget.toString());
		if(closestTarget != null){
			Log.log("Closest target: " + closestTarget.toString());
			// Guess how much it costs to capture? Send waves of 200 maybe?
			if(attackInf > ATTACK_MIN_INFLUENCE){
				attackTarget = closestTarget;
			}
		}
		else{
			Log.log("No closest target found");
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

	public void checkSuicideViable(SpawnInfo[] slandererInfo){
		// TODO: Also somehow check that you're safe for the time being (you have more defense polis than slanderers, and more than 5 defense polis? idk)
//		if(savingForSuicide){ return; }

		// TODO: Uncomment this (testing purposes)
//		savingForSuicide = false;
//		if(rc.getInfluence() > 700 && rc.getEmpowerFactor(myTeam, currRound + 10) > 1.1){
//			savingForSuicide = true;
//		}
		savingForSuicide = false;

		// Calculate the slanderer benefit 10 rounds into the future
//		int slandSum = 0;
//		int projectionRound = currRound + 10;
//		for(int i = 0; i < slanderersSpawned; i++){
//			SpawnInfo info = slandererInfo[i];
//			if(!info.alive){ continue; }
//			if(info.spawnRound + 50 > projectionRound){
//				continue;
//			}
//			slandSum += Util.slandBenefitPerRound(info.spawnInfluence);
//		}
//		slandBenefitDP[currRound % 10] = slandSum;
//
//		// Check if, in the next 10 rounds, you'll be able to pull off a hot suicide
//		savingForSuicide = false;
//		int EC_benefit = 0;
//		int slandBenefit = 0;
//		for(int i = 1; i <= 10; i++){
//			double empowerFactor = rc.getEmpowerFactor(myTeam, i + 10);
//			if(empowerFactor < 1.1){
//				continue;
//			}
//			EC_benefit += Math.ceil(0.2 * (currRound + i));
//			slandBenefit += slandBenefitDP[(currRound + i) % 10];
//			if(rc.getInfluence() + EC_benefit + slandBenefit > 700 && empowerFactor > 1.1){
//				savingForSuicide = true;
//			}
//		}
	}

}
