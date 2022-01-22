package clownbot;

import battlecode.common.*;

import java.util.Random;

public class Soldier extends RobotPlayer {

    enum STATE {LEADER,FOLLOWER,SCOUT};
    enum MODE {RESIGN};

    static STATE state;
    static MODE mode;

    static int timer = 20;

    static final Random rng = new Random();

    static MapLocation target = null;

    static void scout() throws GameActionException {
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

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
            System.out.println("I moved!");
        }
    }

    static boolean findLeader() throws GameActionException {
        int thresholdDistSquared = 20;
        int best = 1000000000;
        int bestIndex = -1;
        MapLocation loc = rc.getLocation();
        for(int i = 0;i < 29;i += 2) {
            SwarmInfo.index = i;
            SwarmInfo.get();
            if(SwarmInfo.mode == 0) continue;
            int dist = SwarmInfo.leader.distanceSquaredTo(loc);
            if(dist < thresholdDistSquared && dist < best) {
                best = dist;
                bestIndex = i;
            }
        }
        SwarmInfo.index = bestIndex;
        if(bestIndex == -1) return false;
        SwarmInfo.get();
        return true;
    }

    static boolean becomeLeader() throws GameActionException {
        for(int i = 0;i < 29;i += 2) {
            SwarmInfo.index = i;
            SwarmInfo.get();
            if(SwarmInfo.mode == 0) {
                SwarmInfo.mode = 1;
                SwarmInfo.write();
                return true;
            }
        }
        return false;
    }

    static void resignate() throws GameActionException {
        SwarmInfo.clear();
        timer = 30;
        state = STATE.FOLLOWER;
    }

    static void leader() throws GameActionException {
        SwarmInfo.get();
        if(SwarmInfo.mode == 2) {
            resignate();
            return;
        }
        if(SwarmInfo.size > 1) {
            // Reset resignation timer
            timer = 10;
            return;
        }

        if(target == null) target = new MapLocation(rng.nextInt(rc.getMapWidth()),rng.nextInt(rc.getMapWidth()));
        Navigation.go(target);
        SwarmInfo.leader = rc.getLocation();
        SwarmInfo.attack = target;
        SwarmInfo.size = 1;
        SwarmInfo.write();
    }

    static void follower() throws GameActionException {
        SwarmInfo.get();
        SwarmInfo.size++;
        SwarmInfo.write();
        if(SwarmInfo.index == -1) {
            // Needs to find a swarm
            transformation();
        }else{
            // Go towards leader
            Navigation.go(SwarmInfo.leader);
            rc.setIndicatorString("FOLLOWER OF " + SwarmInfo.leader.x + " " + SwarmInfo.leader.y);
            // Reset timer
            timer = 30;
        }
    }

    static void transformation() throws GameActionException {
        rc.setIndicatorString("TRANSFORMING!");
        if(findLeader()) {
            timer = 30;
            state = STATE.FOLLOWER;
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
        if(state == STATE.LEADER) {
            leader();
            rc.setIndicatorString("LEADER");
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
