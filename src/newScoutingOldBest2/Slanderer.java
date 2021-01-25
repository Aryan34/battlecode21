//package newScoutingOldBest2;
//
//import battlecode.common.*;
//
//public class Slanderer extends Robot {
//
//	boolean inGrid = false;
//	Politician pol = null;
//	boolean muckNearby;
//
//	public Slanderer (RobotController rc) throws GameActionException {
//		super(rc);
//	}
//
//	public void run() throws GameActionException {
//		super.run();
//		// Changed to politician
//		if(rc.getType() == RobotType.POLITICIAN){
//			myType = RobotType.POLITICIAN;
//			Log.log("Running slanderer code as politician");
//			if(pol == null){
//				pol = new Politician(rc);
//				pol.creatorLoc = creatorLoc;
//				pol.creatorID = creatorID;
//				pol.isAttacking = true;
//				// Reset flag!
//				pol.myFlag = myFlag;
//				pol.attackTarget = attackTarget;
//				// TODO: Copy over the rest of the variables?
//
//				Log.log("Resetting flag to 0!");
//				Comms.robot = pol;
//				Util.robot = pol;
//
//				Comms.setFlag(0);
//			}
//			pol.run();
//		}
//		else{
//			Comms.checkFlag(creatorID);
//			runEco();
//			broadcastIdentity();
//		}
//	}
//
//	public void runEco() throws GameActionException {
//		inGrid = Util.isGridSquare(myLoc, creatorLoc);
//		Log.log("Grid dist: " + Util.getGridSquareDist(myLoc, creatorLoc) + ", On lattice: " + inGrid);
//		checkSafety();
//		if(!muckNearby){
//			if(!inGrid){
//				nav.goToGrid(2);
//			}
//			else{
//				nav.maintainGrid(2);
//			}
//		}
//	}
//
//	// Runs away from nearby mucks
//	public void checkSafety() throws GameActionException {
//		muckNearby = false;
//		MapLocation closestLoc = null;
//		int closestDist = Integer.MAX_VALUE;
//		for(RobotInfo info : nearby){
//			// If you sense an enemy muckracker
//			if(info.getType() != RobotType.MUCKRAKER || info.getTeam() != myTeam.opponent()){
//				continue;
//			}
//			muckNearby = true;
//			// Find the closest one
//			int dist = myLoc.distanceSquaredTo(info.getLocation());
//			Log.log("Found a muck at: " + info.getLocation() + ", which is a distance of: " + dist);
//			if(dist < closestDist){
//				closestDist = dist;
//				closestLoc = info.getLocation();
//			}
//		}
//		// And move away from the closest one
//		if(closestLoc == null){
//			return;
//		}
//		for(Direction dir : Navigation.closeDirections(myLoc.directionTo(closestLoc).opposite())){
//			if(isSafer(myLoc, myLoc.add(dir))){
//				Log.log("It is safe to move: " + dir.toString());
//				nav.tryMove(dir);
//			}
//		}
//	}
//
//	// Returns true if loc2 is safer (farther away from a muck) than loc1, otherwise returns false.
//	public boolean isSafer(MapLocation loc1, MapLocation loc2){
//		int closest1 = Integer.MAX_VALUE;
//		int closest2 = Integer.MAX_VALUE;
//		for(RobotInfo info : nearby){
//			// If you sense an enemy muckracker
//			if(info.getType() != RobotType.MUCKRAKER || info.getTeam() != myTeam.opponent()){
//				continue;
//			}
//			// Find the closest one
//			int dist1 = loc1.distanceSquaredTo(info.getLocation());
//			closest1 = Math.min(closest1, dist1);
//			int dist2 = loc2.distanceSquaredTo(info.getLocation());
//			closest2 = Math.min(closest2, dist2);
//		}
//		return closest2 > closest1;
//	}
//
//
//}



package newScoutingOldBest2;

import battlecode.common.*;

import java.util.Arrays;

class SortingObj implements Comparable<SortingObj>{
	Direction dir;
	int val;
	public SortingObj(Direction dir, int val){
		this.dir = dir;
		this.val = val;
	}
	public int compareTo(SortingObj other){
		return this.val - other.val;
	}
}

public class Slanderer extends Robot {

	boolean inGrid = false;
	Politician pol = null;
	boolean muckNearby;

	public Slanderer (RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		// Changed to politician
		if(rc.getType() == RobotType.POLITICIAN){
			myType = RobotType.POLITICIAN;
			Log.log("Running slanderer code as politician");
			if(pol == null){
				pol = new Politician(rc);
				pol.creatorLoc = creatorLoc;
				pol.creatorID = creatorID;
				pol.isAttacking = true;
				// Reset flag!
				pol.myFlag = myFlag;
				pol.attackTarget = attackTarget;
				// TODO: Copy over the rest of the variables?

				Log.log("Resetting flag to 0!");
				Comms.robot = pol;
				Util.robot = pol;

				Comms.setFlag(0);
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
		Log.log("Grid dist: " + Util.getGridSquareDist(myLoc, creatorLoc) + ", On lattice: " + inGrid);
		checkSafety();
		if(!muckNearby){
			stayPacked(2);
//			if(!inGrid){
//				goToGrid(2);
//			}
//			else{
//				goCloserOnGrid(2);
//			}
		}
	}

	public void goCloserOnGrid(int minDist) throws GameActionException {
		int myGridDist = Util.getGridSquareDist(myLoc, creatorLoc);
		for(Direction dir : Navigation.nonCardinalDirections){
			MapLocation newLoc = myLoc.add(dir);
			int newGridDist = Util.getGridSquareDist(newLoc, creatorLoc);
			if(newGridDist < myGridDist && newGridDist >= minDist){
				nav.tryMove(dir);
			}
		}
	}

	public void stayPacked(int minDist) throws GameActionException {
		int bestDist = Integer.MAX_VALUE;
		MapLocation bestLoc = null;
		int myDist = Util.getGridSquareDist(myLoc, creatorLoc);
		for(int dx = -4; dx <= 4; dx++){
			for(int dy = -4; dy <= 4; dy++){
				MapLocation testLoc = myLoc.translate(dx, dy);
				int dist = Util.getGridSquareDist(testLoc, creatorLoc);
				if(!Util.isGridSquare(testLoc, creatorLoc) || dist < minDist){
					continue;
				}
				if(!rc.canSenseLocation(testLoc) || rc.isLocationOccupied(testLoc) || !rc.onTheMap(testLoc)){
					continue;
				}
				if(dist < bestDist){
					bestDist = dist;
					bestLoc = testLoc;
				}
			}
		}
		if(Util.isGridSquare(myLoc, creatorLoc)){
			if(bestLoc != null && bestDist < myDist){
				nav.goTo(bestLoc);
				Log.log("Going towards best loc at: " + bestLoc.toString() + " because its better.");
			}
		}
		else if(bestLoc != null){
			nav.goTo(bestLoc);
			Log.log("Going towards best loc at: " + bestLoc.toString() + " because I'm not on the grid rn.");
		}
		else{
			Log.log("Moving outwards");
			Direction targetDir = creatorLoc.directionTo(myLoc);
			MapLocation targetLoc = myLoc.add(targetDir).add(targetDir).add(targetDir).add(targetDir);
			nav.goTo(targetLoc);
			rc.setIndicatorLine(myLoc, targetLoc, 255, 0, 0);
		}

	}


	// Runs away from nearby mucks
	public void checkSafety() throws GameActionException {
		// TODO: Early detection system / add "if there's a poli defending that spot" as part of checkSafety
		muckNearby = false;
		MapLocation closestLoc = null;
		int closestDist = Integer.MAX_VALUE;
		for(RobotInfo info : nearby){
			// If you sense an enemy muckracker
			Log.log("Sensed: " + info.getType() + ", " + info.getLocation() + ", " + info.getTeam());
			if(info.getType() != RobotType.MUCKRAKER || info.getTeam() != myTeam.opponent()){
				continue;
			}
			muckNearby = true;
			// Find the closest one
			int dist = myLoc.distanceSquaredTo(info.getLocation());
			Log.log("Found a muck at: " + info.getLocation() + ", which is a distance of: " + dist);
			if(dist < closestDist){
				closestDist = dist;
				closestLoc = info.getLocation();
			}
		}
		// And move away from the closest one
		if(closestLoc == null){
			Log.log("No mucks detected!");
			return;
		}
		// For each direction, if its safer to go there, then go there
		Log.log("Checking the safety of each loc!");
		int currSafety = getSafetyValue(myLoc);
		SortingObj[] directionVals = new SortingObj[Navigation.directions.length];
		for(int i = 0; i < Navigation.directions.length; i++){
			Direction dir = Navigation.directions[i];
			int safety = getSafetyValue(myLoc.add(dir));
			directionVals[i] = new SortingObj(dir, safety);
		}
		Arrays.sort(directionVals);
		for(int i = directionVals.length - 1; i >= 0; i--){
			Log.log(currSafety + " vs. " + directionVals[i].val);
			if(directionVals[i].val > currSafety){
				nav.tryMove(directionVals[i].dir);
			}
		}
	}

	// Returns true if loc2 is safer (farther away from a muck) than loc1, otherwise returns false.
	public int getSafetyValue(MapLocation loc){
		int closest = Integer.MAX_VALUE;
		for(int i = 0; i < nearby.length; i++){
			RobotInfo info = nearby[i];
			// If you sense an enemy muckracker
			if(info.getType() != RobotType.MUCKRAKER || info.getTeam() != myTeam.opponent()){
				continue;
			}
			// Find the closest one
			closest = Math.min(closest, loc.distanceSquaredTo(info.getLocation()));
		}
		return closest;
	}

	public void goToGrid(int minDist) throws GameActionException {
		if(rc.getCooldownTurns() > 1){
			return;
		}

		// Check if ur alr on the grid
		int myGridDist = Util.getGridSquareDist(myLoc, creatorLoc);
		if(Util.isGridSquare(myLoc, creatorLoc) && myGridDist > minDist){
			return;
		}

		// If you can move into a non-occupied grid location, go for it
		for(Direction dir : Navigation.cardinalDirections){
			MapLocation target = myLoc.add(dir);
			if(Util.isGridSquare(target, creatorLoc) && Util.getGridSquareDist(target, creatorLoc) >= minDist){
				Log.log("Found a nearby lattice location: " + target.toString());
				if(nav.tryMove(dir)){
					return;
				}
			}
		}

		int bestDist = Integer.MAX_VALUE;
		MapLocation bestLoc = null;
		for(int dx = -4; dx <= 4; dx++){
			for(int dy = -4; dy <= 4; dy++){
				MapLocation testLoc = myLoc.translate(dx, dy);
				if(!Util.isGridSquare(testLoc, creatorLoc) || Util.getGridSquareDist(testLoc, creatorLoc) < minDist){
					continue;
				}
				if(!rc.canSenseLocation(testLoc) || rc.isLocationOccupied(testLoc) || !rc.onTheMap(testLoc)){
					continue;
				}
				int dist = myLoc.distanceSquaredTo(testLoc);
				if(dist < bestDist){
					bestDist = dist;
					bestLoc = testLoc;
				}
			}
		}

		if(bestLoc != null){
			Log.log("Going towards lattice location: " + bestLoc.toString());
			nav.goTo(bestLoc);
			rc.setIndicatorLine(myLoc, bestLoc, 255, 0, 0);
		}
		else{
			Log.log("Moving outwards");
			Direction targetDir = creatorLoc.directionTo(myLoc);
			MapLocation targetLoc = myLoc.add(targetDir).add(targetDir).add(targetDir).add(targetDir);
			nav.goTo(targetLoc);
		}
	}


}
