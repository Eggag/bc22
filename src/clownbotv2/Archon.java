package clownbotv2;

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
        int sageCnt = rc.readSharedArray(NUM_SAGE_IND);
        int soldierCnt = rc.readSharedArray(NUM_SOLDIERS_IND);
        int labCnt = rc.readSharedArray(NUM_LAB_IND);
        if(rc.getTeamLeadAmount(rc.getTeam()) < 500) {
            if (rt == RobotType.SOLDIER && soldierCnt > 20 && labCnt > 0) {
                if (sageCnt * 3 < soldierCnt) {
                    return;
                }
            }
            if (rt == RobotType.SOLDIER && soldierCnt > 40 && labCnt > 0) {
                if (sageCnt * 2 < soldierCnt) {
                    return;
                }
            }
        }
        for (int i = 0; i < 8; i++) {
            Direction dir = directions[i];
            if (rc.canBuildRobot(rt, dir)){
                rc.buildRobot(rt, dir);
            }
        }
//        rc.setIndicatorString(rc.getTeamLeadAmount(rc.getTeam()) + "");
        rc.writeSharedArray(INCOME_IND, rc.getTeamLeadAmount(rc.getTeam()));
    }

    static void healing() throws GameActionException {
        RobotInfo[] bots = rc.senseNearbyRobots(rc.getType().actionRadiusSquared,rc.getTeam());
        if(bots.length == 0) return;
        RobotInfo bst = bots[0];
        for(RobotInfo bot : bots) {
            if((rc.canRepair(bot.getLocation()) && bot.getHealth() < bst.getHealth() && bot.getType() == RobotType.SAGE) || bst.getType() != RobotType.SAGE) {
                bst = bot;
            }
        }
        rc.repair(bst.getLocation());
    }

    static double threshold() throws GameActionException{
        int leadBalance = rc.getTeamLeadAmount(rc.getTeam());
        int opponentLeadBalance = rc.getTeamLeadAmount(rc.getTeam().opponent());
        double cnt = 1.0 * sumMiners / recentLim;
        double danger = (double)(rc.readSharedArray(AGGRO_IND)) / cnt;
        double coef = 1.0;
        double multiplier = (coef + (danger * 1.5));
        boolean rich = leadBalance > 400;
        if(rich) {
            return 100;
        }
        if(cnt < 8){
            return 4 * multiplier;
        }
        else if(cnt < 16) {
            return 5 * multiplier;
        }else if(cnt < 20) {
            return 6 * multiplier;
        }
        else if(cnt < 30) {
            return 8 * multiplier;
        }
        else if(cnt < 40){
            return 15 * multiplier;
        }
        else if(cnt < 50){
            return 20 * multiplier;
        }
        else {
            return 22 * multiplier;
        }
    }

    static void decideBuild() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        if(enemies.length > 0){
            build(RobotType.SOLDIER);
            return;
        }
        int labCnt = rc.readSharedArray(NUM_LAB_IND);
        int minerCnt = rc.readSharedArray(NUM_MINERS_IND);
        int soldiersCnt = rc.readSharedArray(NUM_SOLDIERS_IND);
        if(rc.getTeamGoldAmount(rc.getTeam()) >= 20){
            build(RobotType.SAGE);
            return;
        }
        if(minerCnt > 10){
            RobotInfo[] fr = rc.senseNearbyRobots(1000, rc.getTeam());
            int f = 0;
            for(RobotInfo owo : fr){
                if(owo.getType() == RobotType.BUILDER){
                    f = 1;
                    break;
                }
            }
            if(f == 0) build(RobotType.BUILDER);
        }
        if(labCnt < rc.getArchonCount() && rc.readSharedArray(NEED_LAB_IND) == 1){
            return;
        }
        double cnt = 1.0 * sumMiners / Math.max(1,recentLim);
        double recent = (double)(sumRecent) / Math.max(0.01,cnt);
        double longer = (double)(sumLonger) / Math.max(0.01,cnt);
        rc.setIndicatorString((int)recent + " " + (int)longer + " " + cnt + " " + rc.readSharedArray(AGGRO_IND));
        int sz = rc.getMapHeight() * rc.getMapWidth();
        int thr = 100;
        if(sz > 1000) thr = 70;
        if(sz > 2000) thr = 20;
        if(cnt < 3 || (recent >= threshold() && (rc.getRoundNum() > thr || soldiersCnt * 2 >= minerCnt))) build(RobotType.MINER);
        else build(RobotType.SOLDIER);
    }

    static void updateIncome() throws GameActionException{
        int soldierCnt = rc.readSharedArray(NUM_SOLDIERS_IND);
        int sageCnt = rc.readSharedArray(NUM_SAGE_IND);
        rc.writeSharedArray(NUM_SOLDIERS_IND_2, soldierCnt);
        rc.writeSharedArray(NUM_SAGE_IND_2, sageCnt);
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

    static void updateMinersAndSoldiers() throws GameActionException {
        rc.writeSharedArray(NUM_SOLDIERS_IND,rc.readSharedArray(NUM_SOLDIERS_IND));
        rc.writeSharedArray(NUM_MINERS_IND,0);
        rc.writeSharedArray(NUM_SOLDIERS_IND,0);
        rc.writeSharedArray(NUM_LAB_IND, 0);
        rc.writeSharedArray(NUM_SAGE_IND, 0);
        rc.writeSharedArray(NEED_LAB_IND, 0);
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
        updateArchonCount();
        updateIncome();
        if(rc.getRoundNum() % numArchons == currentIndex){
            decideBuild();
        }
        //if(rc.getRoundNum() < 100) Hotspot.addHotSpot(rc.getLocation());
        if(currentIndex == rc.getArchonCount() - 1) {
            // Clearing round-based data
            updateMinersAndSoldiers();
            updateDangers();
        }
        healing();
    }
}