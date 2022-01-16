package orzbot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Soldier extends RobotPlayer {
    static void runSoldier() throws GameActionException {
        int nm = rc.readSharedArray(NUM_SOLDIERS_IND);
        rc.writeSharedArray(NUM_SOLDIERS_IND, nm + 1);
    }
}
