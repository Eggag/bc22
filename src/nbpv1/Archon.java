package nbpv1;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Archon extends RobotPlayer {
    static void runArchon(RobotController rc) throws GameActionException {
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rng.nextBoolean()) {
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
            }
        } else {
            if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                rc.buildRobot(RobotType.SOLDIER, dir);
            }
        }
    }
}