package clownbot;

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
        boolean far = true;
        for (RobotInfo en : enemies) {
            if (en.getType() == RobotType.ARCHON) far = false;
        }
        for (int t = 0; t < 2; t++) {
            MapLocation[] cur;
            if (t == 0) cur = rc.senseNearbyLocationsWithGold(1000);
            else cur = rc.senseNearbyLocationsWithLead(1000);
            for (MapLocation nw : cur) {
                if (!rc.isActionReady()) break;
                if (rc.getLocation().isAdjacentTo(nw)) {
                    if (t == 0) {
                        while (rc.canMineGold(nw)) {
                            rc.mineGold(nw);
                        }
                    } else {
                        while (rc.canMineLead(nw)) {
                            if (rc.senseLead(nw) == 1 && far) break;
                            rc.mineLead(nw);
                        }
                    }
                    scouting = false;
                }
            }
        }
        if (!rc.isMovementReady()) return;
        for (int t = 0; t < 2; t++) {
            MapLocation[] cr;
            if (t == 0) cr = rc.senseNearbyLocationsWithGold(1000);
            else cr = rc.senseNearbyLocationsWithLead(1000);
            int d = 1000000;
            MapLocation bst = null;
            for (MapLocation loc : cr) {
                int di = rc.getLocation().distanceSquaredTo(loc);
                int amt = rc.senseGold(loc);
                if (t == 1) amt = rc.senseLead(loc);
                if (t == 1 && amt == 1) continue;
                if ((di - amt / 10) < d) {
                    d = di - amt / 10;
                    bst = loc;
                }
            }
            if (bst != null) {
                Navigation.goPSO(bst);
                break;
            }
        }
    }

    static void tryScout() throws GameActionException {
        if (!rc.isMovementReady()) return;
        if (scouting && rc.getLocation().distanceSquaredTo(scoutGoal) > 20) Navigation.go(scoutGoal);
        else {
            double rand = Math.random();
            if (rand < 0.7) {
                scoutGoal = new MapLocation((Math.abs(rng.nextInt())) % rc.getMapWidth(), (Math.abs(rng.nextInt())) % rc.getMapHeight());
            } else {
                if (rc.getRoundNum() % 4 == 0) {
                    scoutGoal = new MapLocation(Math.min(rc.getMapWidth() - 1, rc.getLocation().x + 10), Math.min(rc.getMapHeight() - 1, rc.getLocation().y - 10));
                }
                if (rc.getRoundNum() % 4 == 1) {
                    scoutGoal = new MapLocation(Math.min(rc.getMapWidth() - 1, rc.getLocation().x + 10), Math.max(0, rc.getLocation().y - 10));
                }
                if (rc.getRoundNum() % 4 == 2) {
                    scoutGoal = new MapLocation(Math.max(0, rc.getLocation().x - 10), Math.min(rc.getMapHeight() - 1, rc.getLocation().y + 10));
                } else {
                    scoutGoal = new MapLocation(Math.max(0, rc.getLocation().x - 10), Math.max(0, rc.getLocation().y - 10));
                }
            }
            scouting = true;
            Navigation.goPSO(scoutGoal);
        }
    }

    static void addDanger() throws GameActionException {
        int message = rc.readSharedArray(AGGRO_IND);
        for (RobotInfo uwu : enemies) {
            if (uwu.type == RobotType.SOLDIER) {
                message++;
            }
            if (uwu.type == RobotType.MINER) {
                message++;
            }
        }
        rc.writeSharedArray(AGGRO_IND, message);
    }


    static void tryRun() throws GameActionException {
        if (!rc.isMovementReady()) return;
        enemies = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        Hotspot.addEnemies(enemies);
        int mn = 10000000;
        MapLocation danger = null;
        for (RobotInfo uwu : enemies) {
            if (uwu.type == RobotType.SOLDIER || uwu.type == RobotType.WATCHTOWER || uwu.type == RobotType.SAGE) {
                int d = rc.getLocation().distanceSquaredTo(uwu.location);
                if (d < mn) {
                    mn = d;
                    danger = uwu.location;
                }
            }
        }
        if (danger == null) return;
        MapLocation bst = null;
        int sc = 0;
        for (int i = -5; i <= 5; i++) {
            for (int j = -5; j <= 5; j++) {
                MapLocation pos = new MapLocation(rc.getLocation().x + i, rc.getLocation().y + j);
                if (danger.distanceSquaredTo(pos) > sc) {
                    sc = danger.distanceSquaredTo(pos);
                    bst = pos;
                }
            }
        }
        if (bst != null) Navigation.goPSO(bst);
    }

    static void runMiner() throws GameActionException {
        updateAlive(NUM_MINERS_IND);
        tryRun();
        addDanger();
        tryMine();
        tryScout();
        addDanger();
    }

}