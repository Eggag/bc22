package nbpv4;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Map;

public class Soldier extends RobotPlayer {

    static boolean defender = true;
    static MapLocation attackGoal = null;

    static void tryDefend() throws GameActionException{
        RobotInfo[] units = rc.senseNearbyRobots();
        int bst = -100000000;
        MapLocation target = null;
        for(int i = 0; i < 8; i++) {
            int cur = 0;
            MapLocation loc = rc.getLocation().add(directions[i]);
            if(rc.onTheMap(loc) && rc.canMove(directions[i])){
                for(RobotInfo uwu : units){
                    if(uwu.team == rc.getTeam() && uwu.type == RobotType.SOLDIER) continue;
                    cur -= loc.distanceSquaredTo(uwu.location);
                }
                if(cur > bst){
                    bst = cur;
                    target = loc;
                }
            }
        }
        Navigation.go(target);
    }

    static void tryReportA() throws GameActionException{
        RobotInfo[] owo = rc.senseNearbyRobots(1000, rc.getTeam().opponent());
        for(RobotInfo cur : owo) if(cur.type == RobotType.ARCHON){
            int num = 4;
            num |= (cur.location.x << 9);
            num |= (cur.location.y << 3);
            int f = 0;
            for(int i = 0; i < maxMsg; i++){
                if(rc.readSharedArray(i) == num){
                    f = 1;
                    break;
                }
            }
            if(f == 0){
                for(int i = maxMsg - 1; i >= 0; i--) if(rc.readSharedArray(i) == 0){
                    rc.writeSharedArray(i, num);
                    break;
                }
            }
        }
    }

    static void tryAttack() throws GameActionException{
        if(attackGoal == null){
            for(int i = 0; i < 59; i++){
                int num = rc.readSharedArray(i);
                if((num & 0b111) == 3){
                    int x = ((num >> 9) & 0b111111), y = (num >> 3) & 0b111111;
                    int ind = Math.abs(rng.nextInt()) % 3;
                    if(ind == 0) attackGoal = new MapLocation(x, rc.getMapHeight() - y - 1);
                    if(ind == 1) attackGoal = new MapLocation(rc.getMapWidth() - x - 1, y);
                    if(ind == 2) attackGoal = new MapLocation(rc.getMapWidth() - x - 1, rc.getMapHeight() - y - 1);
                    if(turnCount % 3 == 0) break;
                }
            }
            for(int i = 0; i < 59; i++){
                int num = rc.readSharedArray(i);
                if((num & 0b111) == 4){
                    int x = ((num >> 9) & 0b111111), y = (num >> 3) & 0b111111;
                    attackGoal = new MapLocation(x, y);
                    break;
                }
            }
        }
        if(rc.canSenseLocation(attackGoal)){
            if(rc.senseRobotAtLocation(attackGoal) == null || (rc.senseRobotAtLocation(attackGoal).team == rc.getTeam())){
                int num = 4;
                num |= (attackGoal.x << 9);
                num |= (attackGoal.y << 3);
                for(int i = 0; i < 60; i++) if(rc.readSharedArray(i) == num){
                    rc.writeSharedArray(i, 0);
                }
                attackGoal = null;
            }
            else if(rc.senseRobotAtLocation(attackGoal).type == RobotType.ARCHON){
                int num = 4;
                num |= (attackGoal.x << 9);
                num |= (attackGoal.y << 3);
                int f = 0;
                for(int i = 0; i < maxMsg; i++){
                    if(rc.readSharedArray(i) == num){
                        f = 1;
                        break;
                    }
                }
                if(f == 0){
                    for(int i = maxMsg - 1; i >= 0; i--) if(rc.readSharedArray(i) == 0){
                        rc.writeSharedArray(i, num);
                        break;
                    }
                }
                Navigation.go(attackGoal);
            }
        }
        else{
            Navigation.go(attackGoal);
        }
    }

    static void runSoldier() throws GameActionException {
        if(turnCount == 1){
            if(rc.getRoundNum() % 4 == 0) defender = false;
        }
        if(rc.getRoundNum() % 100 == 0){
            defender = false;
        }
        tryReportA();
        if(rc.getRoundNum() % 2 == 0){
            int nm = rc.readSharedArray(61);
            rc.writeSharedArray(61, nm + 1);
        }
        else{
            int nm = rc.readSharedArray(60);
            rc.writeSharedArray(60, nm + 1);
        }
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        for(RobotInfo owo : enemies){
            if(!rc.isActionReady()) break;
            if(owo.type == RobotType.SOLDIER || owo.type == RobotType.SAGE || owo.type == RobotType.WATCHTOWER){
                if (rc.canAttack(owo.location)) {
                    rc.attack(owo.location);
                }
            }
        }
        for(RobotInfo owo : enemies){
            if(!rc.isActionReady()) break;
            if (rc.canAttack(owo.location)) {
                rc.attack(owo.location);
            }
        }
        if(defender){
            tryDefend();
        }
        else tryAttack();
    }
}
