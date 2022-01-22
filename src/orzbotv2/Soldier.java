package orzbotv2;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Soldier extends RobotPlayer {
    static boolean scout = false;
    static MapLocation target = null;
    static int archonID;
    static Direction momentum = Direction.CENTER;
    static MapLocation archonLocation;
    static boolean detached = false;
    static boolean avoidSoldier = false;
    static RobotInfo[] enemies;
    static RobotInfo[] teammates;
    static int dlbLen = 4;
    static MapLocation[] dontLookBack = new MapLocation[dlbLen];
    static int dlbInd = 0;
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

    static double dontLookBackFactor(MapLocation loc) throws GameActionException {
        for(int i = 0;i < dlbLen;++i) {
            if(loc.equals(dontLookBack[i])) return 1;
        }
        return 0;
    }

    static void updateDontLookBack() {
        dontLookBack[dlbInd] = rc.getLocation();
        dlbInd = (1 + dlbInd) % dlbLen;
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
        int rad = rc.getType().actionRadiusSquared;
        RobotInfo[] friends = rc.senseNearbyRobots(1000, rc.getTeam());
        int fr = 0;
        for(RobotInfo uwu : friends){
            if(uwu.getType() == RobotType.SOLDIER) fr++;
        }
        for(RobotInfo enemy : enemies) {
            double hp = (double)enemy.getHealth() / (double)enemy.getType().getMaxHealth(0);
            if(enemy.getType() == RobotType.SOLDIER){
                if(rc.getLocation().distanceSquaredTo(archonLocation) <= 30) {
                    score -= (3.0 * hp) / (double) enemy.location.distanceSquaredTo(loc);
                }
                else{
                    if(fr > 2) {
                        score -= (hp * (double)(fr)) / (double) enemy.location.distanceSquaredTo(loc);
                    }
                }
            }
            else if(enemy.getType() == RobotType.MINER) {
                double num = 1.0;
                if(rc.getRoundNum() <= 300) num = 2.0;
                if(enemy.location.distanceSquaredTo(loc) <= rad) hp *= 0.3;
                score -= num / ((double)enemy.location.distanceSquaredTo(loc) * hp);
            }
            else if(enemy.getType() == RobotType.ARCHON){
                score -= 10;
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
        updateDontLookBack();
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
        double rand1 = Math.random();
        if(rand1 < 0.5){
            //we try to find an archon to go to
            for(int p = 30; p < 50; p++){
                int num = rc.readSharedArray(p);
                if((num & (0b1111)) == ENEMY_ARCHON){
                    int targetX = num >> 4 & (0b111111);
                    int targetY = num >> 10 & (0b111111);
                    target = new MapLocation(targetX, targetY);
                    if(Math.random() < 0.5) break;
                }
            }
            if(target != null) return target;
        }
        MapLocation bst = null;
        int mn = 100000;
        MapLocation[] hot = new MapLocation[30];
        int ind = 0;
        for(int i = 4;i < 30;++i) {
            int message = rc.readSharedArray(i);
            if ((message & (0b1111)) == HOTSPOT) {
                int targetX = message >> 4 & (0b111111);
                int targetY = message >> 10 & (0b111111);
                MapLocation owo = new MapLocation(targetX, targetY);
                hot[ind++] = owo;
            }
        }
        for(int i = 4;i < 30;++i) {
            int message = rc.readSharedArray(i);
            if((message & (0b1111)) == HOTSPOT) {
                int targetX = message >> 4 & (0b111111);
                int targetY = message >> 10 & (0b111111);
                MapLocation loc = new MapLocation(targetX, targetY);
                int sur = 0;
                for(int j = 0; j < ind; j++){
                    if(hot[j].distanceSquaredTo(loc) <= 50) sur++;
                }
                int score = rc.getLocation().distanceSquaredTo(loc) - 3 * sur;
                if(score < mn){
                    mn = score;
                    bst = loc;
                }
            }
        }
        if(bst != null) return bst;
        return rc.getLocation();
    }

    static void disband() throws GameActionException {
        scout = true;
        avoidSoldier = true;
        target = null;
        target = findTarget();
    }

    static void addEnemy() throws GameActionException {
        for(int i = 0;i < enemies.length;++i) {
            if(enemies[i].getType() == RobotType.MINER) {
                rc.writeSharedArray(NUM_ENEMY_MINERS_IND,rc.readSharedArray(NUM_ENEMY_MINERS_IND) + 1);
            }
            MapLocation loc = enemies[i].getLocation();
            int f = 0;
            for(int p1 = 4;p1 < 30;p1++) {
                if(rc.readSharedArray(p1) == 0){
                    rc.writeSharedArray(p1,HOTSPOT + (loc.x << 4) + (loc.y << 10));
                    f = 1;
                    break;
                }
            }
            if(f == 0) {
                for (int p1 = 4; p1 < 30; p1++) {
                    if (((rc.readSharedArray(p1) & (0b1111)) == HOTSPOT && Math.random() < 0.4)) {
                        rc.writeSharedArray(p1, HOTSPOT + (loc.x << 4) + (loc.y << 10));
                        break;
                    }
                }
            }
        }
        for(RobotInfo en : enemies) if(en.getType() == RobotType.ARCHON){
            MapLocation loc = en.getLocation();
            int f = 0;
            for(int p = 30 ;p < 50;p++) {
                if(rc.readSharedArray(p) == 0) {
                    rc.writeSharedArray(p,ENEMY_ARCHON + (loc.x << 4) + (loc.y << 10));
                    f = 1;
                    break;
                }
            }
            if(f == 0) {
                for (int p = 30; p < 50; p++) {
                    if (((rc.readSharedArray(p) & (0b1111)) == ENEMY_ARCHON && Math.random() < 0.4)) {
                        rc.writeSharedArray(p, ENEMY_ARCHON + (loc.x << 4) + (loc.y << 10));
                        break;
                    }
                }
            }
        }
    }

    static void combat() throws GameActionException {
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        enemies = rc.senseNearbyRobots(radius, opponent);
        teammates = rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam());
        addEnemy();
        int mn = 1000000;
        MapLocation bst = null;
        for(int i = 0;i < enemies.length;++i) {
            if(enemies[i].getType() != RobotType.SOLDIER) continue;
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
            for(RobotInfo en : enemies){
                if(en.getType() == RobotType.ARCHON){
                    if(rc.canAttack(en.location)) rc.attack(en.location);
                }
            }
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
        final double cohesionCoefficient = 0;
        final double dontLookBackCoefficient = -100;
        MapLocation newLocation = rc.getLocation().add(dir);

        if(!rc.canMove(dir)) return -1e18;

        double currentDistance = Math.sqrt(newLocation.distanceSquaredTo(target));
        double terrainDifficulty = rc.senseRubble(newLocation);
        double momentumAlignment = calculateMomentum(dir);
        double score = currentDistance * targetCoefficient + terrainDifficulty * terrainCoefficient + momentumAlignment * momentumCoefficient + cohesionCoefficient * cohesionFactor(newLocation) + dontLookBackCoefficient * dontLookBackFactor(newLocation);
        return score;
    }

    static double evaluateScout(Direction dir) throws GameActionException {
        final double targetCoefficient = -0.5;
        final double terrainCoefficient = -0.03;
        final double momentumCoefficient = 0.1;
        final double cohesionCoefficient = 0;
        final double dontLookBackCoefficient = -100;
        MapLocation newLocation = rc.getLocation().add(dir);

        if(!rc.canMove(dir)) return -1e18;

        double currentDistance = Math.sqrt(newLocation.distanceSquaredTo(target));
        double terrainDifficulty = rc.senseRubble(newLocation);
        double momentumAlignment = calculateMomentum(dir);
        double score = currentDistance * targetCoefficient + terrainDifficulty * terrainCoefficient + momentumAlignment * momentumCoefficient + dontLookBackCoefficient * dontLookBackFactor(newLocation);

        if(avoidSoldier) {
            final double avoidanceCoefficient = -0.5;
            score += soldierAvoidance(newLocation) * avoidanceCoefficient;
        }else{
            score += cohesionCoefficient * cohesionFactor(newLocation);
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
        updateDontLookBack();
        return;
    }

    static void determineScoutTarget() throws GameActionException {
        MapLocation uwu = rc.getLocation();
        int height = rc.getMapHeight();
        int width = rc.getMapWidth();
        double rand1 = Math.random();
        if(rand1 < 0.7){
            //we try to find an archon to go to
            for(int p = 30; p < 50; p++){
                int num = rc.readSharedArray(p);
                if((num & (0b1111)) == ENEMY_ARCHON){
                    int targetX = num >> 4 & (0b111111);
                    int targetY = num >> 10 & (0b111111);
                    target = new MapLocation(targetX, targetY);
                    if(Math.random() < 0.3) break;
                }
            }
            if(target != null) return;
        }
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

    static boolean isScout() throws GameActionException{
        int sz = rc.getMapHeight() * rc.getMapWidth();
        if(sz <= 1000){
            if(rc.getRoundNum() <= 300) return true;
            else if(rc.getRoundNum() <= 600){
                if(rc.getRoundNum() % 10 <= 6) return true;
                else return false;
            }
            else{
                if(rc.getRoundNum() % 10 <= 4) return true;
                else return false;
            }
        }
        else if(sz <= 2000){
            if(rc.getRoundNum() <= 350) return true;
            else if(rc.getRoundNum() <= 700){
                if(rc.getRoundNum() % 10 <= 6) return true;
                else return false;
            }
            else{
                if(rc.getRoundNum() % 10 <= 4) return true;
                else return false;
            }
        }
        else{
            if(rc.getRoundNum() <= 450) return true;
            else if(rc.getRoundNum() <= 800){
                if(rc.getRoundNum() % 10 <= 6) return true;
                else return false;
            }
            else{
                if(rc.getRoundNum() % 10 <= 4) return true;
                else return false;
            }
        }
    }

    static double cohesionFactor(MapLocation loc) {
        if(true) return 0;
        double score = 0;
        for(int i = 0;i < teammates.length;++i) {
            score += 1.0 / Math.max(1,teammates[i].getLocation().distanceSquaredTo(loc));
        }
        score /= Math.max(1,teammates.length);
        return score;
    }

    static double scoutRatio() throws GameActionException {
        if(rc.getRoundNum() < 200) return 1;
        int numOfEnemyMiners = rc.readSharedArray(NUM_ENEMY_MINERS_IND_2);
        int numOfSoldiers = rc.readSharedArray((NUM_SOLDIERS_IND));
        double ratio = numOfEnemyMiners / rc.readSharedArray(NUM_SOLDIERS_IND_2);
        return ratio;
    }


    static void runSoldier() throws GameActionException {
        updateAlive(NUM_SOLDIERS_IND);
        if(turnCount == 1) {
            getArchon();
            if(isScout()){
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
        for(int p = 30; p < 50; p++){
            int num = rc.readSharedArray(p);
            if((num & (0b1111)) == ENEMY_ARCHON){
                int targetX = num >> 4 & (0b111111);
                int targetY = num >> 10 & (0b111111);
                MapLocation loc = new MapLocation(targetX, targetY);
                if(rc.canSenseLocation(loc)){
                    RobotInfo r = rc.senseRobotAtLocation(loc);
                    if(r == null || (r.getType() != RobotType.ARCHON || r.getTeam() != rc.getTeam().opponent())){
                        rc.writeSharedArray(p, 0);
                    }
                }
            }
        }
        if(scout && rc.getLocation().distanceSquaredTo(target) <= 5){
            int f = 0;
            for(RobotInfo en : enemies){
                if(en.getType() == RobotType.ARCHON){
                    target = en.location;
                    f = 1;
                }
            }
            if(f == 0) disband();
        }
        if(!scout && (rc.getLocation().distanceSquaredTo(target) < 10)){
            int f = 0;
            for(RobotInfo en : enemies){
                if(en.getType() == RobotType.ARCHON){
                    target = en.location;
                    f = 1;
                }
            }
            if(f == 0) disband();
        }
    }
}
