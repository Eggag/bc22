package orzbot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Miner extends RobotPlayer {

    static void runMiner() throws GameActionException{
        int nm = rc.readSharedArray(NUM_MINERS_IND);
        rc.writeSharedArray(NUM_MINERS_IND, nm + 1);
    }

}