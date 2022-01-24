package clownbotv0;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import java.util.Random;

public class Hotspot extends RobotPlayer {
    static int index;
    static MapLocation target;
    // 0 for normal hot spot and 1 for archon
    static int type = 0;

    static final Random rng = new Random();

    static void get() throws GameActionException {
        int message = rc.readSharedArray(index);
        target = new MapLocation(message & (0b111111),(message >> 6) & (0b111111));
        type = (message >> 12) & (0b1);
    }

    static MapLocation findClosestHotspot() throws GameActionException {
        if(rc.getRoundNum() > 500 && Math.random() < 0.3){
            MapLocation bstArchon = null;
            int d = 100000;
            for(int i = 45;i < 50;++i) {
                index = i;
                get();
                if(type == 0) continue;
                if(rc.getLocation().distanceSquaredTo(target) < d){
                    d = rc.getLocation().distanceSquaredTo(target);
                    bstArchon = target;
                }
            }
            if(bstArchon != null){
                return bstArchon;
            }
        }
        MapLocation bst = rc.getLocation();
        MapLocation uwu = rc.getLocation();
        int best = 1000000000;
        for(int i = 30;i < 45;++i) {
            index = i;
            get();
            if(type == 0) continue;
            int dist = uwu.distanceSquaredTo(target);
            if(dist < best) {
                best = dist;
                bst = target;
            }
        }
        return bst;
    }

    static void addHotSpot(MapLocation loc) throws GameActionException {
        int randomInd = rng.nextInt(15) + 30;
        rc.writeSharedArray(randomInd,loc.x | (loc.y << 6) | (1 << 12));
    }

    static void addHotSpot(MapLocation loc,int index) throws GameActionException {
        rc.writeSharedArray(index,loc.x | (loc.y << 6) | (1 << 12));
    }

    static void addArchon(MapLocation loc) throws GameActionException {
        for(int i = 45;i < 50;++i) {
            index = i;
            get();
            if(type == 0) {
                addHotSpot(loc,i);
                return;
            }
        }
        int randomInd = rng.nextInt(5) + 45;
        addHotSpot(loc,randomInd);
    }

    static void addEnemies(RobotInfo[] enemies) throws GameActionException {
        for(RobotInfo enemy : enemies) {
            if(enemy.getType() == RobotType.ARCHON) {
                Hotspot.addArchon(enemy.getLocation());
            }else{
                Hotspot.addHotSpot(enemy.getLocation());
            }
        }
    }
}
