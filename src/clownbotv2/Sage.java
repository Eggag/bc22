package clownbotv2;

import battlecode.common.*;

import java.util.Random;

public class Sage extends RobotPlayer {

    enum STATE {FOLLOWER};
    enum MODE {NORMAL, HEALING};

    static STATE state = STATE.FOLLOWER;
    static MODE mode = MODE.HEALING;

    static int timer = 20;
    static int lastLeaderParity = 0;
    static int violationCounter = 0;
    static RobotInfo[] enemies;

    static final Random rng = new Random();

    static MapLocation target = null;
    static RobotInfo attackTarget = null;

    // Update hotspot, soldier count, etc.
    static void updateInfo() throws GameActionException {
        if(turnCount == 1){
            for(Direction dir : directions){
                MapLocation newLoc = rc.getLocation().add(dir);
                if(rc.canSenseLocation(newLoc)){
                    RobotInfo cr = rc.senseRobotAtLocation(newLoc);
                    if(cr != null){
                        if(cr.getTeam() == rc.getTeam() && cr.getType() == RobotType.ARCHON){
                            homeArchon = cr.getLocation();
                        }
                    }
                }
            }
        }
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
                    if(dist < d && rb.getTeam() == rc.getTeam().opponent()){
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
                        Navigation.goPSO(curTarget.location);
                        rc.attack(curTarget.location);
                    }
                }
            }
            else{
                Navigation.goPSO(curTarget.location);
                if (rc.canAttack(curTarget.location)) rc.attack(curTarget.location);
            }
        }
        curTarget = findNewTarget(true);
        if(curTarget != null){
            if(rc.canAttack(curTarget.location)) rc.attack(curTarget.location);
        }
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

    static void follower() throws GameActionException {
        SwarmInfo.get();
        //randomCombat();
        if(SwarmInfo.index < 0 || SwarmInfo.index >= 30) {
            // Needs to find a swarm
            findLeader();
            scouting();
            return;
        }else{
            combat();
            if(SwarmInfo.mode == 3) {
                // Merging mode!
                SwarmInfo.index = SwarmInfo.getNewLeader();
                SwarmInfo.get();
                violationCounter = 0;
            }
            // Go towards leader
            SwarmInfo.size++;
            SwarmInfo.write();
            Navigation.goPSO(SwarmInfo.leader);
            rc.setIndicatorString("F OF " + SwarmInfo.leader.x + " " + SwarmInfo.leader.y + " with " + violationCounter + " of " + SwarmInfo.index);
            // Reset timer
            timer = 30;
        }
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
    }

    static void healingMode() throws GameActionException {
        combat();
        Navigation.goPSOAvoid(homeArchon,enemies);
    }


    static void scouting() throws GameActionException {
        randomCombat();
        // Also try to go to target
        if(target == null || target.distanceSquaredTo(rc.getLocation()) < 13) {
            if(Math.random() < 0.5) {
                target = Hotspot.findClosestHotspot();
            }else{
                double r = Math.random();
                if(r < 0.33) target = new MapLocation(homeArchon.x, rc.getMapHeight() - homeArchon.y - 1);
                if(r < 0.66) target =  new MapLocation(rc.getMapWidth() - homeArchon.x - 1, rc.getMapHeight() - homeArchon.y - 1);
                else target = new MapLocation(rc.getMapWidth() - homeArchon.x - 1, homeArchon.y);
            }
        }
        Navigation.goPSO(target);
    }


    static void runSage() throws GameActionException {
        timer--;
        updateInfo();
        updateAlive(NUM_SOLDIERS_IND);
        if(mode == MODE.HEALING && rc.getHealth() >= rc.getType().health * 0.5) {
            mode = MODE.NORMAL;
        }
        if(mode == MODE.HEALING || rc.getHealth() < rc.getType().health * 0) {
            state = STATE.FOLLOWER;
            mode = MODE.HEALING;
            timer = 30;
            healingMode();
        }
        if(state == STATE.FOLLOWER) {
            rc.setIndicatorString("FOLLOWER " + SwarmInfo.index);
            follower();
        }
//        rc.setIndicatorString("HOME ARCHON: " + homeArchon);
    }
}