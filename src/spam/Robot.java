package spam;

import battlecode.common.*;

class DetectedInfo {
	Team team;
	RobotType type;
	MapLocation loc;

	public DetectedInfo(Team team, RobotType type, MapLocation loc){
		this.team = team;
		this.type = type;
		this.loc = loc;
	}
}

public class Robot {
	RobotController rc;
	Navigation nav;
	int turnCount;
	MapLocation myLoc;
	MapLocation creatorLoc;
	Team myTeam;
	RobotType myType;
	int teamID;
	int teamVotes;
	boolean wonPrevVote;
	int myFlag;
	DetectedInfo[] robotLocations;
	int robotLocationsIdx;

	public Robot (RobotController rc) throws GameActionException {
		// Initialize classes
		this.rc = rc;
		Util.rc = rc;
		Util.robot = this;
		nav = new Navigation(rc, this);
		myTeam = rc.getTeam();
		myType = rc.getType();
		// Find the location of the EC that spawned you
		creatorLoc = Util.findAdjacentEC();
		if(myTeam == Team.A){
			teamID = 10;
		}
		else{
			teamID = 12;
		}
		myFlag = 0;
		robotLocations = new DetectedInfo[50];
		robotLocationsIdx = 0;
	}

	public void run() throws GameActionException {
		turnCount += 1;
		myLoc = rc.getLocation();
		if(rc.getTeamVotes() == teamVotes) {
			wonPrevVote = false;
		}
		else {
			wonPrevVote = true;
			teamVotes = rc.getTeamVotes();
		}
	}
}