package clownbotv2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;

public class Sage extends RobotPlayer {
    static void runSage() throws GameActionException {
        updateAlive(NUM_SAGE_IND);
        Direction dir = directions[rng.nextInt(8)];
        if(rc.canMove(dir)) rc.move(dir);
    }
}