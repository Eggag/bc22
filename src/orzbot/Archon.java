package orzbot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Archon extends RobotPlayer {

    static int id = -1;

    static void decideBuild() throws GameActionException{
        //don't forget to update income
    }

    static void runArchon() throws GameActionException {
        if(turnCount == 1){
            int cr = rc.readSharedArray(NUM_ARCHONS_IND);
            id = cr;
            rc.writeSharedArray(NUM_ARCHONS_IND, cr + 1);
            for(int i = 0; i < MAX_MSG; i++){
                int num = rc.readSharedArray(i);
                if (num == 0){
                    num = OUR_ARCHON_CODE;
                    num |= (rc.getLocation().x << 9);
                    num |= (rc.getLocation().y << 3);
                    rc.writeSharedArray(i, num);
                    break;
                }
            }
        }
        int lst = rc.readSharedArray(INCOME_IND);
        int inc = rc.getTeamLeadAmount(rc.getTeam()) - lst;
        rc.setIndicatorString("INCOME IS " + inc);
        if(rc.getRoundNum() == 1) inc = 0;
        if(rc.getRoundNum() % rc.readSharedArray(NUM_ARCHONS_IND) == id){
            decideBuild();
        }
    }
}