package clownbot;

import battlecode.common.*;

import java.util.Random;

public class Soldier extends RobotPlayer {

    enum STATE {LEADER,FOLLOWER,SCOUT};
    enum MODE {RESIGN};

    static STATE state;
    static MODE mode;

    static int timer = 0;
    static int leaderIndex = -1;

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


    static void runSoldier() throws GameActionException {
        if(state == STATE.LEADER) {
            leader();
        }else if(state == STATE.FOLLOWER) {
            follower();
        }else{
            scout();
            timer--;
            if(timer < 0) {
                state = STATE.FOLLOWER;
            }
        }
    }
}
