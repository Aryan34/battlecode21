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

class FlagObj {
	int flag;
	int priority;
	boolean added;

	public FlagObj(){
		flag = 0;
		priority = Integer.MAX_VALUE;
		added = false;
	}
}

public class Robot {
	// General variables
	RobotController rc;
	Navigation nav;
	MapLocation myLoc = null;
	int turnCount = 0;
	Team myTeam;
	RobotType myType;
	int myFlag;
	DetectedInfo[] robotLocations;

	// Moving robots variables
	int creatorID;
	MapLocation creatorLoc;
	int robotLocationsIdx;
	CornerInfo targetCorner;
	MapLocation[] visited = new MapLocation[50];
	int visitedIdx = 0;

	// EC variables
	boolean doneScouting = false;
	int[] mapBoundaries = new int[4]; // Format for this is [minX, maxX, minY, maxY], which is also [West, East, South, North]
	int mapWidth = 0;
	int mapHeight = 0;
	boolean wonPrevVote;
	int teamVotes;
	boolean enemySpotted = false;


	public Robot (RobotController rc) throws GameActionException {
		// Initialize classes
		this.rc = rc;
		Util.rc = rc;
		Util.robot = this;
		Comms.rc = rc;
		Comms.robot = this;
		nav = new Navigation(rc, this);
		myTeam = rc.getTeam();
		myType = rc.getType();
		myLoc = rc.getLocation();
		// Find the location of the EC that spawned you
		creatorLoc = Util.findAdjacentEC();
		creatorID = -1;
		if(creatorLoc != null){
			creatorID = rc.senseRobotAtLocation(creatorLoc).getID();
		}
		myFlag = 0;
		robotLocations = new DetectedInfo[50];
		robotLocationsIdx = 0;
		targetCorner = null;

	}

	public void run() throws GameActionException {
		System.out.println("---------------------------------");
		turnCount += 1;
		if(myLoc != null && rc.getLocation().equals(myLoc)){
			visited[visitedIdx] = rc.getLocation();
			visitedIdx = (visitedIdx + 1) % visited.length;
		}
		myLoc = rc.getLocation();
		if(rc.getTeamVotes() == teamVotes) {
			wonPrevVote = false;
		}
		else {
			wonPrevVote = true;
			teamVotes = rc.getTeamVotes();
		}
	}

	public boolean haveVisited(MapLocation loc) throws GameActionException {
		for(int i = 0; i < visited.length; i++){
			if(visited[i] == null) { continue; }
			if(visited[i].equals(loc)){
				return true;
			}
		}
		return false;
	}

	public int distanceToEdge(int i, MapLocation loc){
		if(mapBoundaries[i] == 0){
			return Integer.MAX_VALUE;
		}
		int val = Math.abs(loc.x - mapBoundaries[i]);
		if(i == 2 || i == 3){ val = Math.abs(loc.y - mapBoundaries[i]); }
		return val;
	}

}
