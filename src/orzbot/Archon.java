package orzbot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Archon extends RobotPlayer {

    static int archonCnt = 0;
    static int prevInc = 0;
    static int recentLim = 5;
    static int longerLim = 50;
    static int sumRecent = 0;
    static int sumLonger = 0;
    static int[] recentDiff = new int[recentLim + 5];
    static int[] longerDiff = new int[longerLim + 5];

    static void build(RobotType rt) throws GameActionException{
        for (int i = 0; i < 8; i++) {
            Direction dir = directions[i];
            if (rc.canBuildRobot(rt, dir)){
                rc.buildRobot(rt, dir);
            }
        }
        rc.writeSharedArray(INCOME_IND, rc.getTeamLeadAmount(rc.getTeam()));
    }

    static void decideBuild() throws GameActionException{
        double recent = (double)(sumRecent) / (double)(Math.min(recentLim, rc.getRoundNum()));
        double longer = (double)(sumLonger) / (double)(Math.min(longerLim, rc.getRoundNum()));
        if(recent > longer) build(RobotType.MINER);
        else build(RobotType.SOLDIER);
    }

    static void updateIncome() throws GameActionException{
        int lst = rc.readSharedArray(INCOME_IND);
        int inc = rc.getTeamLeadAmount(rc.getTeam()) - lst;
        if(rc.getRoundNum() == 1) inc = 0;
        int incDiff = inc - prevInc;
        sumRecent -= recentDiff[rc.getRoundNum() % 5] - incDiff;
        sumLonger -= longerDiff[rc.getRoundNum() % 50] - incDiff;
        recentDiff[rc.getRoundNum() % 5] = incDiff;
        longerDiff[rc.getRoundNum() % 50] = incDiff;
        prevInc = inc;
    }

    static void reportLocation() throws GameActionException{
        int cr = rc.readSharedArray(NUM_ARCHONS_IND);
        archonCnt = cr;
        rc.writeSharedArray(NUM_ARCHONS_IND, cr + 1);
        for(int i = 0; i < MAX_MSG; i++){
            int num = rc.readSharedArray(i);
            if (num == 0){
                num = OUR_ARCHON_CODE;
                num |= (rc.getLocation().x << 10);
                num |= (rc.getLocation().y << 4);
                rc.writeSharedArray(i, num);
                break;
            }
        }
    }

    static void runArchon() throws GameActionException {
        if(turnCount == 1) reportLocation();
        updateIncome();
        if(rc.getRoundNum() % rc.readSharedArray(NUM_ARCHONS_IND) == archonCnt){
            decideBuild();
        }
    }
}