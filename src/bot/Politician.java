package bot;
import battlecode.common.*;

public class Politician extends Robot {
	public Politician (RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		Team enemy = rc.getTeam().opponent();
		int actionRadius = rc.getType().actionRadiusSquared;
		RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
		if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
			System.out.println("empowering...");
			rc.empower(actionRadius);
			System.out.println("empowered");
			return;
		}
		if (nav.tryMove(nav.randomDirection()))
			System.out.println("I moved!");
	}
}
