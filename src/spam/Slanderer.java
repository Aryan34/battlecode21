package spam;

import battlecode.common.*;

public class Slanderer extends Robot {

	MapLocation cornerLoc;

	public Slanderer (RobotController rc) throws GameActionException {
		super(rc);
		cornerLoc = null;
	}

	public void run() throws GameActionException {
		super.run();
		checkECFlags();
		runEco();
	}

	public void moveRandom() throws GameActionException {
		System.out.println("Moving randomly");
		nav.tryMove(nav.randomDirection());
	}

	public void runEco() throws GameActionException {
		if(cornerLoc == null){
			moveRandom();
		}
		else{
			System.out.println("Going towards corner");
			nav.goTo(cornerLoc);
		}
	}

	public void checkECFlags() throws GameActionException {
		assert(creatorID >= 0);
		System.out.println("READING EC Flag data");
		if(rc.canGetFlag(creatorID)){
			int flag = rc.getFlag(creatorID);
			int[] splits = Util.parseFlag(flag);
			if(splits.length == 0){
				return; //continue;
			}
			int x, y;
			switch(splits[0]){
				case 2:
					int idx = splits[1];
					// 0: Enemy EC, 1: Friendly EC, 2: Enemy slanderer
					RobotType[] robotTypes = {RobotType.ENLIGHTENMENT_CENTER, RobotType.ENLIGHTENMENT_CENTER, RobotType.SLANDERER};
					Team[] robotTeams = {myTeam.opponent(), myTeam, myTeam.opponent()};
					RobotType detectedType = robotTypes[idx];
					Team detectedTeam = robotTeams[idx];
					x = splits[2];
					y = splits[3];
					MapLocation detectedLoc = Util.xyToMapLocation(x, y);
					boolean alreadySaved = false;
					for(int j = 0; j < robotLocationsIdx; j++){
						if(robotLocations[j].loc == detectedLoc){
							robotLocations[j].team = detectedTeam;
							robotLocations[j].type = detectedType;
							alreadySaved = true;
							break;
						}
					}
					if(!alreadySaved) {
						robotLocations[robotLocationsIdx] = new DetectedInfo(detectedTeam, detectedType, detectedLoc);
						robotLocationsIdx++;
						System.out.println("Detected new robot of type: " + detectedType.toString() + " and of team: " + detectedTeam.toString() + " at: " + detectedLoc.toString());
					}
					break;
				case 3: // Corner location to hide in
					System.out.println("GETTING CORNER LOC FROM EC");
					x = splits[1];
					y = splits[2];
					cornerLoc = Util.xyToMapLocation(x, y);
					break;
			}
		}
	}



}
