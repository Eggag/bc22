package clownbot;

import battlecode.common.*;

import java.util.Random;

public class Soldier extends RobotPlayer {

    enum STATE {LEADER,FOLLOWER,SCOUT};
    enum MODE {RESIGN};

    static STATE state;
    static MODE mode;

    static int timer = 0;

    static final Random rng = new Random(6147);

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
        SwarmInfo.get();
        if(bestIndex == -1) return false;
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
        return true;
    }

    static void leader() throws GameActionException {
        SwarmInfo.get();
        MapLocation target = new MapLocation(rc.getMapWidth() - 1 - rc.getLocation().x,rc.getMapHeight() - 1 - rc.getLocation().y);
        Navigation.go(target);
        SwarmInfo.leader = rc.getLocation();
        SwarmInfo.attack = target;
        SwarmInfo.size = 1;
        SwarmInfo.write();
    }

    static void follower() throws GameActionException {
        SwarmInfo.get();
        if(SwarmInfo.index == -1) {
            // Needs to find a swarm
            transformation();
        }else{
            // Go towards leader
            Navigation.go(SwarmInfo.leader);
        }
    }

    static void transformation() throws GameActionException {
        if(findLeader()) {
            state = STATE.FOLLOWER;
        }else{
            if(becomeLeader()) {
               state = STATE.LEADER;
            }else{
                state = STATE.SCOUT;
            }
        }
    }

    static void runSoldier() throws GameActionException {
        if(state == STATE.LEADER) {
            leader();
        }else if(state == STATE.FOLLOWER) {
            follower();
        }else{
            scout();
            timer--;
            if(timer < 0) {
                transformation();
            }
        }
    }
}
