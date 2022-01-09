package nbpv2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Miner extends RobotPlayer {

    static boolean scouting = false, canExplore = true;
    static MapLocation scoutGoal = null;

    static void tryMine() throws GameActionException {
        MapLocation me = rc.getLocation();
        int av = 0;
        for(int t = 0; t < 2; t++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                    if(rc.canSenseLocation(mineLocation) && (rc.senseGold(mineLocation) > 0 || rc.senseLead(mineLocation) > 1)){
                        scouting = false;
                        canExplore = false;
                    }
                    if(t == 0) {
                        while(rc.canMineGold(mineLocation)){
                            rc.mineGold(mineLocation);
                        }
                    }
                    else{
                        while(rc.canMineLead(mineLocation)){
                            if(rc.senseLead(mineLocation) == 1) break;
                            rc.mineLead(mineLocation);
                        }
                    }
                }
            }
        }
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
                if(di - amt/10 < d){
                    d = di - amt/10;
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
        if(!canExplore) return;
        if(scouting && rc.getLocation().distanceSquaredTo(scoutGoal) != 0) Navigation.go(scoutGoal);
        else{
            scoutGoal = new MapLocation((Math.abs(rng.nextInt())) % rc.getMapWidth(), (Math.abs(rng.nextInt())) % rc.getMapHeight());
            scouting  = true;
            Navigation.go(scoutGoal);
        }
    }

    static void tryRun() throws GameActionException{
        RobotInfo[] r = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        int mn = 10000000;
        MapLocation danger = null;
        for(RobotInfo uwu : r){
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
        if(rc.getRoundNum() % 2 == 0){
            int nm = rc.readSharedArray(63);
            rc.writeSharedArray(63, nm + 1);
        }
        else{
            int nm = rc.readSharedArray(62);
            rc.writeSharedArray(62, nm + 1);
        }
        canExplore = true;
        tryRun();
        tryMine();
        tryScout();
    }

}