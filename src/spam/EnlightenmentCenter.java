package spam;

import battlecode.common.*;

import java.util.HashSet;

public class EnlightenmentCenter extends Robot {

	int[] scoutSpawnedIn;
	int[] spawnedAllies;
	HashSet<Integer> spawnedAlliesSet;
	int numSpawned;
	int[] mapBoundaries; // Format for this is [minX, maxX, minY, maxY], which is also [West, East, South, North]
	int mapWidth;
	int mapHeight;
	boolean doneScouting;


	public EnlightenmentCenter(RobotController rc) throws GameActionException {
		super(rc);
		scoutSpawnedIn = new int[4];
		spawnedAllies = new int[3000];
		spawnedAlliesSet = new HashSet<Integer>();
		numSpawned = 0;
		mapBoundaries = new int[4];
		mapWidth = 0;
		mapHeight = 0;
		doneScouting = false;
	}

	public void run() throws GameActionException {
		super.run();
		saveSpawnedAlliesIDs();
		checkRobotFlags();
		spawnScouts();
	}

	public void saveSpawnedAlliesIDs() throws GameActionException {
		for(Direction dir : Util.directions){
			MapLocation loc = myLoc.add(dir);
			if(rc.canSenseLocation(loc)){
				RobotInfo info = rc.senseRobotAtLocation(loc);
				if(info == null){
					continue;
				}
				int id = info.getID();
				if(!spawnedAlliesSet.contains(id)){
					spawnedAllies[numSpawned] = id;
					spawnedAlliesSet.add(id);
					numSpawned += 1;
				}
			}
		}
	}

	public void checkRobotFlags() throws GameActionException {
		for(int i = 0; i < numSpawned; i++){
			int robotID = spawnedAllies[i];
			if(rc.canGetFlag(robotID)){
				int flag = rc.getFlag(robotID);
				int[] splits = Util.parseFlag(flag);
				if(splits.length == 0){
					continue;
				}
				switch(splits[0]){
					case 1: // Scouting
						if(doneScouting){
							continue;
						}
						int dirIdx = splits[1];
						if(mapBoundaries[dirIdx] == 0){
							mapBoundaries[dirIdx] = splits[2];
						}
						if(mapBoundaries[0] != 0 && mapBoundaries[1] != 0){
							mapWidth = mapBoundaries[1] - mapBoundaries[0];
							System.out.println("Map width: " + mapWidth);
						}
						if(mapBoundaries[2] != 0 && mapBoundaries[3] != 0){
							mapHeight = mapBoundaries[3] - mapBoundaries[2];
							System.out.println("Map height: " + mapHeight);
						}
						if(mapWidth != 0 && mapHeight != 0){
							doneScouting = true;
						}
						break;
					case 2:
						int idx = splits[1];
						// 0: Enemy EC, 1: Friendly EC, 2: Enemy slanderer
						RobotType[] robotTypes = {RobotType.ENLIGHTENMENT_CENTER, RobotType.ENLIGHTENMENT_CENTER, RobotType.SLANDERER};
						Team[] robotTeams = {myTeam.opponent(), myTeam, myTeam.opponent()};
						RobotType detectedType = robotTypes[idx];
						Team detectedTeam = robotTeams[idx];
						int x = splits[2];
						int y = splits[3];
						MapLocation detectedLoc = new MapLocation(x, y);
						robotLocations[robotLocationsIdx] = new DetectedInfo(detectedTeam, detectedType, detectedLoc);
						robotLocationsIdx++;
						System.out.println("Detected Robot of type: " + detectedType.toString() + " and of team: " + detectedTeam.toString() + " at: " + detectedLoc.toString());
						break;
				}
			}
		}
	}

	public void spawnRandom() throws GameActionException {
		RobotType toBuild = RobotType.MUCKRAKER;
		int influence = 50;
		for (Direction dir : Util.directions) {
			if (rc.canBuildRobot(toBuild, dir, influence)) {
				rc.buildRobot(toBuild, dir, influence);
			}
			else {
				break;
			}
		}
	}

	public void spawnScouts() throws GameActionException {
		Direction[] spawnDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
		int influence = 1;
		for (int i = 0; i < spawnDirections.length; i++) {
			if(scoutSpawnedIn[i] != 0){
				continue;
			}
			Direction spawnDir = spawnDirections[i];
			if(Util.tryBuild(RobotType.MUCKRAKER, spawnDir, influence)){
				scoutSpawnedIn[i] = turnCount;
			}
		}
	}
}
