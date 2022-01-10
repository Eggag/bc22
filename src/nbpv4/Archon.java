package nbpv4;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Archon extends RobotPlayer {

    static int id = 0;
    static int[] incomes = new int[5];
    static int[] incomesRange = new int[50];

    static void build(int ind) throws GameActionException {
        RobotType rt = RobotType.MINER;
        if(ind == 1) rt = RobotType.SOLDIER;
        for (int i = 0; i < 8; i++) {
            Direction dir = directions[i];
            if (rc.canBuildRobot(rt, dir)){
                rc.buildRobot(rt, dir);
            }
        }
        rc.writeSharedArray(58, rc.getTeamLeadAmount(rc.getTeam()));
    }

    static void decideBuild(int a, int b) throws GameActionException{
        RobotInfo[] en = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        for(RobotInfo uwu : en){
            if(rc.getType() == RobotType.SOLDIER || rc.getType() == RobotType.SAGE || rc.getType() == RobotType.WATCHTOWER){
                build(1);
                return;
            }
        }
        double avg = (double)(incomes[0] + incomes[1] + incomes[2] + incomes[3] + incomes[4]) / Math.min(5, rc.getRoundNum());
        int sumIncome = 0;
        for(int i = 0; i < 50; i++) sumIncome += incomesRange[i];
        double totAvg = (double)sumIncome / Math.min(50, rc.getRoundNum());
        if(rc.getRoundNum() <= 75) {
            if (avg > (totAvg * 1.7)) build(1);
            else build(0);
        }
        else if(rc.getRoundNum() <= 150){
            if (avg > (totAvg * 1.3) || a / 2 > b) build(1);
            else build(0);
        }
        else if(rc.getRoundNum() <= 400){
            if (avg > (totAvg * 1.2) || a > b) build(1);
            else build(0);
        }
        else{
            if (avg > totAvg || a * 3 > b * 5) build(1);
            else build(0);
        }
    }


    static void runArchon() throws GameActionException {
        if(turnCount == 1){
            int cr = rc.readSharedArray(59);
            id = cr;
            rc.writeSharedArray(59, cr + 1);
            for(int i = 0; i < maxMsg; i++){
                int num = rc.readSharedArray(i);
                if (num == 0) {
                    num = 3;
                    num |= (rc.getLocation().x << 9);
                    num |= (rc.getLocation().y << 3);
                    rc.writeSharedArray(i, num);
                    break;
                }
            }
        }
        int lst = rc.readSharedArray(58);
        int inc = rc.getTeamLeadAmount(rc.getTeam()) - lst;
        rc.setIndicatorString("INCOME IS " + inc);
        if(rc.getRoundNum() == 1) inc = 0;
        incomes[rc.getRoundNum() % 5] = inc;
        incomesRange[rc.getRoundNum() % 50] = inc;
        tryReportR();
        int numMiner = rc.readSharedArray(63);
        if(rc.getRoundNum() % 2 == 0){
            numMiner = rc.readSharedArray(62);
            rc.writeSharedArray(63, 0);
        }
        else{
           rc.writeSharedArray(62, 0);
        }
        int numSoldier = rc.readSharedArray(61);
        if(rc.getRoundNum() % 2 == 0){
            numSoldier = rc.readSharedArray(60);
            rc.writeSharedArray(61, 0);
        }
        else{
            rc.writeSharedArray(60, 0);
        }
        if(rc.getRoundNum() % rc.readSharedArray(59) == id) decideBuild(numMiner, numSoldier);
    }
}