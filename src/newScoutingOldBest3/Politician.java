package newScoutingOldBest3;

import battlecode.common.*;

// NOTE: Defense politicians have odd influence, attack politicians have even influence

public class Politician extends Robot {

	boolean circlingCCW = true;
	boolean isAttacking;

	public Politician (RobotController rc) throws GameActionException {
		super(rc);
		isAttacking = rc.getInfluence() % 2 == 0;
	}

	public void run() throws GameActionException {
		super.run();

		if(!rc.canGetFlag(creatorID)){
			creatorLoc = null;
			creatorID = 0;
		}

		if (rc.getTeamVotes() < 750 && rc.getRoundNum() == 1490) {
			runSuicide();
		}
		else if(creatorLoc == null){
			isAttacking = true;
			runConverted(nearby);
		}
		else {
			Comms.checkFlag(creatorID);
			Log.log(isAttacking ? "Attacking poli" : "Defensive poli");

			if (isAttacking && attackTarget != null) {
				Log.log("Attacking: " + attackTarget.toString());
				runAttack();
			} else {
				runEco(nearby);
			}

			if (!setFlagThisRound) {
				Comms.setFlag(0);
			}
		}
	}

	public void runConverted(RobotInfo[] nearby) throws GameActionException {
		for(RobotInfo info : nearby){
			int dist = myLoc.distanceSquaredTo(info.getLocation());
			if((info.getType() == RobotType.POLITICIAN || info.getType() == RobotType.ENLIGHTENMENT_CENTER) && info.getTeam() != myTeam && info.getConviction() > Math.min(50, rc.getConviction())){
				Log.log("Tryna convert enemy at: " + info.getLocation());
				boolean moved = false;
				if(dist > 2){
					moved = nav.goTo(info.getLocation());
				}
				if (!moved && rc.canEmpower(dist)){
					rc.empower(dist);
				}
			}
			else if(info.getType() == RobotType.ENLIGHTENMENT_CENTER && info.getTeam() == myTeam){
				Log.log("Found a new home! at: " + info.getLocation());
				creatorLoc = info.getLocation();
				creatorID = info.getID();
				runEco(nearby);
				return;
			}
			else if(info.getType() == RobotType.MUCKRAKER && info.getTeam() != myTeam && rc.getConviction() < 50){
				Log.log("Attacking enemy muck at: " + info.getLocation());
				boolean moved = false;
				if(dist > 2){
					moved = nav.goTo(info.getLocation());
				}
				if (!moved && rc.canEmpower(dist)){
					rc.empower(dist);
				}
			}
			else if (info.getTeam() == myTeam && (info.getType() == RobotType.POLITICIAN || info.getType() == RobotType.MUCKRAKER)){
				nav.tryMove(myLoc.directionTo(info.getLocation()));
			}
			else{
				nav.tryMove(nav.randomDirection());
			}
		}
	}

	public void runEco(RobotInfo[] nearby) throws GameActionException {
		killNearbyMucks();
		int minDist = 4; // Default distance
		boolean spotted = false;
		for(RobotInfo info : nearby){
			// Filter out everything except friendly slanderers / politicians
			if(info.getType() != RobotType.POLITICIAN || info.getTeam() != myTeam){
				continue;
			}
			// Unfortunately, slanderers appear as politicians, so we have to read their flag to figure out if they're actually politicians
			if(!Util.isSlanderer(info.getID())){
				continue;
			}
			// Stay a gridDistance of atleast two farther away from the nearest slanderer that you're currently guarding
			if(Navigation.getAngleDiff(creatorLoc, myLoc, info.getLocation()) > 30){
				Log.log("Friendly slanderer at: " + info.getLocation() + ", but not within my angle");
				Log.log("" + Navigation.getAngleDiff(creatorLoc, myLoc, info.getLocation()));
				continue;
			}
			int gridDist = Util.getGridSquareDist(info.getLocation(), creatorLoc);
			Log.log("Friendly slanderer at: " + info.getLocation());
			minDist = Math.max(minDist, gridDist + 2);
			spotted = true;
		}
		Log.log("My min dist: " + minDist);
		Log.log("My grid dist: " + Util.getGridSquareDist(myLoc, creatorLoc));

		nav.brownian();

//		// If you're too close, move farther away
//		if(Util.getGridSquareDist(myLoc, creatorLoc) < minDist){
//			Log.log("Going farther!");
//			Direction targetDir = creatorLoc.directionTo(myLoc);
//			Direction[] options = {targetDir, targetDir.rotateRight(), targetDir.rotateLeft(), targetDir.rotateRight().rotateRight(), targetDir.rotateLeft().rotateLeft()};
//			nav.tryMove(options);
////			nav.goTo(myLoc.add(creatorLoc.directionTo(myLoc)));
//		}
//		// Always make sure theres a friendly slanderer in site
//		else if(!spotted && Util.getGridSquareDist(myLoc, creatorLoc) > minDist){
//			Log.log("Going closer!");
//			Direction targetDir = myLoc.directionTo(creatorLoc);
//			Direction[] options = {targetDir, targetDir.rotateRight(), targetDir.rotateLeft(), targetDir.rotateRight().rotateRight(), targetDir.rotateLeft().rotateLeft()};
//			nav.tryMove(options);
////			nav.goTo(myLoc.add(myLoc.directionTo(creatorLoc)));
//		}
//		else{
//			chooseSide();
//			nav.circle(circlingCCW, minDist);
//			Log.log("Circling: " + circlingCCW);
//		}
	}

	public void runAttack() throws GameActionException {
		if(rc.canSenseLocation(attackTarget)){
			if(rc.senseRobotAtLocation(attackTarget).getTeam() == myTeam){
				creatorLoc = attackTarget;
				creatorID = rc.senseRobotAtLocation(attackTarget).getID();
				attackTarget = null;
				return;
			}
		}
		int dist = myLoc.distanceSquaredTo(attackTarget);
		if (dist > 1) {
			// TODO: If the EC is blocked off and fuzzynaving towards it doesn't work, j go yeet
			nav.goTo(attackTarget);
		}
		else if (senseFromLoc(myLoc, 1).length > 1) {
			Log.log("There's other troops nearby, so try moving around the EC to isolate?");
			Direction right = myLoc.directionTo(attackTarget).rotateRight();
			Direction left = myLoc.directionTo(attackTarget).rotateLeft();
			Direction[] tryLocs = {right, left};
			// Try moving around the EC to get to an open space
			if(rc.getCooldownTurns() < 1){
				Log.log("Cooldown < 1");
				if(!nav.tryMove(tryLocs)){
					// If you can't move around the EC, just empower from where u are
					Log.log("Couldn't go around the EC, so j gonna yeet it");
					if(rc.canEmpower(dist)){
						rc.empower(dist);
					}
				}
			}
		}
		else if(rc.canEmpower(dist)) {
			Log.log("Empowering...distance to target: " + dist);
			rc.empower(dist);
		}
	}

	public void runSuicide() throws GameActionException {
		if (rc.senseNearbyRobots(RobotType.POLITICIAN.actionRadiusSquared, myTeam.opponent()).length > 0) {
			rc.empower(RobotType.POLITICIAN.actionRadiusSquared);
		}
	}

//	public void jasonKill() throws GameActionException {
//		RobotInfo[] enemyTargets = new RobotInfo[nearby.length];
//		boolean[] threatening = new boolean[nearby.length];
//		int idx = 0;
//		RobotInfo[] friendlyPolis = new RobotInfo[nearby.length];
//		int idx2 = 0;
//		RobotInfo[] friendlySlands = new RobotInfo[nearby.length];
//		int idx3 = 0;
//		for (int i = 0; i < nearby.length; i++) {
//			RobotInfo info = nearby[i];
//			if(info.getTeam() == myTeam && Util.isSlanderer(info.getID())){
//				friendlySlands[idx3++] = info;
//			}
//		}
//		for (int i = 0; i < nearby.length; i++) {
//			RobotInfo info = nearby[i];
//			// Kill enemy politicians; tries to kill with smallest infl poli in range
//			if (info.getType() == RobotType.POLITICIAN && info.getTeam() != myTeam && info.getConviction() > 10) {
//				if(info.getLocation().distanceSquaredTo(creatorLoc) <= info.getType().sensorRadiusSquared){ // Using sensorRadius instead of actionRadius cuz i dont wanna cut it too close
//					threatening[idx] = true;
//				}
//				enemyTargets[idx++] = info;
//			}
//			else if (info.getType() == RobotType.MUCKRAKER && info.getTeam() != myTeam) { // Add mucks to the mix too
//				for(int j = 0; j < friendlySlands.length; j++){
//					if(info.getLocation().distanceSquaredTo(friendlySlands[j].getLocation()) <= info.getType().sensorRadiusSquared){
//						threatening[idx] = true;
//					}
//				}
//				enemyTargets[idx++] = info;
//			}
//			else if (info.getTeam() == myTeam && info.getType() == RobotType.POLITICIAN) {
//				friendlyPolis[idx2++] = info;
//			}
//		}
//		for (int j = 0; j < idx; j++) { // Go through all targets, find who should kill it
//			RobotInfo enemy = enemyTargets[j];
//			int minConv = Integer.MAX_VALUE; // minimum conviction of a poli that can kill the target
//			boolean someoneCanKill = true;
//			boolean thisCanKill = canKill(myLoc, enemy.getLocation(), rc.getConviction(), enemy.getConviction());
//			someoneCanKill |= thisCanKill;
//			int dist = myLoc.distanceSquaredTo(enemy.getLocation());
//			for (int i = 0; i < idx2; i++) {
//				RobotInfo friendly = friendlyPolis[i];
//				int tempDist = friendly.getLocation().distanceSquaredTo(enemy.getLocation());
//				if (tempDist <= RobotType.POLITICIAN.actionRadiusSquared) { // friendly can attack too
//					boolean tempCanKill = canKill(friendly.getLocation(), enemy.getLocation(), friendly.getConviction(), enemy.getConviction());
//					if (tempCanKill) {
//						minConv = Math.min(minConv, friendly.getConviction());
//					}
//				}
//				someoneCanKill |= thisCanKill;
//			}
//			if (rc.getConviction() <= minConv && rc.canEmpower(dist)) { // we have the lowest conv, should attack
//				rc.empower(dist); // TODO: Add code to move closer before empowering?
//				break;
//			}
//			if (!someoneCanKill) { // if no one can kill it, just attack anyway
//				if(threatening[j] && rc.canEmpower(dist)){
//					rc.empower(dist);
//				}
//				break;
//			}
//		}
//
//	}


	public void efficientKill() throws GameActionException {
		if (rc.senseNearbyRobots(9, myTeam.opponent()).length == 0) {
			return;
		}
		Log.log("Attempting to do efficient kill");
		int maxKills = -1;
		int bestRadius = 0;
		// TODO: better formula for conviction -> kill ratio
//		int maxConvictionPerKill = 3 + rc.getRoundNum() / 300;
		int maxConvictionPerKill = Integer.MAX_VALUE;
		int[] radii = {1, 2, 4, 5, 8, 9};

		for (int radius : radii) {
			Log.log("Testing radius: " + radius);
			int killCount = 0;
			RobotInfo[] nearbyRadius = rc.senseNearbyRobots(radius);
			RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(radius, myTeam.opponent());
			if (nearbyRadius.length == 0) {
				continue;
			}

			double netConvictionLost = rc.getConviction();
			int empowerStrength = (int)Math.floor(Math.floorDiv(rc.getConviction() - 10, nearbyRadius.length) * rc.getEmpowerFactor(myTeam, 0));
			Log.log("Empower strength: " + empowerStrength);
			for (int i = 0; i < nearbyEnemies.length; i++) {
				RobotInfo info = nearbyEnemies[i];
				if (info.getConviction() - empowerStrength <= -1.0) {
					killCount++;
				}
				netConvictionLost -= Math.min(empowerStrength, info.getConviction());
			}

			Log.log("Kill count: " + killCount);

			if (killCount > 0 && Math.floorDiv((int)netConvictionLost, killCount) <= maxConvictionPerKill && killCount > maxKills) {
				maxKills = killCount;
				bestRadius = radius;
			}
		}

		if (maxKills > 0) {
			Log.log("Found efficient kill(s)!");
			rc.empower(bestRadius);
		}
	}

	public void killNearbyMucks() throws GameActionException {
		// Try and kill enemy mucks, mainly the ones that are threatening friendly slands
		MapLocation[] enemyMucks = new MapLocation[nearby.length]; int idx1 = 0;
		MapLocation[] friendlySlands = new MapLocation[nearby.length + 1]; int idx2 = 0;

		for (int i = 0; i < nearby.length; i++) {
			RobotInfo info = nearby[i];
			// Only kill enemy mucks, and only kill them if there's a sland nearby (otherwise follow them around)
			if (info.team == myTeam.opponent() && info.type == RobotType.MUCKRAKER) {
				enemyMucks[idx1++] = info.getLocation();
			}
			else if (info.team == myTeam && info.type == RobotType.POLITICIAN && Util.isSlanderer(info.getID())) {
				friendlySlands[idx2++] = info.getLocation();
			}
		}

		// Add EC as something to defend
		friendlySlands[idx2++] = creatorLoc;

		// If there's no mucks, we gucci
		if(idx1 == 0){
			return;
		}

		// Find the biggest threat (the muckracker thats closest to a friendly sland)
		MapLocation biggestThreat = null;
		int biggestThreatInf = Integer.MAX_VALUE;
		int closestDist = Integer.MAX_VALUE;
		for(int i = 0; i < idx1; i++){
			for(int j = 0; j < idx2; j++){
				// Check if any of the enemy mucks can kill friendly slands
				int dist = enemyMucks[i].distanceSquaredTo(friendlySlands[j]);
				Log.debug("Enemy muck: " + enemyMucks[i].toString());
				Log.debug("Friendly sland: " + friendlySlands[j].toString());
				Log.debug("Dist: " + dist);
				if(dist < closestDist){
					closestDist = dist;
					biggestThreat = enemyMucks[i];
					biggestThreatInf = rc.senseRobotAtLocation(enemyMucks[i]).getConviction();
				}
			}
		}
		Log.log("Biggest threat: " + biggestThreat.toString());
		Log.log("It's distance to our closest sland is: " + closestDist);

		// If the muck can kill our sland
		if(closestDist <= RobotType.MUCKRAKER.actionRadiusSquared){
			// If we can kill the muck, go for it
			int attackDist = myLoc.distanceSquaredTo(biggestThreat);
			Log.log("Biggest threat can kill our closest slanderer, and is at: " + biggestThreat.toString());
			if(attackDist <= RobotType.POLITICIAN.actionRadiusSquared && canKill(myLoc, biggestThreat, rc.getConviction(), biggestThreatInf)){
				if(rc.canEmpower(attackDist)){
					Log.log("Empowering to kill the biggest threat");
					rc.empower(attackDist);
				}
			}
			// Otherwise, go closer to it so we can kill it
			else{
				Log.log("Going towards the biggestThreat to kill it");
				boolean moved = false;
				for(Direction dir : Direction.allDirections()){
					if(myLoc.add(dir).distanceSquaredTo(biggestThreat) < attackDist && rc.canMove(dir)){
						moved |= nav.tryMove(dir);
					}
				}
				if(!moved && rc.canEmpower(attackDist)){
					// If you can't go closer, then just try doing as much damage as possible
					rc.empower(attackDist);
				}
			}
		}
		else{
			efficientKill();
			if(biggestThreat != null && shouldApproach(biggestThreat)){
				nav.goTo(biggestThreat);
			}
		}
	}

	public boolean canKill(MapLocation polLoc, MapLocation muckLoc, int polInf, int muckInf) throws GameActionException {
		int dist = polLoc.distanceSquaredTo(muckLoc);
		if(dist > RobotType.POLITICIAN.actionRadiusSquared){
			return false;
		}

		RobotInfo[] withinRange = senseFromLoc(polLoc, polLoc.distanceSquaredTo(muckLoc));
		if(Math.floor((polInf - 10) / withinRange.length) > muckInf){
			Log.log("I can do enough damage to kill it!");
			return true;
		}
		return false;
	}

	public boolean shouldApproach(MapLocation muckLoc) throws GameActionException {
		RobotInfo closestPol = getClosestPol(muckLoc);
		if(closestPol.location.equals(myLoc)){
			return true;
		}
		if(closestPol.getConviction() < rc.senseRobotAtLocation(muckLoc).getConviction()){
			return true;
		}
		if(rc.getConviction() <= closestPol.getConviction() && rc.getConviction() > rc.senseRobotAtLocation(muckLoc).getConviction()){
			return true;
		}
		return false;
	}

	public RobotInfo getClosestPol(MapLocation muckLoc) throws GameActionException {
		int closestDist = myLoc.distanceSquaredTo(muckLoc);
		RobotInfo closest = rc.senseRobotAtLocation(myLoc);
		for(RobotInfo info : nearby){
			if(info.getType() == RobotType.POLITICIAN && info.getTeam() == myTeam && !Util.isSlanderer(info.getID())){
				int dist = info.getLocation().distanceSquaredTo(muckLoc);
				if(dist < closestDist){
					closestDist = dist;
					closest = info;
				}
			}
		}
		return closest;
	}

	public void checkOnWall() throws GameActionException {
		// North wall blocks CCW, East wall blocks ccw, West wall blocks cw, South wall blocks cw

	}

	public void chooseSide() throws GameActionException {
		int ccwCount = 0; int cwCount = 0;
		for(RobotInfo info : nearby){
			// Filter out everything except friendly slanderers / politicians
			if(info.getType() != RobotType.POLITICIAN || info.getTeam() != myTeam){
				continue;
			}
			// Unfortunately, slanderers appear as politicians, so we have to read their flag to figure out if they're actually politicians
			typeInQuestion = null;
			Comms.checkFlag(info.getID());
			if(typeInQuestion == RobotType.SLANDERER){
				continue;
			}
			// Check if there's more slanderers to ur ccw than ur cw
			double angleDiff = Navigation.getAngleDiff(creatorLoc, myLoc, info.getLocation());
			if(angleDiff > 45){
				continue;
			}
			if(Util.isCCW(myLoc, info.getLocation(), creatorLoc)){
				ccwCount++;
			}
			else{
				cwCount++;
			}
		}
		circlingCCW = cwCount > ccwCount;
	}

}