package spam;

import battlecode.common.*;

// NOTE: Defense politicians have odd influence, attack politicians have even influence

public class Politician extends Robot {

	boolean inGrid = false;
	boolean ccw = true;
	boolean isAttacking;

	public Politician (RobotController rc) throws GameActionException {
		super(rc);
		isAttacking = rc.getInfluence() % 2 == 0;
	}

	public void run() throws GameActionException {
		super.run();
		Comms.checkFlag(creatorID);
		System.out.println(isAttacking ? "Attacking poli" : "Defensive poli");

		if(isAttacking && attackTarget != null){
			System.out.println("Attacking: " + attackTarget.toString());
			runAttack();
		}
		else {
			if (rc.getInfluence() > 700 && rc.getEmpowerFactor(myTeam, 0) >= 1.1){
				System.out.println("Sacrificial politician activated");
				int dist = myLoc.distanceSquaredTo(creatorLoc);
				if (rc.canEmpower(dist) && rc.detectNearbyRobots(dist).length == 1){
					rc.empower(dist);
				}
				else{
					nav.circle(true, 1);
				}
			}
			else {
				runEco(nearby);
			}
		}

		if(!setFlagThisRound){
			Comms.setFlag(0);
		}
	}

	public void runEco(RobotInfo[] nearby) throws GameActionException {
		killNearby();
		int minDist = 4; // Default distance
		boolean spotted = false;
		for(RobotInfo info : nearby){
			// Filter out everything except friendly slanderers / politicians
			if(info.getType() != RobotType.POLITICIAN || info.getTeam() != myTeam){
				continue;
			}
			// Unfortunately, slanderers appear as politicians, so we have to read their flag to figure out if they're actually politicians
			typeInQuestion = null;
			Comms.checkFlag(info.getID());
			if(typeInQuestion != RobotType.SLANDERER){
				continue;
			}
			// Stay a gridDistance of atleast two farther away from the nearest slanderer that you're currently guarding
			if(Navigation.getAngleDiff(creatorLoc, myLoc, info.getLocation()) > 30){
				System.out.println("Friendly slanderer at: " + info.getLocation() + ", but not within my angle");
				System.out.println(Navigation.getAngleDiff(creatorLoc, myLoc, info.getLocation()));
				continue;
			}
			int gridDist = Util.getGridSquareDist(info.getLocation(), creatorLoc);
			System.out.println("Friendly slanderer at: " + info.getLocation());
			minDist = Math.max(minDist, gridDist + 1);
			spotted = true;
		}
		System.out.println("My min dist: " + minDist);
		System.out.println("My grid dist: " + Util.getGridSquareDist(myLoc, creatorLoc));

//		inGrid = Util.isGridSquare(myLoc, creatorLoc) && Util.getGridSquareDist(myLoc, creatorLoc) >= minDist;
//		System.out.println("Am I on the grid? " + inGrid);
//
//		if(!inGrid){
//			System.out.println("Going to grid at minDist: " + minDist);
//			nav.goToGrid(minDist);
//		}
//		else{
//			checkOnWall();
//			System.out.println("Alr on grid at minDist: " + minDist + ", going ccw? " + ccw);
//			nav.runAroundGrid(minDist, ccw);
//		}

		// If you're too close, move farther away
		if(Util.getGridSquareDist(myLoc, creatorLoc) < minDist){
			System.out.println("Going farther!");
			Direction targetDir = creatorLoc.directionTo(myLoc);
			Direction[] options = {targetDir, targetDir.rotateRight(), targetDir.rotateLeft(), targetDir.rotateRight().rotateRight(), targetDir.rotateLeft().rotateLeft()};
			nav.tryMove(options);
//			nav.goTo(myLoc.add(creatorLoc.directionTo(myLoc)));
		}
		// Always make sure theres a friendly slanderer in site
		else if(!spotted && Util.getGridSquareDist(myLoc, creatorLoc) > minDist){
			System.out.println("Going closer!");
			Direction targetDir = myLoc.directionTo(creatorLoc);
			Direction[] options = {targetDir, targetDir.rotateRight(), targetDir.rotateLeft(), targetDir.rotateRight().rotateRight(), targetDir.rotateLeft().rotateLeft()};
			nav.tryMove(options);
//			nav.goTo(myLoc.add(myLoc.directionTo(creatorLoc)));
		}
		else{
			nav.circle(true, minDist);
			System.out.println("Circling!");
		}

	}

	public void runAttack() throws GameActionException {
		if(rc.canSenseLocation(attackTarget)){
			if(rc.senseRobotAtLocation(attackTarget).getTeam() == myTeam){
				attackTarget = null;
				return;
			}
		}
		int dist = myLoc.distanceSquaredTo(attackTarget);
		if (dist > 1) {
			nav.goTo(attackTarget);
		}
		else if (rc.canEmpower(dist)) {
			System.out.println("Empowering...distance to target: " + dist);
			rc.empower(dist);
		}
	}

	public void killNearby() throws GameActionException {
		for (RobotInfo info : nearby) {
			// Only kill enemy mucks, and only kill them if there's a sland nearby (otherwise follow them around)
			if (info.team == myTeam.opponent() && myLoc.distanceSquaredTo(info.location) < 3 && info.type == RobotType.MUCKRAKER) {
				if(rc.canEmpower(2)){
					rc.empower(2);
				}
			}
		}
	}

	public void checkOnWall() throws GameActionException {
		// Move around
		Direction start = myLoc.directionTo(creatorLoc);
		Direction dir = start;
		boolean occupied = true;
		for(int i = 0; i <= 4; i++){
			MapLocation newLoc = myLoc.add(dir);
			// You've run into a wall! So start going the other way
			if(!rc.onTheMap(newLoc)){
				ccw = !ccw;
				return;
			}
			if(rc.canMove(dir) || !rc.isLocationOccupied(newLoc)){
				occupied = false;
			}
			if(ccw){ dir = dir.rotateRight(); }
			else{ dir = dir.rotateLeft(); }
		}
		if(occupied){
			ccw = !ccw;
			return;
		}
	}

}