package clownbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;

import java.awt.*;

public class Archon extends RobotPlayer {

    static void runArchon() throws GameActionException {
        if(turnCount == 1) {
            rc.buildRobot(RobotType.MINER,directions[0]);
        }
    }
}