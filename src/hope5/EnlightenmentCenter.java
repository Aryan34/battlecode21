package hope5;

import battlecode.common.*;

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

	int lastBid = -1;
	int EC_MIN_INFLUENCE = 10;
	final int DEF_POLI_MIN_COST = 24;
	final int ATK_POLI_MIN_COST = 50;
	final int SLAND_MIN_COST = 21;
	int[] slandBenefits = new int[1500];
	int[] activeSlands = new int[1500];
	int numVotes = 0;
	DetectedInfo attackTargetInfo = null;
	RobotInfo nearestMuck = null;
	boolean saveForAttack = false;
	int capturerInf = -1;
	int nextRoundFlag = 0;
	double maxBidProportion = 0.1; // Never bid more than this much

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
		if(turnCount > 200){
			bid();
		}

//		checkSlandLatticeDir();
		if(nextRoundFlag != 0){
			Comms.setFlag(nextRoundFlag);
			nextRoundFlag = 0;
		}

		saveSpawnedAlliesIDs();
		checkRobotFlags();

		Log.log("Troop count:" + (slandsAlive + attackersAlive + defendersAlive + mucksAlive));
		Log.log("Slanderers: " + slandsAlive);
		Log.log("Active slands: " + activeSlands[currRound]);
		Log.log("Attack polis: " + attackersAlive);
		Log.log("Defense polis: " + defendersAlive);
		Log.log("Mucks: " + mucksAlive);

		setAttackTarget();
		checkReadyToAttack();

		if(attackTargetInfo != null){ Log.log("Saving up for: " + attackTargetInfo.loc.toString()); }
		if(attackTarget != null){ Log.log("ATTACKING: " + attackTarget.toString()); }

		spawn();

		if(currRound % 2 == 0){
			updateFlag();
		}
		else{
			setBrownian();
		}
	}

	public void checkSlandLatticeDir() throws GameActionException {
		if(!enemySpotted){
			return;
		}
		DetectedInfo[] enemies = Util.getCorrespondingRobots(myTeam.opponent(), null, null);
		int closest = Integer.MAX_VALUE;
		MapLocation closestLoc = null;
		for(int i = 0; i < enemies.length; i++){
			int dist = myLoc.distanceSquaredTo(enemies[i].loc);
			if(dist < closest){
				closest = dist;
				closestLoc = enemies[i].loc;
			}
		}
		int levels = numSlandLevels();
		if(closestLoc != null && Util.getGridSquareDist(closestLoc, myLoc) > levels + 4){ // If the enemy is really close, relay that info asap
			int[] xy = Comms.mapLocationToXY(closestLoc);
			int purpose = 7;
			int[] flagArray = {purpose, 4, 1, 1, xy[0], 7, xy[1], 7};
			int flag = Comms.concatFlag(flagArray);
			Comms.setFlag(flag);
			Log.log("Broadcasting that there's a nearby threat at: ");
			Comms.printFlag(flag);
			earlyScoutingLoc = closestLoc;
		}
		else if(earlyScoutingLoc != null){
			int[] xy = Comms.mapLocationToXY(myLoc);
			int purpose = 7;
			int[] flagArray = {purpose, 4, 0, 1, xy[0], 7, xy[1], 7};
			int flag = Comms.concatFlag(flagArray);
			Comms.setFlag(flag);
			Log.log("Broadcasting that there's no nearby threat");
			earlyScoutingLoc = null;
		}
	}

	public int numSlandLevels() throws GameActionException{
		int startLevel = 8;
		int temp = slandsAlive;
		int levels = 1;
		while(temp > 0){
			temp -= startLevel;
			startLevel += 4;
			levels++;
		}
		return levels;
	}

	public void setBrownian() throws GameActionException {
		if(setFlagThisRound){
			return;
		}
		int purpose = 8;
		int outerLevel = numSlandLevels();
		int defendersNeeded = (outerLevel + 2) * 6;
		Log.log("Outer level: " + outerLevel + ", defenders needed: " + defendersNeeded + ", defenders alive: " + defendersAlive);
		int currDefenders = defendersAlive;
		if(attackTarget == null){
			currDefenders += attackersAlive;
		}
		Log.log("Curr defenders: " + currDefenders);
		int runBrown = 0;
		if(currDefenders > defendersNeeded) {
			runBrown = 1;
		}
		int[] flagArray = {purpose, 4, runBrown, 1};
		int flag = Comms.concatFlag(flagArray);
		Comms.setFlag(flag);
		Log.log("Setting flag to run brownian");
		runningBrownian = true;
	}

	public void spawn() throws GameActionException{
		// If you sense an enemy poli really close by, spawn mucks in that direction to spread out the effect
		// TODO: Mucks should swarm / try surrounding high inf enemy polis?
		// When spawning: 0 = slanderer, 1 = defensive poli, 2 = attacking poli, 3 = scout muck, 4 = attack muck

		Direction enemyPoliDir = enemyPoliNearby();
		nearestMuck = enemyMuckNearby();
		double defenderToSlandRatio = Util.scaleValue(0, 200, 0, 1.5, Math.min(currRound, 200));
		if(enemyPoliDir != null){
			Direction[] spawnDirs = {enemyPoliDir, enemyPoliDir.rotateLeft(), enemyPoliDir.rotateRight()};
			Util.tryBuild(RobotType.MUCKRAKER, spawnDirs, 2);
		}
		else if (!enemySpotted && saveForAttack){
			spawnSave();
		}
		else if(currRound - turnCount < 3 && !enemySpotted){ // If ur the initial EC
			Log.log("Spawning A");
//			int[] order = {0, 3, 1, 0, 3, 1, 3};
//			int[] order = {0, 3, 0, 3, 1, 0, 3, 1, 3, 0, 3, 0, 1, 0, 3};
			int[] order = {0, 3, 1, 3, 0, 1, 3, 0, 3, 1, 3, 3, 0, 3};
			spawnOrder(order);
		}
		else if(defendersAlive < defenderToSlandRatio * slandsAlive){
			Log.log("Spawning B");
			avoidDirs = new Direction[1];
			spawnPoliticians(true);
		}
		else if(nearestMuck != null){
			Log.log("Enemy muck nearby so spawning pol");
			avoidDirs = new Direction[1];
			spawnPoliticians(true);
		}
		// Save up for big boi attacking poli
		else if(saveForAttack){
			spawnSave();
		}
		else if(attackTargetInfo != null){
			if(attackTargetInfo.team == myTeam.opponent()){
				Log.log("Spawning eco buildup against opponent");
				int[] order = {0, 1, 5};
				spawnOrder(order);
			}
			else{
				Log.log("Spawning eco buildup against neutral");
				int[] order = {0, 2, 3};
				spawnOrder(order);
			}
		}
		else if(turnCount < 300){
			Log.log("Spawning early game");
			int[] order = {0, 3, 0, 3, 0, 3, 2, 3, 0, 1, 0, 3, 2, 3};
			spawnOrder(order);
		}
		else{
			System.out.println("Spawning late game");
			int[] order = {1, 0, 3, 0, 3, 2, 5, 0, 3, 1, 0, 3, 0, 5, 3};
			spawnOrder(order);
		}

//		else if(attackTargetInfo != null){
//			Log.log("Spawning eco buildup");
//			int[] order = {0, 3, 2, 1, 0, 3};
//			spawnOrder(order);
//		}
//		else if(activeSlands[currRound] < slandsAlive / 5){
//			Log.log("Spawning slands cuz we're low on active ones rn");
//			spawnSlanderers();
//		}
//		else{
//			Log.log("Spawning early game");
//			int[] order = {0, 3, 2, 3, 0, 1, 3};
//			spawnOrder(order);
//		}
//		else{
//			Log.log("Spawning late game");
//			int[] order = {1, 0, 3, 0, 3, 2, 3, 0, 3, 1, 0, 3, 0, 3, 3};
//			spawnOrder(order);
//		}
		// If you didn't have the eco to spawn anything this round, j spawn a muck
		spawnMucks(true, false);
	}

	public void spawnOrder(int[] order) throws GameActionException {
		int type = order[numSpawned % order.length];
		System.out.println("Spawn type: " + type);
		if(type == 0){ spawnSlanderers(); }
		if(type == 1){ spawnPoliticians(true); }
		if(type == 2){ spawnPoliticians(false); }
		if(type == 3){ spawnMucks(true, false); }
		if(type == 4){ spawnMucks(false, false); }
		if(type == 5){ spawnMucks(false, true); }
	}

	public void spawnSave() throws GameActionException {
		if(rc.getInfluence() - EC_MIN_INFLUENCE >= capturerInf){
			Direction[] spawnDirs = Navigation.closeDirections(myLoc.directionTo(attackTargetInfo.loc));
			Log.log("Spawning big captuerer boi!");
			if(Util.tryBuild(RobotType.POLITICIAN, spawnDirs, capturerInf)){
				attackTarget = attackTargetInfo.loc;
				saveForAttack = false;
				capturerInf = -1;
				avoidDirs = new Direction[1];
			}
		}
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
						for(int i = 0; i < 50; i++){
							slandBenefits[currRound + i] += Util.slandBenefitPerRound(spawnInfo.spawnInfluence);
							activeSlands[currRound + i] += 1;
						}
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
					if(info.type == RobotType.SLANDERER){
						slandsAlive--;
						for(int j = currRound; j < info.spawnRound + 50; j++){
							slandBenefits[currRound + j] -= Util.slandBenefitPerRound(info.spawnInfluence);
							activeSlands[currRound + j] -= 1;
						}
					}
					else if(info.type == RobotType.POLITICIAN && info.spawnInfluence % 2 == 0){ attackersAlive--; }
					else if(info.type == RobotType.POLITICIAN && info.spawnInfluence % 2 == 0){ defendersAlive--; }
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

	public Direction enemyPoliNearby(){
		int threshold = 9;
		Direction closestDir = null;
		int closestDist = Integer.MAX_VALUE;
		for(int i = 0; i < nearby.length; i++){
			RobotInfo info = nearby[i];
			// If there's an enemy poli that can kill me, return that info
			if(info.getTeam() == myTeam.opponent() && info.getType() == RobotType.POLITICIAN && info.getConviction() > rc.getConviction()){
				int dist = myLoc.distanceSquaredTo(info.getLocation());
				if(dist <= threshold && dist < closestDist){
					closestDist = dist;
					closestDir = myLoc.directionTo(info.getLocation());
				}
			}
		}
		return closestDir;
	}


	public void setMuckFlag(int numMucks) throws GameActionException {
		int purpose = 6;
		int[] xy = Comms.mapLocationToXY(lastBuilt);
		int[] flagArray = {purpose, 4, numMucks, 6, xy[0], 7, xy[1], 7};
		int flag = Comms.concatFlag(flagArray);
//		Comms.setFlag(flag);
		nextRoundFlag = flag;
		Log.log("Setting muck flag: " + Comms.printFlag(flag));
	}

	public void spawnMucks(boolean scout, boolean buffraker) throws GameActionException {
		// TODO: Fix this
		Log.log("spawnMucks -- Cooldown left: " + rc.getCooldownTurns());
		Log.log("Spawning scout muck: " + scout + ", num of mucks spawned: " + mucksSpawned);
		if(buffraker){
			scout = false;
		}
		if (scout) {
			Direction[] spawnDirs = Navigation.closeDirections(Navigation.directions[mucksSpawned % 8]);
			if (mucksSpawned < 24 && !setFlagThisRound) {
				int numMucks = mucksSpawned;
				if (Util.tryBuild(RobotType.MUCKRAKER, spawnDirs, 1)) {
					Log.log("Successfully built muck, now setting my flag to display " + numMucks);
					setMuckFlag(numMucks);
					return;
				}
				else{
					Log.log("Failed to build muck rip");
				}
			}
			scout = false;
		}
		if (!scout) {
			Log.log("Spawning attack muck!");
			Direction[] spawnDirs = Navigation.randomizedDirs();
			if (attackTarget != null) {
				spawnDirs = Navigation.closeDirections(myLoc.directionTo(attackTarget));
			}
			int influence = 1;
			if(buffraker){
				System.out.println("SPAWNING BUFFRAKER!!!");
				double divFactor = 3.0;
				influence = Util.getSpawnInfluence(1, rc.getInfluence() - EC_MIN_INFLUENCE, divFactor, false, false);
			}
			// Build attacking muck - influence of 1
			if (Util.tryBuild(RobotType.MUCKRAKER, spawnDirs, influence)) {
				return;
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
		double divFactor = 1.15;
		int spawnInfluence = Util.getSpawnInfluence(SLAND_MIN_COST, rc.getInfluence() - EC_MIN_INFLUENCE, divFactor, false, false);
		if(spawnInfluence == -1){
			return;
		}

		Direction[] spawnDirs = Navigation.randomizedDirs(); // Default: random spawn direction
		// Try spawning in the direction opposite of the nearest enemy EC
		DetectedInfo enemyECInfo = Util.getClosestEnemyEC();
		if(enemyECInfo != null){
			spawnDirs = Navigation.closeDirections(myLoc.directionTo(enemyECInfo.loc).opposite());
		}
		Util.tryBuild(RobotType.SLANDERER, spawnDirs, spawnInfluence);
	}

	public void spawnPoliticians(boolean defense) throws GameActionException {
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
		if (defense) {
			spawnInfluence = Util.getSpawnInfluence(DEF_POLI_MIN_COST, rc.getInfluence() - EC_MIN_INFLUENCE, 20, true, false);
			if(nearestMuck != null){
				// If you sense an enemy muck nearby, spawn it in the direction of the muck
				spawnDirs = Navigation.closeDirections(myLoc.directionTo(nearestMuck.location));
			}
		}
		else {
			spawnInfluence = Util.getSpawnInfluence(ATK_POLI_MIN_COST, rc.getInfluence() - EC_MIN_INFLUENCE, 2.5, false, true);
			// Spawn it in the direction of the target ur attacking
			if(attackTargetInfo != null){
				spawnDirs = Navigation.closeDirections(myLoc.directionTo(attackTargetInfo.loc));
			}
		}
		if(spawnInfluence == -1){
			return;
		}

		if(Util.tryBuild(RobotType.POLITICIAN, spawnDirs, spawnInfluence)){
			Log.log("Successfully built politician!");
		}
		else{
			Log.log("Failed to build defense politician :((");
		}
	}

	// Find a target EC to attack!
	public void setAttackTarget() throws GameActionException {
		if(attackTargetInfo != null){
			DetectedInfo[] targetInfo = Util.getCorrespondingRobots(null, null, attackTargetInfo.loc);
			assert(targetInfo.length > 0);
			Log.log(targetInfo[0].team.toString());
			if(targetInfo[0].team == myTeam){
				// Captured the target, lets go attack a diff target
				Log.log("We captured the target bois!");
				attackTarget = null;
				attackTargetInfo = null;
			}
		}
		if(attackTarget != null){
			return;
		}

		DetectedInfo[] allECInfo = Util.getCorrespondingRobots(null, RobotType.ENLIGHTENMENT_CENTER, null);
		Log.log("Num of ECs known: " + allECInfo.length);
		// Attack the closest EC
		DetectedInfo bestTarget = null;
		int bestHeuristic = Integer.MAX_VALUE;
		for(DetectedInfo info : allECInfo){
			if(info.team == myTeam){
				continue;
			}
			// Calculate a heuristic for each EC
			int dist = info.loc.distanceSquaredTo(myLoc);
			int inf = info.influence;
			Team team = info.team;
			int heuristic = (inf / 10) + (int)Math.sqrt(dist);
			if(team == myTeam.opponent()){
				heuristic *= 2.5;
			}
			if(heuristic < bestHeuristic){
				bestHeuristic = heuristic;
				bestTarget = info;
			}
		}

		if(bestTarget != null){
			attackTargetInfo = bestTarget;
		}
	}

	public void checkReadyToAttack() throws GameActionException {
		if(attackTarget != null || attackTargetInfo == null){
			return;
		}
		// Check if ready to attack
		int attackInf = 0;
		for(int i = 0; i < attackPolisSpawned; i++){
			if(!attackPoliInfo[i].alive){ continue; }
			attackInf += attackPoliInfo[i].spawnInfluence - 10;
		}
		Log.log("Potential attack target: " + attackTargetInfo.loc.toString());
		Log.log("Attack influence: " + attackInf);
		if(attackInf > attackTargetInfo.influence * 2.0){
			attackTarget = attackTargetInfo.loc;
			return;
		}
		double multiple = 1.5;
		if(attackTargetInfo.team == myTeam.opponent()){
			multiple = 2.5;
		}
		int infNeeded = (int)(attackTargetInfo.influence * multiple) - attackInf + 10;
		int infExpected = getExpectedInfluence(currRound + 10);
		Log.log("Inf needed: " + infNeeded);
		Log.log("Inf expected: " + infExpected);
		if(infExpected > infNeeded + EC_MIN_INFLUENCE){
			capturerInf = infNeeded;
			saveForAttack = true;
			avoidDirs = new Direction[1];
			avoidDirs[0] = myLoc.directionTo(attackTargetInfo.loc);
		}
	}

	public void updateFlag() throws GameActionException {
		int purpose = 4;
		int[] xy = {0, 0};
		int attackType = 3; // Stop attacking
		if(attackTarget != null){
			System.out.println("Setting flag to attack!: " + attackTarget.toString());
			xy = Comms.mapLocationToXY(attackTarget);
			if(attackTargetInfo.team == myTeam.opponent()){
				attackType = 2; // Everyone attack!
			}
			else{
				attackType = 0; // Only politicians attack!
			}
		}
		int[] flagArray = {purpose, 4, xy[0], 7, xy[1], 7, attackType, 2};
		int flag = Comms.concatFlag(flagArray);
		Comms.setFlag(flag);
	}

	public RobotInfo enemyMuckNearby() {
		for(int i = 0; i < nearby.length; i++){
			RobotInfo info = nearby[i];
			if(info.getType() == RobotType.MUCKRAKER && info.getTeam() == myTeam.opponent()){
				return info;
			}
		}
		return null;
	}

	// Returns the amount of influence you would have on roundNum if you saved till then
	public int getExpectedInfluence(int roundNum){
		int expectedInf = rc.getInfluence();
		for(int i = currRound; i <= roundNum; i++){
			expectedInf += slandBenefits[i]; // How much influence you expect to gain from slanderers
			expectedInf += 0.2 * Math.sqrt(i); // How much passive influence you expect to gain
		}
		return expectedInf;
	}

	public void bid() throws GameActionException {
		if(rc.getTeamVotes() > 750){
			return;
		}
		int bid = 0;

		if(lastBid == -1){
			bid = rc.getInfluence() / 20;
		}
		else{
			if(rc.getTeamVotes() > numVotes){
				// We won the vote, so stick with that bid
				bid = (int)(lastBid/ 1.2);
			}
			else{
				// Double our vote
				bid = lastBid * 2;
			}

		}
		if(bid > rc.getInfluence()){
			bid = rc.getInfluence() - EC_MIN_INFLUENCE;
		}
		bid = Math.min(bid, (int)(rc.getInfluence() * maxBidProportion));
		Log.log("Curr inf: " + rc.getInfluence());
		Log.log("Max bid: " + (int)(rc.getInfluence() * maxBidProportion));
		Log.log("Bidding: " + bid);
		if(rc.canBid(bid)){
			rc.bid(bid);
			lastBid = bid;
		}
		numVotes = rc.getTeamVotes();
	}

}
