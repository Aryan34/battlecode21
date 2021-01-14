package spam;

import battlecode.common.*;

public class Slanderer extends Robot {

	public Slanderer (RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		System.out.println("Running check flag on EC id");
		Comms.checkFlag(creatorID);
		runEco();
	}

	public void moveRandom() throws GameActionException {
		System.out.println("Moving randomly");
		nav.tryMove(nav.randomDirection());
	}

	public void runEco() throws GameActionException {
		if(targetCorner == null){
			moveRandom();
		}
		else{
			System.out.println("Going towards corner");
			nav.goTo(targetCorner.loc);
		}
	}




}
