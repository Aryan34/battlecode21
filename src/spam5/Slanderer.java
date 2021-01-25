package spam5;

import battlecode.common.*;

public class Slanderer extends Robot {

	boolean inGrid = false;
	Politician pol = null;
	boolean muckNearby;

	public Slanderer (RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		// Changed to politician
		if(rc.getType() == RobotType.POLITICIAN){
			myType = RobotType.POLITICIAN;
			Log.log("Running slanderer code as politician");
			if(pol == null){
				pol = new Politician(rc);
				pol.creatorLoc = creatorLoc;
				pol.creatorID = creatorID;
				pol.isAttacking = true;
				// Reset flag!
				pol.myFlag = myFlag;
				pol.attackTarget = attackTarget;
				// TODO: Copy over the rest of the variables?

				Log.log("Resetting flag to 0!");
				Comms.robot = pol;
				Util.robot = pol;

				Comms.setFlag(0);
			}
			pol.run();
		}
		else{
			Comms.checkFlag(creatorID);
			runEco();
			broadcastIdentity();
		}
	}

	public void runEco() throws GameActionException {
		inGrid = Util.isGridSquare(myLoc, creatorLoc);
		Log.log("Grid dist: " + Util.getGridSquareDist(myLoc, creatorLoc) + ", On lattice: " + inGrid);
		checkSafety();
		if(!muckNearby){
			checkSuicide();
			if(!inGrid){
				nav.goToGrid(2);
			}
			else{
				nav.maintainGrid(2);
			}
		}
	}

	// Simple check to see if a friendly politician is tryna sewercide
	public void checkSuicide() throws GameActionException {
		MapLocation checkLocation = myLoc.add(myLoc.directionTo(creatorLoc));
		if(!rc.canSenseLocation(checkLocation)){ return; }
		RobotInfo info = rc.senseRobotAtLocation(checkLocation);
		if(info == null){ return; }

		// If the robot is tryna sewercide, move away
		Log.log("Checking for suiciding poli");
		if(myLoc.distanceSquaredTo(creatorLoc) != 4){ return; }
		if(info.getType() != RobotType.POLITICIAN){ return; }
		if(info.getInfluence() % 2 != 1){ return; }
		if(info.getInfluence() < 700){ return; }
		if(rc.getEmpowerFactor(myTeam, 0) < 1.1){ return; }

		Direction targetDir = creatorLoc.directionTo(myLoc);
		nav.tryMove(Navigation.closeDirections(targetDir));
	}

	// Runs away from nearby mucks
	public void checkSafety() throws GameActionException {
		muckNearby = false;
		MapLocation closestLoc = null;
		int closestDist = Integer.MAX_VALUE;
		for(RobotInfo info : nearby){
			// If you sense an enemy muckracker
			if(info.getType() != RobotType.MUCKRAKER || info.getTeam() != myTeam.opponent()){
				continue;
			}
			muckNearby = true;
			// Find the closest one
			int dist = myLoc.distanceSquaredTo(info.getLocation());
			Log.log("Found a muck at: " + info.getLocation() + ", which is a distance of: " + dist);
			if(dist < closestDist){
				closestDist = dist;
				closestLoc = info.getLocation();
			}
		}
		// And move away from the closest one
		if(closestLoc == null){
			return;
		}
		for(Direction dir : Navigation.closeDirections(myLoc.directionTo(closestLoc).opposite())){
			if(isSafer(myLoc, myLoc.add(dir))){
				Log.log("It is safe to move: " + dir.toString());
				nav.tryMove(dir);
			}
		}
	}

	// Returns true if loc2 is safer (farther away from a muck) than loc1, otherwise returns false.
	public boolean isSafer(MapLocation loc1, MapLocation loc2){
		int closest1 = Integer.MAX_VALUE;
		int closest2 = Integer.MAX_VALUE;
		for(RobotInfo info : nearby){
			// If you sense an enemy muckracker
			if(info.getType() != RobotType.MUCKRAKER || info.getTeam() != myTeam.opponent()){
				continue;
			}
			// Find the closest one
			int dist1 = loc1.distanceSquaredTo(info.getLocation());
			closest1 = Math.min(closest1, dist1);
			int dist2 = loc2.distanceSquaredTo(info.getLocation());
			closest2 = Math.min(closest2, dist2);
		}
		return closest2 > closest1;
	}


}
