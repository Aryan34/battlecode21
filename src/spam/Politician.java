package spam;

import battlecode.common.*;

public class Politician extends Robot {

//	final int WALL_LENGTH = 6;
//	MapLocation[] wallCheckLocs;
	int wallCheckIdx;
	boolean inGrid = false;

	public Politician (RobotController rc) throws GameActionException {
		super(rc);
//		wallCheckIdx = 0;
//		wallCheckLocs = new MapLocation[4];
	}

	public void run() throws GameActionException {
		super.run();
		Comms.checkFlag(creatorID);
		RobotInfo[] nearby = rc.senseNearbyRobots();
		runEco(nearby);
//		runDefense();
	}

	public void runEco(RobotInfo[] nearby) throws GameActionException {

		int minDist = 0; // Default distance

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
			// Stay a gridDistance of atleast two away from the nearest slanderer
//			System.out.println("Nearby slanderer at: " + info.getLocation().toString());
			int gridDist = Util.getGridSquareDist(info.getLocation(), creatorLoc);
			minDist = Math.max(minDist, gridDist + 2);
		}
		if(minDist == 0){
			minDist = 4; // Default politician distance
		}
		System.out.println("My min dist: " + minDist);

		if(Util.isGridSquare(myLoc, creatorLoc) && Util.getGridSquareDist(myLoc, creatorLoc) >= minDist){
			inGrid = true;
		}
		else{
			inGrid = false;
		}
		System.out.println("Am I on the grid? " + inGrid);
		if(!inGrid){
			System.out.println("Going to grid at minDist: " + minDist);
			nav.goToGrid(minDist);
		}
		else{
			System.out.println("Alr on grid at minDist: " + minDist + ", maintaining it");
			nav.maintainGrid(minDist);
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
