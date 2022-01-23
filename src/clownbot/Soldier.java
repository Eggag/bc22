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
    static RobotInfo attackTarget = null;

    // Update hotspot, soldier count, etc.
    static void updateInfo() throws GameActionException {
        enemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam().opponent());
        Hotspot.addEnemies(enemies);
    }

    static void randomCombat() throws GameActionException {
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

    static void combat() throws GameActionException{
        MapLocation goal = SwarmInfo.leader;
        RobotInfo curTarget = null;
        int d = 10000000;
        for(int i = 0; i < 10; i++){
            Direction dir;
            if(i < 9) dir = directions[i];
            else dir = Direction.CENTER;
            MapLocation newLoc = goal.add(dir);
            if(rc.canSenseLocation(newLoc)){
                RobotInfo rb = rc.senseRobotAtLocation(newLoc);
                if(rb != null){
                    int dist = rb.getHealth() + rb.getLocation().distanceSquaredTo(rc.getLocation()) / 5;
                    if(attackingUnit(rb)) dist -= 20;
                    int rad = rc.getType().actionRadiusSquared;
                    if(dist < d){
                        d = dist;
                        curTarget = rb;
                    }
                }
            }
        }
        if(curTarget != null){
            int attackRadius = rc.getType().actionRadiusSquared;
            if(attackingUnit(curTarget)) {
                if (curTarget.location.distanceSquaredTo(rc.getLocation()) <= attackRadius) {
                    //attack and retreat
                    if (rc.canAttack(curTarget.location)) rc.attack(curTarget.location);
                    Navigation.goOP(curTarget.location);
                } else {
                    //come forth and attack (only worth if we can actually attack)
                    if (rc.canAttack(curTarget.location)) {
                        Navigation.go(curTarget.location);
                        rc.attack(curTarget.location);
                    }
                }
            }
            else{
                Navigation.go(curTarget.location);
                if (rc.canAttack(curTarget.location)) rc.attack(curTarget.location);
            }
        }
        curTarget = findNewTarget(true);
        if(curTarget != null){
            if(rc.canAttack(curTarget.location)) rc.attack(curTarget.location);
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

    static RobotInfo findNewTarget(boolean inRad) throws GameActionException{
        //find target based on health + distance
        int dist = 10000000;
        RobotInfo bst = null;
        int rad = rc.getType().actionRadiusSquared;
        for (RobotInfo rb : enemies) {
            if (rb.getType() == RobotType.SOLDIER || rb.getType() == RobotType.WATCHTOWER || rb.getType() == RobotType.SAGE) {
                int d = rb.getHealth() + rb.getLocation().distanceSquaredTo(rc.getLocation()) / 5;
                if(inRad){
                    if(rc.getLocation().distanceSquaredTo(rb.getLocation()) > rad) continue;
                }
                if (d < dist) {
                    dist = d;
                    bst = rb;
                }
            }
        }
        if (bst == null) {
            for (RobotInfo rb : enemies) {
                int d = rb.getHealth() + rb.getLocation().distanceSquaredTo(rc.getLocation()) / 5;
                if(rb.getType() == RobotType.ARCHON) d -= 200;
                if(inRad){
                    if(rc.getLocation().distanceSquaredTo(rb.getLocation()) > rad) continue;
                }
                if (d < dist) {
                    dist = d;
                    bst = rb;
                }
            }
        }
        return bst;
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
        if(attackTarget == null){
            attackTarget = findNewTarget(false);
        }
        else {
            int f = 0;
            //try seeing if it is still in vision range
            for (RobotInfo rb : enemies) {
                if (rb.getID() == attackTarget.getID()) {
                    attackTarget = rb;
                    f = 1;
                }
            }
            if (f == 0){
                //try moving towards it if it is out of sight, hopefully we reach it again
                if(rc.isMovementReady()){
                    Navigation.go(attackTarget.location);
                    int f1 = 0;
                    for(RobotInfo rb : enemies){
                        if(rb.getID() == attackTarget.getID()){
                            attackTarget = rb;
                            f1 = 1;
                        }
                    }
                    if(f1 == 0) attackTarget = findNewTarget(false);
                }
            }
        }
        if(attackTarget != null){
            if(attackingUnit(attackTarget)) {
                int attackRadius = rc.getType().actionRadiusSquared;
                if (attackTarget.location.distanceSquaredTo(rc.getLocation()) <= attackRadius) {
                    //attack and retreat
                    if (rc.canAttack(attackTarget.location)) rc.attack(attackTarget.location);
                    Navigation.goOP(attackTarget.location);
                } else {
                    //come forth and attack (only worth if we can actually attack)
                    if (rc.canAttack(attackTarget.location)) {
                        Navigation.go(attackTarget.location);
                        rc.attack(attackTarget.location);
                    }
                }
            }
            else{
                Navigation.go(attackTarget.location);
                if(rc.canAttack(attackTarget.location)) rc.attack(attackTarget.location);
            }
        }
        if(rc.isMovementReady()) Navigation.go(target);
        if(!merge()) {
            SwarmInfo.leader = rc.getLocation();
            if(attackTarget != null) SwarmInfo.leader = attackTarget.location;
            SwarmInfo.parity ^= 1;
            SwarmInfo.size = 1;
            SwarmInfo.write();
        }

    }

    static void follower() throws GameActionException {
        SwarmInfo.get();
        //randomCombat();
        combat();
        if(lastLeaderParity == SwarmInfo.parity) {
            // Parity Violation!
            violationCounter++;
            if(violationCounter >= 3) {
                // Leader is DEAD
                rc.setIndicatorString("DEAD LEADER!");
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
            rc.setIndicatorString("F OF " + SwarmInfo.leader.x + " " + SwarmInfo.leader.y + " with " + violationCounter + " of " + SwarmInfo.index);
            // Reset timer
            timer = 30;
        }
    }

    static void transformation() throws GameActionException {
        rc.setIndicatorString("TRANSFORMING!");
        violationCounter = 0;
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
