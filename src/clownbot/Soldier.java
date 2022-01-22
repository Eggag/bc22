package clownbot;

import battlecode.common.*;

import java.util.Random;

public class Soldier extends RobotPlayer {

    enum STATE {LEADER,FOLLOWER,SCOUT};
    enum MODE {RESIGN};

    static STATE state;
    static MODE mode;

    static int timer = 20;
    static int lastLeaderParity = 0;
    static int violationCounter = 0;
    static RobotInfo[] enemies;

    static final Random rng = new Random();

    static MapLocation target = null;

    // Update hotspot, soldier count, etc.
    static void updateInfo() throws GameActionException {
        enemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam().opponent());
        Hotspot.addEnemies(enemies);
    }

    static void randomCombat() throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }
    }

    static void scout() throws GameActionException {
        randomCombat();
        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
            System.out.println("I moved!");
        }
    }

    static boolean merge() throws GameActionException {
        int originalIndex = SwarmInfo.index;
        int thresholdDistSquared = 20;
        int originalSize = SwarmInfo.size;
        int best = 1000000000;
        int bestIndex = -1;
        MapLocation loc = rc.getLocation();
        for(int i = 0;i < 29;i += 2) {
            SwarmInfo.index = i;
            SwarmInfo.get();
            if(SwarmInfo.mode != 1 || i == originalIndex || SwarmInfo.size < originalSize) continue;
            int dist = SwarmInfo.leader.distanceSquaredTo(loc);
            if(dist < thresholdDistSquared && dist < best) {
                rc.setIndicatorString("DISTANCE " + dist);
                rc.setIndicatorString("COUP DE TA! " + SwarmInfo.leader.x + " " + SwarmInfo.leader.y);
                best = dist;
                bestIndex = i;
            }
        }
        SwarmInfo.index = originalIndex;
        SwarmInfo.get();
        if(bestIndex != -1) {
            SwarmInfo.resignWithNewLeader(bestIndex);
            return true;
        }
        return false;
    }

    static boolean findLeader() throws GameActionException {
        int thresholdDistSquared = 20;
        int thresholdSwarmSize = 10;
        int best = 1000000000;
        int bestIndex = -1;
        MapLocation loc = rc.getLocation();
        for(int i = 0;i < 29;i += 2) {
            SwarmInfo.index = i;
            SwarmInfo.get();
            if(SwarmInfo.mode != 1 || SwarmInfo.size >= thresholdSwarmSize) continue;
            int dist = SwarmInfo.leader.distanceSquaredTo(loc);
            rc.setIndicatorString("PASS! " + SwarmInfo.size + " " + dist);
            if(dist < thresholdDistSquared && dist < best) {
                best = dist;
                bestIndex = i;
            }
        }
        SwarmInfo.index = bestIndex;
        if(bestIndex == -1) return false;
        SwarmInfo.get();
        SwarmInfo.size++;
        SwarmInfo.write();
        return true;
    }

    static boolean becomeLeader() throws GameActionException {
        for(int i = 0;i < 29;i += 2) {
            SwarmInfo.index = i;
            SwarmInfo.get();
            if(SwarmInfo.mode == 0) {
                SwarmInfo.leader = rc.getLocation();
                SwarmInfo.mode = 1;
                SwarmInfo.size = 1;
                SwarmInfo.write();
                return true;
            }
        }
        return false;
    }


    static void leader() throws GameActionException {
        SwarmInfo.get();
        randomCombat();
        rc.setIndicatorDot(rc.getLocation(),255,0,0);
        if(SwarmInfo.mode == 0 || SwarmInfo.mode == 2) {
            if(SwarmInfo.mode == 0) {
                rc.setIndicatorString("CLOWNERY!");
            }
            SwarmInfo.clear();
            timer = 20;
            state = STATE.SCOUT;
            return;
        }else if(SwarmInfo.mode == 3) {
            int newLeader = SwarmInfo.getNewLeader();
            rc.setIndicatorString("RESIGNING FOR MERGING " + newLeader);
            SwarmInfo.clear();
            timer = 30;
            state = STATE.FOLLOWER;
            SwarmInfo.index = newLeader;
            violationCounter = 0;
            return;
        }
        rc.setIndicatorString("LEADER! " + SwarmInfo.size + " " + SwarmInfo.index);
        if(SwarmInfo.size > 1) {
            // Reset resignation timer
            timer = 10;
        }

        target = Hotspot.findClosestHotspot();
        Navigation.go(target);
        if(!merge()) {
            SwarmInfo.leader = rc.getLocation();
            SwarmInfo.parity ^= 1;
            SwarmInfo.size = 1;
            SwarmInfo.write();
        }

    }

    static void follower() throws GameActionException {
        SwarmInfo.get();
        randomCombat();
        if(lastLeaderParity == SwarmInfo.parity) {
            // Parity Violation!
            violationCounter++;
            if(violationCounter >= 3) {
                // Leader is DEAD
                SwarmInfo.clear();
            }
        }else{
            lastLeaderParity ^= 1;
            violationCounter = 0;
        }
        if(SwarmInfo.index == -1) {
            // Needs to find a swarm
            transformation();
        }else{
            if(SwarmInfo.mode == 3) {
                // Merging mode!
                SwarmInfo.index = SwarmInfo.getNewLeader();
                SwarmInfo.get();
                violationCounter = 0;
            }
            // Go towards leader
            SwarmInfo.size++;
            SwarmInfo.write();
            Navigation.go(SwarmInfo.leader);
            rc.setIndicatorString("FOLLOWER OF " + SwarmInfo.leader.x + " " + SwarmInfo.leader.y + " with " + violationCounter + " of " + SwarmInfo.size);
            // Reset timer
            timer = 30;
        }
    }

    static void transformation() throws GameActionException {
        rc.setIndicatorString("TRANSFORMING!");
        if(findLeader()) {
            timer = 30;
            state = STATE.FOLLOWER;
            violationCounter = 0;
        }else{
            if(becomeLeader()) {
                timer = 10;
                state = STATE.LEADER;
            }else{
                timer = 20;
                state = STATE.SCOUT;
            }
        }
    }

    static void runSoldier() throws GameActionException {
        timer--;
        updateInfo();
        if(state == STATE.LEADER) {
            rc.setIndicatorString("LEADER");
            leader();
            if(timer < 0) {
                SwarmInfo.mode = 2;
                SwarmInfo.write();
            }
        }else if(state == STATE.FOLLOWER) {
            rc.setIndicatorString("FOLLOWER");
            follower();
            if(timer < 0) {
                timer = 20;
                state = STATE.SCOUT;
            }
        }else{
            scout();
            rc.setIndicatorString("SCOUT");
            if(timer < 0) {
                transformation();
            }
        }
    }
}
