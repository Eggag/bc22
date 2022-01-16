package orzbot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Archon extends RobotPlayer {

    static int id = 0;

    static void build(int ind) throws GameActionException {
        RobotType rt = RobotType.MINER;
        if(ind == 1) rt = RobotType.SOLDIER;
        for (int i = 0; i < 8; i++) {
            Direction dir = directions[i];
            if (rc.canBuildRobot(rt, dir)) {
                rc.buildRobot(rt, dir);
            }
        }
    }

    static void decideBuild(int a, int b) throws GameActionException{
        RobotInfo[] en = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        for(RobotInfo uwu : en){
            if(rc.getType() == RobotType.SOLDIER || rc.getType() == RobotType.SAGE || rc.getType() == RobotType.WATCHTOWER){
                build(1);
                return;
            }
        }
        if(a < 20){
            build(0);
        }
        else if(a < 30){
            if(b < a / 3) build(1);
            else build(0);
        }
        else if(a < 40){
            if(b < a) build(1);
            else build(0);
        }
        else if(a < 45){
            if(b / 2 < a) build(1);
            else build(0);
        }
        else{
            if(b / 4 < a) build(1);
            else build(0);
        }
    }


    static void runArchon() throws GameActionException {
        if(turnCount == 1){
            int cr = rc.readSharedArray(59);
            id = cr;
            rc.writeSharedArray(59, cr + 1);
            for(int i = 0; i < 60; i++){
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