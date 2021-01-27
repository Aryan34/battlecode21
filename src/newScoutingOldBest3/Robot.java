package newScoutingOldBest3;

import battlecode.common.*;

import java.util.Arrays;

class DetectedInfo {
	Team team;
	RobotType type;
	MapLocation loc;
	int influence;

	public DetectedInfo(Team team, RobotType type, MapLocation loc, int influence){
		this.team = team;
		this.type = type;
		this.loc = loc;
		this.influence = influence;
	}
}


public class Robot {
	// General variables
	RobotController rc;
	Navigation nav;
	MapLocation myLoc = null;
	int turnCount = 0;
	int currRound;
	Team myTeam;
	RobotType myType;
	int myFlag = 0;
	DetectedInfo[] robotLocations = new DetectedInfo[1000];
	int robotLocationsIdx = 0;
	RobotInfo[] nearby;
	boolean setFlagThisRound = false;

	// Moving robots variables
	int creatorID;
	MapLocation creatorLoc;
	MapLocation attackTarget = null;
	MapLocation[] visited = new MapLocation[50];
	int visitedIdx = 0;
	RobotType typeInQuestion = null;

	// EC variables
	boolean doneScouting = false;
	int[] mapBoundaries = new int[4]; // Format for this is [minX, maxX, minY, maxY], which is also [West, East, South, North]
	int mapWidth = 0;
	int mapHeight = 0;
	boolean wonPrevVote = false;
	int teamVotes;
	boolean enemySpotted = false;
	boolean neutralSpotted = false;


	public Robot (RobotController rc) throws GameActionException {
		// Initialize classes
		this.rc = rc;
		Util.rc = rc;
		Util.robot = this;
		Comms.rc = rc;
		Comms.robot = this;
		Log.rc = rc;
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
		currRound = rc.getRoundNum();
	}

	public void run() throws GameActionException {
		Log.log("---------------------------------");
		Log.log("Cooldown: " + rc.getCooldownTurns());
//		if(currRound > 500){
//			rc.resign();
//		}
		turnCount += 1;
		currRound = rc.getRoundNum();
		setFlagThisRound = false;
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
		Comms.checkFlag(rc.getID());
		nearby = rc.senseNearbyRobots();
		for(RobotInfo info : nearby){
			if(info.getType() == RobotType.ENLIGHTENMENT_CENTER){
				RobotInfo[] broadcastInfo = {info};
				relayRobotLocations(broadcastInfo);
			}
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

	public void broadcastIdentity() throws GameActionException {
		int purpose = 5;
		int typeIdx = -1;
		if(rc.getType() == RobotType.SLANDERER){ typeIdx = 0; }
		else if(rc.getType() == RobotType.POLITICIAN){ typeIdx = 0; }
		else if(rc.getType() == RobotType.MUCKRAKER){ typeIdx = 0; }
		else if(rc.getType() == RobotType.ENLIGHTENMENT_CENTER){ typeIdx = 0; }
		assert(typeIdx != -1);

		int[] flagArray = {purpose, 4, typeIdx, 2};
		int flag = Comms.concatFlag(flagArray);
		Comms.setFlag(flag);
	}

	// If you can sense any ECs, relay that information to everyone else
	public void relayRobotLocations(RobotInfo[] nearby) throws GameActionException {
		for(RobotInfo info : nearby){
			if(setFlagThisRound){ // Don't waste the bytecode
				Log.log("Already set my flag this round ://");
				continue;
			}
			DetectedInfo[] savedLocations = Util.getCorrespondingRobots(null, null, info.getLocation());
			if(savedLocations.length != 0 && (savedLocations[0].team == info.getTeam() && savedLocations[0].type == info.getType())){
				continue;
			}
			Log.log("Found something new! Broadcasting it");
			int purpose = 2;
			int[] xy = Comms.mapLocationToXY(info.getLocation());
			int x = xy[0];
			int y = xy[1];
			int inf = Math.min((info.getConviction() / 100), 15);
			// 0: Enemy EC, 1: Friendly EC, 2: Neutral EC, 3: Enemy robot
			if(info.getTeam() == myTeam.opponent()){
				int robot_type = 0;
				int[] flagArray = {purpose, 4, robot_type, 2, x, 7, y, 7, inf, 4};
				int flag = Comms.concatFlag(flagArray);
				Log.log("Setting flag to enemy: " + Comms.printFlag(flag));
				Comms.setFlag(flag);
			}
			else if(info.getTeam() == myTeam && info.getType() == RobotType.ENLIGHTENMENT_CENTER){
				int[] flagArray = {purpose, 4, 1, 2, x, 7, y, 7, inf, 4};
				int flag = Comms.concatFlag(flagArray);
				Log.log("Setting flag to friendly EC: " + Comms.printFlag(flag));
				Comms.setFlag(flag);
			}
			else if(info.getTeam() == Team.NEUTRAL){
				int[] flagArray = {purpose, 4, 2, 2, x, 7, y, 7, inf, 4};
				int flag = Comms.concatFlag(flagArray);
				Log.log("Setting flag to neutral: " + Comms.printFlag(flag));
				Comms.setFlag(flag);
			}
		}
	}

	public RobotInfo[] senseFromLoc(MapLocation center, int radius){
		RobotInfo[] withinRange = new RobotInfo[nearby.length];
		int i = 0;
		for(RobotInfo info : nearby){
			if(center.distanceSquaredTo(info.getLocation()) <= radius){
				withinRange[i] = info;
				i++;
			}
		}
		return Arrays.copyOfRange(withinRange, 0, i);
	}

}
