package clownbotv2;

import battlecode.common.*;

public class Builder extends RobotPlayer {

    static RobotInfo[] friends;

    static void updateInfo() throws GameActionException{
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
        friends = rc.senseNearbyRobots(1000, rc.getTeam());
    }

    static void tryBuildLab() throws GameActionException{
        MapLocation bstHeal = null;
        int crBst = 10000;
        int f = 0;
        for(RobotInfo uwu : friends){
            if(uwu.getType() == RobotType.LABORATORY){
                f = 1;
                int hp = uwu.getHealth();
                if(hp == uwu.getType().getMaxHealth(uwu.getLevel())) continue;
                if(hp < crBst){
                    crBst = hp;
                    bstHeal = uwu.location;
                }
            }
        }
        if(bstHeal != null){
            if(rc.canRepair(bstHeal)) rc.repair(bstHeal);
        }
        int sageCnt = rc.readSharedArray(NUM_SAGE_IND_2);
        int soldierCnt = rc.readSharedArray(NUM_SOLDIERS_IND_2);
        if(sageCnt > 10 && sageCnt * 3 < soldierCnt || rc.getTeamLeadAmount(rc.getTeam()) > 1000) f = 0;
        if(f == 0) {
            rc.writeSharedArray(NEED_LAB_IND, 1);
            Direction bst = null;
            int d = -100000;
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.LABORATORY, dir)) {
                    int di = homeArchon.distanceSquaredTo(rc.getLocation().add(dir)) - 100 * rc.senseRubble(rc.getLocation().add(dir));
                    if (di > d) {
                        d = di;
                        bst = dir;
                    }
                }
            }
            if (bst != null) {
                rc.buildRobot(RobotType.LABORATORY, bst);
            }
        }
    }

    static void tryHeal() throws GameActionException{
        int rad = rc.getType().actionRadiusSquared;
        int lw = 1000000;
        RobotInfo bst = null;
        for(RobotInfo uwu : friends){
            if(uwu.getType() == RobotType.ARCHON && uwu.getLocation().distanceSquaredTo(rc.getLocation()) <= rad){
                int hp = uwu.getHealth();
                if(hp == uwu.getType().getMaxHealth(uwu.getLevel())) continue;
                if(hp < lw){
                    bst = uwu;
                    lw = hp;
                }
            }
        }
        if(bst != null){
            if(rc.canRepair(bst.location)) rc.repair(bst.location);
        }
    }

    static void runBuilder() throws GameActionException {
        updateInfo();
        tryHeal();
        tryBuildLab();
    }
}