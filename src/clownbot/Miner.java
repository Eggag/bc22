package clownbot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Miner extends RobotPlayer {
    static MapLocation target;
    static void runMiner() throws GameActionException{
        if(turnCount == 1) {
            target = new MapLocation(rc.getMapWidth() - 1 - rc.getLocation().x,rc.getMapHeight() - 1 - rc.getLocation().y);
        }else{
            Navigation.go(target);
        }
    }

}