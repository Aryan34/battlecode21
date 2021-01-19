package spam;

import battlecode.common.*;

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

		if(isAttacking && attackTarget != null){
			System.out.println("Attacking: " + attackTarget.toString());
			runAttack();
		}
		else {
			runEco(nearby);
		}
	}

	public void runEco(RobotInfo[] nearby) throws GameActionException {
		killNearby();
		int minDist = 4; // Default distance
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
			// Stay a gridDistance of atleast two farther away from the nearest slanderer
			int gridDist = Util.getGridSquareDist(info.getLocation(), creatorLoc);
			minDist = Math.max(minDist, gridDist + 2);
		}
		System.out.println("My min dist: " + minDist);

//		inGrid = Util.isGridSquare(myLoc, creatorLoc) && Util.getGridSquareDist(myLoc, creatorLoc) >= minDist;
		inGrid = Util.isGridSquare(myLoc, creatorLoc) && Util.getGridSquareDist(myLoc, creatorLoc) >= minDist;
		System.out.println("Am I on the grid? " + inGrid);

		if(!inGrid){
			System.out.println("Going to grid at minDist: " + minDist);
			nav.goToGrid(minDist);
		}
		else{
			checkOnWall();
			System.out.println("Alr on grid at minDist: " + minDist + ", going ccw? " + ccw);
			nav.runAroundGrid(minDist, ccw);
		}
	}

	public void runAttack() throws GameActionException {
		killNearby();
		if(rc.canSenseLocation(attackTarget)){
			if(rc.senseRobotAtLocation(attackTarget).getTeam() == myTeam){
				attackTarget = null;
				return;
			}
		}
		if (rc.getLocation().distanceSquaredTo(attackTarget) > 1) {
			nav.goTo(attackTarget);
		}

		else if (rc.canEmpower(rc.getLocation().distanceSquaredTo(attackTarget))) {
			System.out.println("Empowering...distance to target: " + rc.getLocation().distanceSquaredTo(attackTarget));
			System.out.println(rc.getLocation());
			System.out.println(attackTarget);
			rc.empower(rc.getLocation().distanceSquaredTo(attackTarget));
		}
	}

	public void killNearby() throws GameActionException {
		for (RobotInfo info : rc.senseNearbyRobots(5, myTeam.opponent())) {
			if (info.type != RobotType.ENLIGHTENMENT_CENTER && myLoc.distanceSquaredTo(info.location) < 3) {
				rc.empower(2);
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

//	public void runAlongWall() throws GameActionException {
//		MapLocation currTarget = wallCheckLocs[wallCheckIdx];
//		if(myLoc.distanceSquaredTo(currTarget) < 4){
//			wallCheckIdx += 1;
//			wallCheckIdx %= wallCheckLocs.length;
//			runAlongWall();
//		}
//		else{
//			nav.goTo(currTarget);
//		}
//	}

	// Example bot attacking code
//		Team enemy = rc.getTeam().opponent();
//		int actionRadius = rc.getType().actionRadiusSquared;
//		RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
//		if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
//			System.out.println("empowering...");
//			rc.empower(actionRadius);
//			System.out.println("empowered");
//			return;
//		}
}