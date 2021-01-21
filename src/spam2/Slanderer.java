package spam2;

import battlecode.common.*;

public class Slanderer extends Robot {

	boolean inGrid = false;
	Politician pol = null;

	public Slanderer (RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		// Changed to politician
		if(rc.getType() == RobotType.POLITICIAN){
			myType = RobotType.POLITICIAN;
			System.out.println("Running slanderer code as politician");
			if(pol == null){
				pol = new Politician(rc);
				pol.creatorLoc = creatorLoc;
				pol.creatorID = creatorID;
				pol.isAttacking = true;
				// Reset flag!
				pol.myFlag = myFlag;
				pol.attackTarget = attackTarget;
				// TODO: Copy over the rest of the variables?

				System.out.println("Resetting flag to 0!");
				System.out.println(Comms.setFlag(0));
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
		System.out.println("Grid dist: " + Util.getGridSquareDist(myLoc, creatorLoc) + ", On lattice: " + inGrid);
		checkSuicide();
		if(!inGrid){
			nav.goToGrid(2);
		}
		else{
			nav.maintainGrid(2);
		}
	}

	// Simple check to see if a friendly politician is tryna sewercide
	public void checkSuicide() throws GameActionException {
		MapLocation checkLocation = myLoc.add(myLoc.directionTo(creatorLoc));
		if(!rc.canSenseLocation(checkLocation)){ return; }
		RobotInfo info = rc.senseRobotAtLocation(checkLocation);
		if(info == null){ return; }
		// If the robot is tryna sewercide, move away
		System.out.println("Checking for suiciding poli");
		if(myLoc.distanceSquaredTo(creatorLoc) != 4){ return; }
		System.out.println("A");
		if(info.getType() != RobotType.POLITICIAN){ return; }
		System.out.println("B");
		if(info.getInfluence() % 2 != 1){ return; }
		System.out.println("C");
		if(info.getInfluence() < 700){ return; }
		System.out.println("D");
		if(rc.getEmpowerFactor(myTeam, 0) < 1.1){ return; }
		System.out.println("Found suiciding poli!");
		Direction targetDir = creatorLoc.directionTo(myLoc);
		nav.tryMove(Navigation.closeDirections(targetDir));
	}


}
