package spam;

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
		if(!inGrid){
			nav.goToGrid(2);
		}
		else{
			nav.maintainGrid(2);
		}
	}


}
