package nbpv4;

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
        if(!canExplore || !rc.isMovementReady()) return;
        if(scouting && rc.getLocation().distanceSquaredTo(scoutGoal) != 0) Navigation.go(scoutGoal);
        else{
            scoutGoal = new MapLocation((Math.abs(rng.nextInt())) % rc.getMapWidth(), (Math.abs(rng.nextInt())) % rc.getMapHeight());
            scouting  = true;
            Navigation.go(scoutGoal);
        }
    }

    static void tryRun() throws GameActionException{
        if(!rc.isActionReady()) return;
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

    static void tryDie() throws GameActionException {
        int nmMiners = rc.readSharedArray(63);
        if (rc.getRoundNum() % 2 == 0) nmMiners = rc.readSharedArray(62);
        if (nmMiners > 30 && rc.canSenseLocation(rc.getLocation()) && rc.senseLead(rc.getLocation()) == 0) {
            int d = 10000000;
            for (int i = 0; i < 59; i++) {
                int num = rc.readSharedArray(i);
                if ((num & 0b111) == 3) {
                    int x = ((num >> 9) & 0b111111), y = (num >> 3) & 0b111111;
                    int rn = rc.getLocation().distanceSquaredTo(new MapLocation(x, y));
                    if (rn < d) d = rn;
                }
            }
            if (d <= 2 && Math.abs(rng.nextInt()) % 2 == 0) rc.disintegrate(); //:clown:
        }
    }


    static void runMiner() throws GameActionException {
        if(rc.getRoundNum() % 2 == 0){
            int nm = rc.readSharedArray(63);
            rc.writeSharedArray(63, nm + 1);
        }
        else{
            int nm = rc.readSharedArray(62);
            rc.writeSharedArray(62, nm + 1);
        }
        canExplore = true;
        tryDie();
        tryRun();
        tryMine();
        tryScout();
    }

}