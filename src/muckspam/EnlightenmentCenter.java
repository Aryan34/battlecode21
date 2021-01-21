package muckspam;

import battlecode.common.*;

import java.util.HashSet;

public class EnlightenmentCenter extends Robot {

	int[] spawnedAllies = new int[3000];
	HashSet<Integer> spawnedAlliesSet = new HashSet<Integer>();
	int numSpawned = 0;
	int lastBid;
	int slanderersSpawned = 0;
	final int EC_MIN_INFLUENCE = 20;
	final int DEF_POLI_MIN_COST = 20;
	final int ATK_POLI_MIN_COST = 50;
	final int SLAND_MIN_COST = 40;

	public EnlightenmentCenter(RobotController rc) throws GameActionException {
		super(rc);
		lastBid = 5;
	}

	public void run() throws GameActionException {
		super.run();
//		bid();
		saveSpawnedAlliesIDs();
		checkRobotFlags();
		if(attackTarget != null){
			for(int i = 0; i < robotLocationsIdx; i++){
				DetectedInfo detected = robotLocations[i];
				if(detected.loc.equals(attackTarget) && detected.team == myTeam){
					attackTarget = null;
				}
			}
		}
		System.out.println("Leftover bytecode: " + Clock.getBytecodesLeft());
		spawnMucks();
	}

	public void checkRobotFlags() throws GameActionException {
		for (int i = 0; i < numSpawned; i++) {
			int robotID = spawnedAllies[i];
			Comms.checkFlag(robotID);
		}
	}

	public void saveSpawnedAlliesIDs() throws GameActionException {
		for (Direction dir : Util.directions) {
			MapLocation loc = myLoc.add(dir);
			if (rc.canSenseLocation(loc)) {
				RobotInfo info = rc.senseRobotAtLocation(loc);
				if (info == null) {
					continue;
				}
				// NOTE: It only saves muck's ids rn
//				if(info.getType() != RobotType.MUCKRAKER){
//					continue;
//				}
				int id = info.getID();
				if (!spawnedAlliesSet.contains(id)) {
					spawnedAllies[numSpawned] = id;
					spawnedAlliesSet.add(id);
					numSpawned += 1;
				}
			}
		}
	}


	public void spawnMucks() throws GameActionException {
		// Find a list of directions that I could spawn the robot in (list of unsearched directions)
		System.out.println("spawnMucks -- Cooldown left: " + rc.getCooldownTurns());
		Direction[] spawnDirections = new Direction[8];
		int tempIdx = 0;
		if (mapBoundaries[0] == 0) {
			spawnDirections[tempIdx] = Direction.WEST;
			tempIdx++;
		}
		if (mapBoundaries[1] == 0) {
			spawnDirections[tempIdx] = Direction.EAST;
			tempIdx++;
		}
		if (mapBoundaries[2] == 0) {
			spawnDirections[tempIdx] = Direction.SOUTH;
			tempIdx++;
		}
		if (mapBoundaries[3] == 0) {
			spawnDirections[tempIdx] = Direction.NORTH;
			tempIdx++;
		}
		if (mapBoundaries[0] == 0 && mapBoundaries[2] == 0) {
			spawnDirections[tempIdx] = Direction.SOUTHWEST;
			tempIdx++;
		}
		if (mapBoundaries[0] == 0 && mapBoundaries[3] == 0) {
			spawnDirections[tempIdx] = Direction.NORTHWEST;
			tempIdx++;
		}
		if (mapBoundaries[1] == 0 && mapBoundaries[2] == 0) {
			spawnDirections[tempIdx] = Direction.SOUTHEAST;
			tempIdx++;
		}
		if (mapBoundaries[1] == 0 && mapBoundaries[3] == 0) {
			spawnDirections[tempIdx] = Direction.NORTHEAST;
			tempIdx++;
		}

		System.out.println(mapBoundaries[0] + " " + mapBoundaries[1] + " " + mapBoundaries[2] + " " + mapBoundaries[3]);
		for (int i = numSpawned; i < numSpawned + tempIdx; i++) {
			Direction spawnDir = spawnDirections[numSpawned % tempIdx];
			if (Util.tryBuild(RobotType.MUCKRAKER, spawnDir, 1)) {
				break;
			}
		}
	}
}
