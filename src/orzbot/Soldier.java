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
        int targetX = (message >> 4) & (0b11111111);
        int targetY = (message >> 10) & (0b11111111);
        target = new MapLocation(targetX,targetY);
    }

    static boolean getDetach() throws GameActionException {
        int message = rc.readSharedArray(archonID * 2 + 1);
        return ((message >> 4) & (0b11111111)) > 0;
    }

    static void increaseSize() throws GameActionException {
        int message = rc.readSharedArray(archonID * 2 + 1);
        int newMessage = message + (1 << 10);
        rc.writeSharedArray(archonID * 2 + 1,newMessage);
    }

    static void scoutBehavior() throws GameActionException {
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
            System.out.println("Scout moving randomly");
        }
    }

    static void waitingSwarm() throws GameActionException {
        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            if(rc.getLocation().add(dir).distanceSquaredTo(archonLocation) <= 20) rc.move(dir);
            System.out.println("Staying next to the swarm randomly");
        }
    }

    static double evaluateSwarm(Direction dir) throws GameActionException {
        final double targetCoefficient = 1;
        final double terrainCoefficient = 1;
        final double momentumCoefficient = 1;
        MapLocation newLocation = rc.getLocation().add(dir);
        double currentDistance = Math.sqrt(newLocation.distanceSquaredTo(target));
        double terrainDifficulty = rc.senseRubble(newLocation);
        double momentumAlignment = calculateMomentum(dir);
        double score = currentDistance * targetCoefficient + terrainDifficulty * terrainCoefficient + momentumAlignment * momentumCoefficient;
        return score;
    }

    static void activeSwarm() throws GameActionException {
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
        if(rc.getRoundNum() % 3 == 0) {
            target = new MapLocation(uwu.x,height - uwu.y - 1);
        }else if(rc.getRoundNum() % 3 == 1) {
            target = new MapLocation(width - uwu.x - 1,uwu.y);
        }else{
            target = new MapLocation(width - uwu.x - 1,height - uwu.y - 1);
        }
    }

    static void runSoldier() throws GameActionException {
        if(turnCount == 1) {
            getArchon();
            if(rc.getRoundNum() % 3 < 1) {
                scout = true;
                determineScoutTarget();
            }
        }else{
            // Initialization for swarm
            if(!scout) {
                if(detached) {

                }else{
                    getTarget();
                    increaseSize();
                    if(getDetach()) {
                        detached = true;
                    }
                }
            }
        }

        if(scout) {
            scoutBehavior();
        }else{
            if(detached) {
                // Waiting for recruitment
                waitingSwarm();
            }else{
                activeSwarm();
            }
        }
    }
}
