package orzbot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Soldier extends RobotPlayer {
    static boolean scout = false;
    static MapLocation target;
    static int archonID;
    static Direction momentum = Direction.CENTER;
    static MapLocation archonLocation;
    static boolean detached = false;
    static boolean avoidSoldier = false;
    static RobotInfo[] enemies;
    static int arriveAtTarget = 10000;

    static int calculateMomentum(Direction dir) {
        // Cross product for calculating how much it deviates from momentum
        return momentum.dx * dir.dx + momentum.dy * dir.dy;
    }

    static void getArchon() throws GameActionException {
        RobotInfo[] archons = rc.senseNearbyRobots(1,rc.getTeam());
        for(int i = 0;i < archons.length;++i) {
            if(archons[i].getType() == RobotType.ARCHON) {
                archonID = archons[i].getID();
                archonLocation = archons[i].getLocation();
            }
        }
    }

    static void getTarget() throws GameActionException {
        int message = rc.readSharedArray(archonID * 2);
        int targetX = (message >> 4) & (0b111111);
        int targetY = (message >> 10) & (0b111111);
        target = new MapLocation(targetX,targetY);
        arriveAtTarget = 10000;
    }

    static boolean getDetach() throws GameActionException {
        int message = rc.readSharedArray(archonID * 2 + 1);
        return ((message >> 4) & (0b111111)) == 1;
    }

    static void increaseSize() throws GameActionException {
        int message = rc.readSharedArray(archonID * 2 + 1);
        int newMessage = message + (1 << 10);
        rc.writeSharedArray(archonID * 2 + 1,newMessage);
    }

    static double soldierAvoidance(MapLocation loc) throws GameActionException {
        // Avoid soldier but attracted to miners
        double score = 0;
        for(RobotInfo enemy : enemies) {
            double hp = (double)enemy.getHealth() / (double)enemy.getType().getMaxHealth(0);
            if(enemy.getType() == RobotType.SOLDIER) {
                score += (hp) / (double)enemy.location.distanceSquaredTo(loc);
            }else if(enemy.getType() == RobotType.MINER) {
                double num = 1.0;
                if(rc.getRoundNum() <= 200) num = 2.0;
                score -= num / ((double)enemy.location.distanceSquaredTo(loc) * hp);
            }
        }
        return score;
    }


    static void scoutBehavior() throws GameActionException {
        combat();

        // Also try to move randomly.
        // Actively going to the swarm direction
        double bestScore = -1e18;
        Direction owo = Direction.CENTER;
        for(Direction dir : directions) {
            double uwu = evaluateScout(dir);
            if (uwu > bestScore) {
                bestScore = uwu;
                owo = dir;
            }
        }
        rc.move(owo);
        momentum = owo;
        return;
    }

    static void waitingSwarm() throws GameActionException {
        // Also try to move randomly.
        combat();
        Direction dir = directions[rng.nextInt(directions.length)];
        for (int i = 0;i < 10;++i) {
            if(!rc.canMove(dir)) continue;
            if(rc.getLocation().add(dir).distanceSquaredTo(archonLocation) <= 20 && rc.getLocation().add(dir).distanceSquaredTo(archonLocation) >= 4) {
                rc.move(dir);
                break;
            }
            System.out.println("Staying next to the swarm randomly");
        }
    }

    static MapLocation findTarget() throws GameActionException {
        for(int i = 4;i < 30;++i) {
            int message = rc.readSharedArray(i);
            if((message & (0b1111)) == HOTSPOT && Math.random() < 0.4) {
                int targetX = message >> 4 & (0b111111);
                int targetY = message >> 10 & (0b111111);
                rc.setIndicatorString(targetX + " HEHE " + targetY);
                return new MapLocation(targetX,targetY);
            }
        }
        return rc.getLocation();
    }

    static void disband() throws GameActionException {
        scout = true;
        avoidSoldier = true;
        target = findTarget();
    }

    static void addEnemy() throws GameActionException {
        int p = 4;
        for(int i = 0;i < enemies.length;++i) {
            if(enemies[i].team == rc.getTeam()) continue;
            MapLocation loc = enemies[i].getLocation();
            for(;p < 20;p++) {
                if(rc.readSharedArray(p) == 0 || ((rc.readSharedArray(p) & (0b1111)) == HOTSPOT && Math.random() < 0.4)) {
                    rc.writeSharedArray(p,HOTSPOT + (loc.x << 4) + (loc.y << 10));
                    break;
                }
            }
        }
    }

    static void combat() throws GameActionException {
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        enemies = rc.senseNearbyRobots(radius, opponent);
        addEnemy();
        int mn = 1000000;
        MapLocation bst = null;
        for(int i = 0;i < enemies.length;++i) {
            if(enemies[i].getType() == RobotType.MINER) continue;
            MapLocation toAttack = enemies[i].location;
            if (rc.canAttack(toAttack)) {
                if(enemies[i].getHealth() < mn){
                    mn = enemies[i].getHealth();
                    bst = toAttack;
                }
            }
        }
        if(bst != null) rc.attack(bst);
        else {
            for(int i = 0;i < enemies.length;++i) if(enemies[i].getType() == RobotType.MINER){
                MapLocation toAttack = enemies[i].location;
                if (rc.canAttack(toAttack)) {
                    if(enemies[i].getHealth() < mn){
                        mn = enemies[i].getHealth();
                        bst = toAttack;
                    }
                }
            }
            if(bst != null) rc.attack(bst);
        }
    }


    static double evaluateSwarm(Direction dir) throws GameActionException {
        final double targetCoefficient = -1;
        final double terrainCoefficient = -0.03;
        final double momentumCoefficient = 0.01;
        MapLocation newLocation = rc.getLocation().add(dir);

        if(!rc.canMove(dir)) return -1e18;

        double currentDistance = Math.sqrt(newLocation.distanceSquaredTo(target));
        double terrainDifficulty = rc.senseRubble(newLocation);
        double momentumAlignment = calculateMomentum(dir);
        double score = currentDistance * targetCoefficient + terrainDifficulty * terrainCoefficient + momentumAlignment * momentumCoefficient;
        return score;
    }

    static double evaluateScout(Direction dir) throws GameActionException {
        final double targetCoefficient = -0.5;
        final double terrainCoefficient = -0.03;
        final double momentumCoefficient = 0.1;
        MapLocation newLocation = rc.getLocation().add(dir);

        if(!rc.canMove(dir)) return -1e18;

        double currentDistance = Math.sqrt(newLocation.distanceSquaredTo(target));
        double terrainDifficulty = rc.senseRubble(newLocation);
        double momentumAlignment = calculateMomentum(dir);
        double score = currentDistance * targetCoefficient + terrainDifficulty * terrainCoefficient + momentumAlignment * momentumCoefficient;

        if(avoidSoldier) {
            final double avoidanceCoefficient = -0.5;
            score += soldierAvoidance(newLocation) * avoidanceCoefficient;
        }

        return score;
    }

    static void activeSwarm() throws GameActionException {
        combat();

        // Actively going to the swarm direction
        double bestScore = -1e18;
        Direction owo = Direction.CENTER;
        for(Direction dir : directions) {
            double uwu = evaluateSwarm(dir);
            if(uwu > bestScore) {
                bestScore = uwu;
                owo = dir;
            }
        }
        rc.move(owo);
        momentum = owo;
        return;
    }

    static void determineScoutTarget() throws GameActionException {
        MapLocation uwu = rc.getLocation();
        int height = rc.getMapHeight();
        int width = rc.getMapWidth();
        double rand = Math.random();
        if(rand < 0.33) {
            target = new MapLocation(uwu.x,height - uwu.y - 1);
        }else if(rand < 0.66) {
            target = new MapLocation(width - uwu.x - 1,uwu.y);
        }else{
            target = new MapLocation(width - uwu.x - 1,height - uwu.y - 1);
        }
    }

    static boolean outOfBound(MapLocation loc) {
        return (loc.x < 0 || loc.x >= rc.getMapWidth() || loc.y < 0 || loc.y >= rc.getMapHeight());
    }


    static void runSoldier() throws GameActionException {
        updateAlive(NUM_SOLDIERS_IND);
        if(turnCount == 1) {
            getArchon();
            if(rc.getRoundNum() <= 300 || rc.getRoundNum() % 10 < 5) {
                scout = true;
                avoidSoldier = true;
                determineScoutTarget();
            }
        }else{
            // Initialization for swarm
            if(!scout) {
                rc.setIndicatorString(detached + "");
                if(getDetach()) {
                    detached = true;
                    if(rc.getRoundNum() % 10 < 4) {
                        getTarget();
                    }else{
                        target = findTarget();
                        if(target.equals(rc.getLocation())) getTarget();
                    }
                }
                if(detached) {

                }else{
                    increaseSize();
                }
            }else{
                rc.setIndicatorString("SCOUT " + target.x + " " + target.y);
            }
        }

        if(scout) {
            scoutBehavior();
        }else{
            if(!detached) {
                // Waiting for recruitment
                waitingSwarm();
            }else{
                activeSwarm();
            }
        }
        if(rc.getLocation().distanceSquaredTo(target) < 10) {
            arriveAtTarget = Math.min(arriveAtTarget,rc.getRoundNum());
        }
        if(scout && (rc.getRoundNum() - arriveAtTarget >= 20 || outOfBound(target))) disband();
        if(!scout && (rc.getLocation().distanceSquaredTo(target) < 10 && rc.senseRobotAtLocation(target).getType() != RobotType.ARCHON)) disband();
    }
}
