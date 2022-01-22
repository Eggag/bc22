package clownbot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import java.awt.*;

public class SwarmInfo {
    int index = 0;
    int size = 0;
    MapLocation leader;
    MapLocation attack;
    int mode = 0;

    public SwarmInfo(int ind) {
        rc = RC;
        index = ind;
        update();
    }

    void update() throws GameActionException {
        int message = RobotPlayer.rc.readSharedArray(index);
    }

}
