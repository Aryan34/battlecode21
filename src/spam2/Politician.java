package spam2;

import battlecode.common.*;

// NOTE: Defense politicians have odd influence, attack politicians have even influence

public class Politician extends Robot {

	boolean inGrid = false;
	boolean circlingCCW = true;
	boolean isAttacking;

	public Politician (RobotController rc) throws GameActionException {
		super(rc);
		isAttacking = rc.getInfluence() % 2 == 0;
	}

	public void run() throws GameActionException {
		super.run();

		if(creatorLoc == null){
			runConverted();
			isAttacking = true;
		}
		else {
			Comms.checkFlag(creatorID);
			Log.log(isAttacking ? "Attacking poli" : "Defensive poli");

			if (isAttacking && attackTarget != null) {
				Log.log("Attacking: " + attackTarget.toString());
				runAttack();
			} else {
				if (rc.getInfluence() > 700 && rc.getEmpowerFactor(myTeam, 0) >= 1.1) {
					Log.log("Sacrificial politician activated");
					int dist = myLoc.distanceSquaredTo(creatorLoc);
					if (rc.canEmpower(dist) && rc.detectNearbyRobots(dist).length == 1) {
						rc.empower(dist);
					} else {
						nav.circle(true, 1);
					}
				} else {
					runEco(nearby);
				}
			}

			if (!setFlagThisRound) {
				Comms.setFlag(0);
			}
		}
	}

	public void runConverted() throws GameActionException {
		// TODO: Write code for this
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

		// If you're too close, move farther away
		if(Util.getGridSquareDist(myLoc, creatorLoc) < minDist){
			Log.log("Going farther!");
			Direction targetDir = creatorLoc.directionTo(myLoc);
			Direction[] options = {targetDir, targetDir.rotateRight(), targetDir.rotateLeft(), targetDir.rotateRight().rotateRight(), targetDir.rotateLeft().rotateLeft()};
			nav.tryMove(options);
//			nav.goTo(myLoc.add(creatorLoc.directionTo(myLoc)));
		}
		// Always make sure theres a friendly slanderer in site
		else if(!spotted && Util.getGridSquareDist(myLoc, creatorLoc) > minDist){
			Log.log("Going closer!");
			Direction targetDir = myLoc.directionTo(creatorLoc);
			Direction[] options = {targetDir, targetDir.rotateRight(), targetDir.rotateLeft(), targetDir.rotateRight().rotateRight(), targetDir.rotateLeft().rotateLeft()};
			nav.tryMove(options);
//			nav.goTo(myLoc.add(myLoc.directionTo(creatorLoc)));
		}
		else{
			chooseSide();
			nav.circle(circlingCCW, minDist);
			Log.log("Circling: " + circlingCCW);
		}

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
			nav.goTo(attackTarget);
		}
		else if (rc.canEmpower(dist)) {
			Log.log("Empowering...distance to target: " + dist);
			rc.empower(dist);
		}
	}

	public void killNearbyMucks() throws GameActionException {
		MapLocation[] enemyMucks = new MapLocation[nearby.length]; int idx1 = 0;
		MapLocation[] friendlySlands = new MapLocation[nearby.length]; int idx2 = 0;
		for (RobotInfo info : nearby) {
			// Only kill enemy mucks, and only kill them if there's a sland nearby (otherwise follow them around)
			if (info.team == myTeam.opponent() && info.type == RobotType.MUCKRAKER) {
				enemyMucks[idx1] = info.getLocation(); idx1++;
			}
			else if (info.team == myTeam && info.type == RobotType.POLITICIAN && Util.isSlanderer(info.getID())) {
				friendlySlands[idx2] = info.getLocation(); idx2++;
			}
		}
		// Find the biggest threat (the muckracker thats closest to a friendly sland)
		MapLocation biggestThreat = null;
		int closestDist = Integer.MAX_VALUE;
		for(int i = 0; i < idx1; i++){
			for(int j = 0; j < idx2; j++){
				// Check if any of the enemy mucks can kill friendly slands
				int dist = enemyMucks[i].distanceSquaredTo(friendlySlands[j]);
				Log.log("Enemy muck: " + enemyMucks[i].toString());
				Log.log("Friendly sland: " + friendlySlands[j].toString());
				Log.log("Dist: " + dist);
				if(dist < closestDist){
					closestDist = dist;
					biggestThreat = enemyMucks[i];
				}
			}
		}
		// If there's no mucks, we gucci
		if(biggestThreat == null){
			return;
		}
		// If the muck can kill our sland
		Log.log("Biggest threat: " + biggestThreat.toString());
		Log.log("It's distance to our closest sland is: " + closestDist);
		if(closestDist < RobotType.MUCKRAKER.actionRadiusSquared){
			// If we can kill the muck, go for it
			int attackDist = myLoc.distanceSquaredTo(biggestThreat);
			Log.log("Biggest threat can kill our closest slanderer, and is at: " + biggestThreat.toString());
			if(attackDist <= RobotType.POLITICIAN.actionRadiusSquared){
				if(rc.canEmpower(attackDist)){
					rc.empower(attackDist);
				}
			}
			// Otherwise, go closer to it so we can kill it
			else{
				nav.goTo(biggestThreat);
			}
		}
		else if(biggestThreat != null){
			// TODO: Go to the area from which I'll be able to kill the most mucks biggest threat
			nav.goTo(biggestThreat);
		}
		else{
			for(int radius = 1; radius <= myType.actionRadiusSquared; radius++){
				int canKill = 0;
				RobotInfo[] robotsInKillRange = rc.senseNearbyRobots(radius);
				for(RobotInfo info : robotsInKillRange){
					if(info.getType() == RobotType.MUCKRAKER && info.getTeam() == myTeam.opponent() && info.getInfluence() < (rc.getInfluence() - 10) / robotsInKillRange.length){
						canKill++;
					}
				}
				if(canKill >= 2 && rc.canEmpower(radius)){
					rc.empower(radius);
				}
			}
		}

	}

	public void killEnemyPols(int minKills) throws GameActionException {
		int maxKilledPols = 0;
		int bestAttackRadius = 0;
		int[] attackRadii = {1, 2, 4, 5, 8, 9};
		for (int radius : attackRadii) {
			int killedPols = 0;
			RobotInfo[] nearby = rc.senseNearbyRobots(radius);
			int damage = (int)((rc.getConviction() - 10) * rc.getEmpowerFactor(myTeam, 0) / nearby.length);
			for (RobotInfo info : rc.senseNearbyRobots(radius, myTeam.opponent())) {
				if (info.getType() == RobotType.POLITICIAN && info.getConviction() <= damage) {
					killedPols++;
				}
			}
			if (killedPols > maxKilledPols) {
				maxKilledPols = killedPols;
				bestAttackRadius = radius;
			}
		}

		if (maxKilledPols >= minKills) {
			rc.empower(bestAttackRadius);
		}
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