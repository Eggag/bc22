package orzbot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Archon extends RobotPlayer {

    static int prevInc = 0;
    static int recentLim = 10;
    static int longerLim = 50;
    static int sumRecent = 0;
    static int sumLonger = 0;
    static int sumMiners = 0;
    static int[] recentDiff = new int[recentLim + 5];
    static int[] recentMiners = new int[recentLim + 5];
    static int[] longerDiff = new int[longerLim + 5];
    static int numArchons = 0;
    static int currentIndex = 0;

    static void build(RobotType rt) throws GameActionException{
        for (int i = 0; i < 8; i++) {
            Direction dir = directions[i];
            if (rc.canBuildRobot(rt, dir)){
                rc.buildRobot(rt, dir);
            }
        }
//        rc.setIndicatorString(rc.getTeamLeadAmount(rc.getTeam()) + "");
        rc.writeSharedArray(INCOME_IND, rc.getTeamLeadAmount(rc.getTeam()));
    }

    static double threshold() throws GameActionException{
        int leadBalance = rc.getTeamLeadAmount(rc.getTeam());
        int opponentLeadBalance = rc.getTeamLeadAmount(rc.getTeam().opponent());
        double cnt = 1.0 * sumMiners / recentLim;
        double danger = (double)(rc.readSharedArray(AGGRO_IND)) / cnt;
        double multiplier = (0.90 + (danger / 5.0)) * Math.max(100,rc.getRoundNum()) / 100;

        if(cnt < 10) {
            return 3 * multiplier;
        }else if(cnt < 30) {
            return 4 * multiplier;
        }else if(leadBalance < 40){
            return 10 * multiplier;
        }else if(leadBalance < 100) {
            return 30 * multiplier;
        }else {
            return 50 * multiplier;
        }
    }

    static int dynamicSwarmSize() throws GameActionException{
        int leadBalance = rc.getTeamLeadAmount(rc.getTeam());
        if(leadBalance > 400) {
            return 20;
        }else if(leadBalance > 200) {
            return 10;
        }
        int numSoldiers = rc.readSharedArray(NUM_SOLDIERS_IND);
        return 5 + (numSoldiers / 5);
    }


    static void decideBuild() throws GameActionException {
        double cnt = 1.0 * sumMiners / Math.max(1,recentLim);
        double recent = (double)(sumRecent) / Math.max(0.01,cnt);
        double longer = (double)(sumLonger) / Math.max(0.01,cnt);
        rc.setIndicatorString((int)recent + " " + (int)longer + " " + cnt + " " + rc.readSharedArray(AGGRO_IND));
        if(cnt < 4 || recent >= threshold()) build(RobotType.MINER);
//        if(rc.getRoundNum() % 10 < 1) build(RobotType.MINER);
        else build(RobotType.SOLDIER);
    }

    static void updateIncome() throws GameActionException{
        int lst = rc.readSharedArray(INCOME_IND);
        int inc = rc.getTeamLeadAmount(rc.getTeam()) - lst;
        rc.setIndicatorString(lst + " " + rc.getTeamLeadAmount(rc.getTeam()));
        if(rc.getRoundNum() == 1) inc = 0;
        int incDiff = inc - prevInc;
        int minerCnt = rc.readSharedArray(NUM_MINERS_IND);
        sumRecent -= recentDiff[rc.getRoundNum() % recentLim] - inc;
        sumLonger -= longerDiff[rc.getRoundNum() % longerLim] - inc;
        sumMiners -= recentMiners[rc.getRoundNum() % recentLim] - minerCnt;
        recentDiff[rc.getRoundNum() % recentLim] = inc;
        longerDiff[rc.getRoundNum() % longerLim] = inc;
        recentMiners[rc.getRoundNum() % recentLim] = minerCnt;
        prevInc = inc;
    }

    static void reportLocation() throws GameActionException{
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

    static void updateMinersAndSoldiers() throws GameActionException {
        rc.writeSharedArray(NUM_MINERS_IND,0);
        rc.writeSharedArray(NUM_SOLDIERS_IND,0);
    }

    static void updateSwarm() throws GameActionException {
        int id = rc.getID();
        int message = rc.readSharedArray(id * 2 + 1);
        int swarmSize = (message >> 10) & (0b11111111);
//        rc.setIndicatorString(message + "");
        if((message & (1 << 4)) != 0) {
            rc.writeSharedArray(id * 2 + 1,2);
            message = 2;
        }else{
            // Threshold for detaching the swarm
            int threshold = dynamicSwarmSize();
            if(swarmSize > threshold) {
                // Detach them
                rc.writeSharedArray(id * 2 + 1,message ^ (1 << 4));
                // Write target
                MapLocation uwu = rc.getLocation();
                int height = rc.getMapHeight();
                int width = rc.getMapWidth();
                int targetX = width - 1 - uwu.x;
                int targetY = height - 1 - uwu.y;
                rc.writeSharedArray(id * 2,2 + (targetX << 4) + (targetY << 10));
                return;
            }
        }
        rc.writeSharedArray(id * 2 + 1,(message | 2) & ((1 << 10) - 1));
    }

    static void updateArchonCount() throws GameActionException{
        if(rc.getRoundNum() % 2 == 0){
            currentIndex = rc.readSharedArray(NUM_ARCHONS_IND);
            numArchons = rc.readSharedArray(NUM_ARCHONS_IND_2);
            if(rc.getArchonCount() == currentIndex + 1) rc.writeSharedArray(NUM_ARCHONS_IND_2, 0);
            rc.writeSharedArray(NUM_ARCHONS_IND, currentIndex + 1);
        }
        else{
            currentIndex = rc.readSharedArray(NUM_ARCHONS_IND_2);
            numArchons = rc.readSharedArray(NUM_ARCHONS_IND);
            if(rc.getArchonCount() == currentIndex + 1) rc.writeSharedArray(NUM_ARCHONS_IND, 0);
            rc.writeSharedArray(NUM_ARCHONS_IND_2, currentIndex + 1);
        }
    }

    static void updateDangers() throws GameActionException {
        rc.setIndicatorString(rc.readSharedArray(AGGRO_IND) + "");
        rc.writeSharedArray(AGGRO_IND,0);
    }

    static void runArchon() throws GameActionException {
        if(turnCount == 1) reportLocation();
        updateArchonCount();
        updateIncome();
        updateSwarm();
        if(rc.getRoundNum() % numArchons == currentIndex){
            decideBuild();
        }
        if(currentIndex == rc.getArchonCount() - 1) {
            // Clearing round-based data
            updateMinersAndSoldiers();
            updateDangers();
        }
    }
}