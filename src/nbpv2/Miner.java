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
                    if(rc.canSenseLocation(mineLocation) && rc.senseGold(mineLocation) + rc.senseLead(mineLocation) != 0){
                        av = 1;
                        scouting = false;
                        canExplore = false;
                    }
                    if (t == 0) {
                        while (rc.canMineGold(mineLocation)) {
                            rc.mineGold(mineLocation);
                        }
                    }
                    else {
                        while (rc.canMineLead(mineLocation)) {
                            rc.mineLead(mineLocation);
                        }
                    }
                }
            }
        }
        if(av == 0) {
            for(int t = 1; t <= 2; t++) {
                MapLocation bst = new MapLocation(1000, 1000);
                int mn = 100000000;
                for (int i = 0; i < 60; i++) {
                    int num = rc.readSharedArray(i);
                    if ((num & 0b111) == t) {
                        int x = ((num >> 9) & 0b111111), y = (num >> 3) & 0b111111;
                        int d = rc.getLocation().distanceSquaredTo(new MapLocation(x, y));
                        if (d < mn) {
                            mn = d;
                            bst = new MapLocation(x, y);
                        }
                    }
                }
                if (mn <= 200 - 50 * (2 - t)) {
                    Navigation.go(bst);
                    scouting = false;
                    canExplore = false;
                    break;
                }
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
        tryReportR();
        tryMine();
        tryScout();
    }

}