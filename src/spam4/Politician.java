package spam4;

import battlecode.common.*;

// NOTE: Defense politicians have odd influence, attack politicians have even influence

public class Politician extends Robot {

	final int LATTICE_MOD = 3;

	boolean circlingCCW = true;
	boolean isAttacking;

	MapLocation latticeTile = null;
	MapLocation[] emptyLatticeTiles = new MapLocation[100];

	public Politician(RobotController rc) throws GameActionException {
		super(rc);
		isAttacking = rc.getInfluence() % 2 == 0;
	}

	public void run() throws GameActionException {
		super.run();

		if (creatorLoc == null) {
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
				if (rc.getInfluence() > 700 && rc.getEmpowerFactor(myTeam, 0) >= 1.1) {
					Log.log("Sacrificial politician activated");
					int dist = myLoc.distanceSquaredTo(creatorLoc);
					if (rc.canEmpower(dist) && rc.detectNearbyRobots(dist).length == 1) {
						Log.log("Empowering for suicide");
						rc.empower(dist);
					}
					else {
						nav.circle(true, 1);
					}
				}
				else {
					runEco();
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

	public void runEco() throws GameActionException {
		killNearbyMucks();
		// move away from HQ
		if (myLoc.distanceSquaredTo(creatorLoc) <= 8) {
			Direction awayFromHQ = creatorLoc.directionTo(myLoc);
			Direction[] dirs = {awayFromHQ.rotateRight(), awayFromHQ, awayFromHQ.rotateLeft()};
			nav.tryMove(dirs);
		}
		else if (myLoc.x % LATTICE_MOD == 0 && myLoc.y % LATTICE_MOD == 0) {
			Log.log("On the lattice!");
		}
		else {
			findEmptyLatticeTiles();
		}
	}

	public void findEmptyLatticeTiles() throws GameActionException {
		if (latticeTile != null && rc.canSenseLocation(latticeTile) && !rc.isLocationOccupied(latticeTile)) {
			nav.goTo(latticeTile);
		}

		else {
			int index = 0;
			for (int dx = -4; dx <= 4; dx++) {
				for (int dy = -4; dy <= 4; dy++) {
					MapLocation loc = new MapLocation(myLoc.x + dx, myLoc.y + dy);
					if (loc.x % LATTICE_MOD == 0 && loc.y % LATTICE_MOD == 0 && rc.canSenseLocation(loc) && !rc.isLocationOccupied(loc)) {
						emptyLatticeTiles[index] = loc;
						index++;
					}
				}
			}

			if (index > 0) {
				int minDistSquared = 10000;
				MapLocation closestLatticeTile = null;
				for (int i = 0; i < index; i++) {
					MapLocation loc = emptyLatticeTiles[i];
					int dist = myLoc.distanceSquaredTo(loc);
					if (dist < minDistSquared) {
						minDistSquared = dist;
						closestLatticeTile = loc;
					}
				}
				latticeTile = closestLatticeTile;
				assert latticeTile != null;
				Log.log("Closest lattice tile: " + latticeTile.toString());
				nav.goTo(latticeTile);
			}
			else {
				Direction mainDir = creatorLoc.directionTo(myLoc);
				Direction[] dirs = {mainDir, mainDir.rotateLeft(), mainDir.rotateRight()};
				if (!nav.tryMove(dirs)) {
					nav.tryMove(nav.randomDirection());
				}
			}
		}
	}

	public void runAttack() throws GameActionException {
		if (rc.canSenseLocation(attackTarget)) {
			if (rc.senseRobotAtLocation(attackTarget).getTeam() == myTeam) {
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
		// Try and kill enemy mucks, mainly the ones that are threatening friendly slands
		MapLocation[] enemyMucks = new MapLocation[nearby.length];
		int idx1 = 0;
		MapLocation[] friendlySlands = new MapLocation[nearby.length + 1];
		int idx2 = 0;

		for (RobotInfo info : nearby) {
			// Only kill enemy mucks, and only kill them if there's a sland nearby (otherwise follow them around)
			if (info.team == myTeam.opponent() && info.type == RobotType.MUCKRAKER) {
				enemyMucks[idx1] = info.getLocation();
				idx1++;
			}
			else if (info.team == myTeam && info.type == RobotType.POLITICIAN && Util.isSlanderer(info.getID())) {
				friendlySlands[idx2] = info.getLocation();
				idx2++;
			}
		}

		// Add EC as something to defend
		friendlySlands[idx2] = creatorLoc;
		idx2++;

		// If there's no mucks, we gucci
		if (idx1 == 0) {
			return;
		}

		// Find the biggest threat (the muckracker thats closest to a friendly sland)
		MapLocation biggestThreat = null;
		int biggestThreatInf = Integer.MAX_VALUE;
		int closestDist = Integer.MAX_VALUE;
		for (int i = 0; i < idx1; i++) {
			for (int j = 0; j < idx2; j++) {
				// Check if any of the enemy mucks can kill friendly slands
				int dist = enemyMucks[i].distanceSquaredTo(friendlySlands[j]);
				Log.debug("Enemy muck: " + enemyMucks[i].toString());
				Log.debug("Friendly sland: " + friendlySlands[j].toString());
				Log.debug("Dist: " + dist);
				if (dist < closestDist) {
					closestDist = dist;
					biggestThreat = enemyMucks[i];
					biggestThreatInf = rc.senseRobotAtLocation(enemyMucks[i]).getInfluence();
				}
			}
		}
		Log.log("Biggest threat: " + biggestThreat.toString());
		Log.log("It's distance to our closest sland is: " + closestDist);

		// If the muck can kill our sland
		if (closestDist < RobotType.MUCKRAKER.actionRadiusSquared) {
			// If we can kill the muck, go for it
			int attackDist = myLoc.distanceSquaredTo(biggestThreat);
			Log.log("Biggest threat can kill our closest slanderer, and is at: " + biggestThreat.toString());
			if (attackDist <= RobotType.POLITICIAN.actionRadiusSquared && canKill(myLoc, biggestThreat, rc.getInfluence(), biggestThreatInf)) {
				if (rc.canEmpower(attackDist)) {
					Log.log("Empowering to kill the biggest threat");
					rc.empower(attackDist);
				}
			}
			// Otherwise, go closer to it so we can kill it
			else {
				nav.goTo(biggestThreat);
			}
		}
		else if (shouldApproach(biggestThreat)) {
			nav.goTo(biggestThreat);
		}
		else {
			int bestRad = -1;
			int bestKill = 0;
			for (int radius = 1; radius <= myType.actionRadiusSquared; radius++) {
				int canKill = 0;
				RobotInfo[] robotsInKillRange = senseFromLoc(myLoc, radius);
				for (RobotInfo info : robotsInKillRange) {
					if (info.getType() == RobotType.MUCKRAKER && info.getTeam() == myTeam.opponent() && info.getInfluence() < (rc.getInfluence() - 10) / robotsInKillRange.length) {
						canKill++;
					}
				}
				if (canKill > bestKill) {
					bestKill = canKill;
					bestRad = radius;
				}
			}
			if (bestKill >= 2 && rc.canEmpower(bestRad)) {
				Log.log("Empowering because I can kill multiple mucks");
				rc.empower(bestRad);
			}
		}
	}

	public boolean canKill(MapLocation polLoc, MapLocation muckLoc, int polInf, int muckInf) throws GameActionException {
		int dist = polLoc.distanceSquaredTo(muckLoc);
		if (dist > RobotType.POLITICIAN.actionRadiusSquared) {
			return false;
		}

		RobotInfo[] withinRange = senseFromLoc(polLoc, polLoc.distanceSquaredTo(muckLoc));
		if ((polInf - 10) / withinRange.length >= muckInf) {
			return true;
		}
		return false;
	}

	public boolean shouldApproach(MapLocation muckLoc) throws GameActionException {
		RobotInfo closestPol = getClosestPol(muckLoc);
		if (closestPol.location.equals(myLoc)) {
			return true;
		}
		if (closestPol.getInfluence() < rc.senseRobotAtLocation(muckLoc).getInfluence()) {
			return true;
		}
		return false;
	}

	public RobotInfo getClosestPol(MapLocation muckLoc) throws GameActionException {
		int closestDist = myLoc.distanceSquaredTo(muckLoc);
		RobotInfo closest = rc.senseRobotAtLocation(myLoc);
		for (RobotInfo info : nearby) {
			if (info.getType() == RobotType.POLITICIAN && info.getTeam() == myTeam && !Util.isSlanderer(info.getID())) {
				int dist = info.getLocation().distanceSquaredTo(muckLoc);
				if (dist < closestDist) {
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
		int ccwCount = 0;
		int cwCount = 0;
		for (RobotInfo info : nearby) {
			// Filter out everything except friendly slanderers / politicians
			if (info.getType() != RobotType.POLITICIAN || info.getTeam() != myTeam) {
				continue;
			}
			// Unfortunately, slanderers appear as politicians, so we have to read their flag to figure out if they're actually politicians
			typeInQuestion = null;
			Comms.checkFlag(info.getID());
			if (typeInQuestion == RobotType.SLANDERER) {
				continue;
			}
			// Check if there's more slanderers to ur ccw than ur cw
			double angleDiff = Navigation.getAngleDiff(creatorLoc, myLoc, info.getLocation());
			if (angleDiff > 45) {
				continue;
			}
			if (Util.isCCW(myLoc, info.getLocation(), creatorLoc)) {
				ccwCount++;
			}
			else {
				cwCount++;
			}
		}
		circlingCCW = cwCount > ccwCount;
	}

}
