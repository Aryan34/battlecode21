package muckspam;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

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
			// Reset flag!
			Comms.setFlag(0);
			if(pol == null){
				pol = new Politician(rc);
				pol.creatorLoc = creatorLoc;
				pol.creatorID = creatorID;
			}
			pol.run();
		}
		else{
			Comms.checkFlag(creatorID);
			runEco();
		}
		broadcastIdentity();
	}

	public void runEco() throws GameActionException {
		if(Util.isGridSquare(myLoc, creatorLoc)){
			inGrid = true;
		}
		else{
			inGrid = false;
		}
		System.out.println("Grid dist: " + Util.getGridSquareDist(myLoc, creatorLoc) + ", On lattice: " + inGrid);
		if(!inGrid){
			nav.goToGrid(2);
		}
		else{
			nav.maintainGrid(2);
		}
	}


}
