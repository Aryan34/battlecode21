package bot;
import battlecode.common.*;

public class Muckraker extends Robot {
	public Muckraker (RobotController rc) throws GameActionException {
		super(rc);
	}

	public void run() throws GameActionException {
		super.run();
		Team enemy = rc.getTeam().opponent();
		int actionRadius = rc.getType().actionRadiusSquared;
		for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
			if (robot.type.canBeExposed()) {
				// It's a slanderer... go get them!
				if (rc.canExpose(robot.location)) {
					System.out.println("e x p o s e d");
					rc.expose(robot.location);
					return;
				}
			}
		}
		if (nav.tryMove(nav.randomDirection())) {
			System.out.println("I moved!");
		}
	}
}
