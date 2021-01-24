package spam3;

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

		if(creatorLoc == null){
			isAttacking = true;
			runConverted();
		}
		else {
			Comms.checkFlag(creatorID);
			Log.log(isAttacking ? "Attacking poli" : "Defensive poli");

			if (isAttacking && attackTarget != null) {
				Log.log("Attacking: " + attackTarget.toString());
				runAttack();
			}
			else {
				runEco(nearby);
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
		if(dist > myType.actionRadiusSquared){
			nav.goTo(attackTarget);
		}
		if (dist > 1) {
			// If you're blocked out, just empower to kill all the guys blocking u and atleast do some damage to the EC
			boolean moved = nav.goTo(attackTarget);
			if(rc.getCooldownTurns() < 1 && !moved){
				rc.empower(dist);
			}
		}
		else if (senseFromLoc(myLoc, 1).length > 1) {
			Log.log("Moving in a circle to avoid annoying enemies");
			nav.circle(true, 1);
		}
		else{
			Log.log("Empowering...distance to target: " + dist);
			rc.empower(dist);
		}
	}

	public void killNearbyMucks() throws GameActionException {
		// Try and kill enemy mucks, mainly the ones that are threatening friendly slands
		MapLocation[] enemyMucks = new MapLocation[nearby.length]; int idx1 = 0;
		MapLocation[] friendlySlands = new MapLocation[nearby.length + 1]; int idx2 = 0;

		for (RobotInfo info : nearby) {
			// Only kill enemy mucks, and only kill them if there's a sland nearby (otherwise follow them around)
			if (info.team == myTeam.opponent() && info.type == RobotType.MUCKRAKER) {
				enemyMucks[idx1] = info.getLocation(); idx1++;
			}
			else if (info.team == myTeam && info.type == RobotType.POLITICIAN && Util.isSlanderer(info.getID())) {
				friendlySlands[idx2] = info.getLocation(); idx2++;
			}
		}

		// Add EC as something to defend
		friendlySlands[idx2] = creatorLoc; idx2++;

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
					biggestThreatInf = rc.senseRobotAtLocation(enemyMucks[i]).getInfluence();
				}
			}
		}
		Log.log("Biggest threat: " + biggestThreat.toString());
		Log.log("It's distance to our closest sland is: " + closestDist);

		// If the muck can kill our sland
		if(closestDist < RobotType.MUCKRAKER.actionRadiusSquared){
			// If we can kill the muck, go for it
			int attackDist = myLoc.distanceSquaredTo(biggestThreat);
			Log.log("Biggest threat can kill our closest slanderer, and is at: " + biggestThreat.toString());
			if(attackDist <= RobotType.POLITICIAN.actionRadiusSquared && canKill(myLoc, biggestThreat, rc.getInfluence(), biggestThreatInf)){
				if(rc.canEmpower(attackDist)){
					Log.log("Empowering to kill the biggest threat");
					rc.empower(attackDist);
				}
			}
			// Otherwise, go closer to it so we can kill it
			else{
				nav.goTo(biggestThreat);
			}
		}
		else if(shouldApproach(biggestThreat)){
			nav.goTo(biggestThreat);
		}
		else{
			int bestRad = -1;
			int bestKill = 0;
			for(int radius = 1; radius <= myType.actionRadiusSquared; radius++){
				int canKill = 0;
				RobotInfo[] robotsInKillRange = senseFromLoc(myLoc, radius);
				for(RobotInfo info : robotsInKillRange){
					if(info.getType() == RobotType.MUCKRAKER && info.getTeam() == myTeam.opponent() && info.getInfluence() < (rc.getInfluence() - 10) / robotsInKillRange.length){
						canKill++;
					}
				}
				if(canKill > bestKill){
					bestKill = canKill;
					bestRad = radius;
				}
			}
			if(bestKill >= 2 && rc.canEmpower(bestRad)){
				Log.log("Empowering because I can kill multiple mucks");
				rc.empower(bestRad);
			}
		}
	}

	public boolean canKill(MapLocation polLoc, MapLocation muckLoc, int polInf, int muckInf) throws GameActionException {
		int dist = polLoc.distanceSquaredTo(muckLoc);
		if(dist > RobotType.POLITICIAN.actionRadiusSquared){
			return false;
		}

		RobotInfo[] withinRange = senseFromLoc(polLoc, polLoc.distanceSquaredTo(muckLoc));
		if((polInf - 10) / withinRange.length >= muckInf){
			return true;
		}
		return false;
	}

	public boolean shouldApproach(MapLocation muckLoc) throws GameActionException {
		RobotInfo closestPol = getClosestPol(muckLoc);
		if(closestPol.location.equals(myLoc)){
			return true;
		}
		if(closestPol.getInfluence() < rc.senseRobotAtLocation(muckLoc).getInfluence()){
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