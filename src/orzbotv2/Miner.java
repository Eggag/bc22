package orzbotv2;

import battlecode.common.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

public class Miner extends RobotPlayer {

    static boolean scouting = false;
    static MapLocation scoutGoal = null;
    static RobotInfo[] enemies;

    static void tryMine() throws GameActionException {
        MapLocation me = rc.getLocation();
        int av = 0;
        for(int t = 0; t < 2; t++) {
            MapLocation[] cur;
            if(t == 0) cur = rc.senseNearbyLocationsWithGold(1000);
            else cur = rc.senseNearbyLocationsWithLead(1000);
            for(MapLocation nw : cur){
                if(!rc.isActionReady()) break;
                if(rc.getLocation().isAdjacentTo(nw)){
                    if(t == 0) {
                        while(rc.canMineGold(nw)){
                            rc.mineGold(nw);
                        }
                    }
                    else{
                        while(rc.canMineLead(nw)){
                            if(rc.senseLead(nw) == 1) break;
                            rc.mineLead(nw);
                        }
                    }
                }
            }
        }
        if(!rc.isMovementReady()) return;
        for(int t = 0; t < 2; t++){
            MapLocation[] cr;
            if(t == 0) cr = rc.senseNearbyLocationsWithGold(1000);
            else cr = rc.senseNearbyLocationsWithLead(1000);
            int d = 1000000;
            MapLocation bst = null;
            for(MapLocation loc : cr){
                int di = rc.getLocation().distanceSquaredTo(loc);
                int amt = rc.senseGold(loc);
                if(t == 1) amt = rc.senseLead(loc);
                if(t == 1 && amt == 1) continue;
                if((di - amt / 10) < d){
                    d = di - amt / 10;
                    bst = loc;
                }
            }
            if(bst != null){
                Navigation.go(bst);
                break;
            }
        }
    }

    static void tryScout() throws GameActionException {
        if(!rc.isMovementReady()) return;
        if(scouting && rc.getLocation().distanceSquaredTo(scoutGoal) > 2) Navigation.go(scoutGoal);
        else{
            scoutGoal = new MapLocation((Math.abs(rng.nextInt())) % rc.getMapWidth(), (Math.abs(rng.nextInt())) % rc.getMapHeight());
            scouting  = true;
            Navigation.go(scoutGoal);
        }
    }

    static void addEnemy() throws GameActionException {
        int p = 4;
        for(int i = 0;i < enemies.length;++i) {
            if(enemies[i].team == rc.getTeam()) continue;
            MapLocation loc = enemies[i].getLocation();
            for(;p < 20;p++) {
                if(rc.readSharedArray(p) == 0 || ((rc.readSharedArray(p) & (0b1111)) == HOTSPOT && Math.random() < 0.4)) {
                    rc.writeSharedArray(p,HOTSPOT + (loc.x << 4) + (loc.y << 10));
                    break;
                }
            }
        }
    }

    static void addDanger() throws GameActionException {
        int message = rc.readSharedArray(AGGRO_IND);
        for(RobotInfo uwu : enemies) {
            if(uwu.type == RobotType.SOLDIER) {
                message++;
            }
            if(uwu.type == RobotType.MINER){
                if(Math.random() < 0.5) message++;
            }
        }
        rc.writeSharedArray(AGGRO_IND,message);
    }

    static void tryRun() throws GameActionException{
        if(!rc.isMovementReady()) return;
        enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        addEnemy();
        int mn = 10000000;
        MapLocation danger = null;
        for(RobotInfo uwu : enemies){
            if(uwu.type == RobotType.SOLDIER || uwu.type == RobotType.WATCHTOWER || uwu.type == RobotType.SAGE){
                int d = rc.getLocation().distanceSquaredTo(uwu.location);
                if(d < mn){
                    mn = d;
                    danger = uwu.location;
                }
            }
        }
        if(danger != null) Navigation.goOP(danger);
    }

    static void runMiner() throws GameActionException{
        updateAlive(NUM_MINERS_IND);
        tryRun();
        addDanger();
        tryMine();
        tryScout();
        addDanger();
    }

}