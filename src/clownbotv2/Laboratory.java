package clownbotv2;

import battlecode.common.GameActionException;

public class Laboratory extends RobotPlayer {
    static void runLaboratory() throws GameActionException {
        updateAlive(NUM_LAB_IND);
        int sageCnt = rc.readSharedArray(NUM_SAGE_IND_2);
        int soldiersCnt = rc.readSharedArray(NUM_SOLDIERS_IND_2);
        rc.setIndicatorString("T RATE: " + rc.getTransmutationRate() + " sages: " + sageCnt + " soldiers: " + soldiersCnt);
        if(soldiersCnt < 20) {
            if (sageCnt * 2 < soldiersCnt) {
                if (rc.canTransmute()) rc.transmute();
            }
        }
        else{
            if (sageCnt < soldiersCnt * 2) {
                if (rc.canTransmute()) rc.transmute();
            }
        }
    }
}