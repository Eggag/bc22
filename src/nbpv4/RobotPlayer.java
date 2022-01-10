package nbpv4;

import battlecode.common.*;
import java.util.Random;

public strictfp class RobotPlayer {
    static RobotController rc;

    static int turnCount = 0;
    static final Random rng = new Random();

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");
        RobotPlayer.rc = rc;
        while (true) {
            turnCount += 1;
            try {
                switch (rc.getType()) {
                    case ARCHON:     Archon.runArchon();  break;
                    case MINER:      Miner.runMiner();   break;
                    case SOLDIER:    Soldier.runSoldier(); break;
                    case LABORATORY: Laboratory.runLaboratory(); break;
                    case WATCHTOWER: Watchtower.runWatchtower(); break;
                    case BUILDER:    Builder.runBuilder(); break;
                    case SAGE:       Sage.runSage(); break;
                }
            }
            catch (GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
            catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
            finally {
                Clock.yield();
            }
        }
    }


    static void tryReportR() throws GameActionException {
        //try to delete stuff that isnt there
        int l = 0, r = 32;
        if(turnCount % 2 == 1){
            l = 32;
            r = 59;
        }
        for(int t = 1; t <= 2; t++) {
            for(int i = l; i < r; i++) {
                int num = rc.readSharedArray(i);
                if ((num & 0b111) == t) {
                    int x = ((num >> 9) & 0b111111), y = (num >> 3) & 0b111111;
                    MapLocation loc = new MapLocation(x, y);
                    if(t == 1){
                        if(rc.canSenseLocation(loc) && rc.senseGold(loc) == 0) rc.writeSharedArray(i, 0);
                    }
                    else{
                        if(rc.canSenseLocation(loc) && rc.senseLead(loc) == 0) rc.writeSharedArray(i, 0);
                    }
                }
            }
        }
        //we try to report resources we can see
        for(int t = 1; t <= 2; t++) {
            MapLocation[] cur = rc.senseNearbyLocationsWithGold(1000);
            if(t == 2){
                cur = rc.senseNearbyLocationsWithLead(1000);
            }
            for (MapLocation loc : cur) {
                int mnDist = 1000000;
                for (int i = 0; i < 59; i++) {
                    int num = rc.readSharedArray(i);
                    if ((num & 0b111) == t) {
                        int x = ((num >> 9) & 0b111111), y = (num >> 3) & 0b111111;
                        int curDist = loc.distanceSquaredTo(new MapLocation(x, y));
                        if (curDist < mnDist) mnDist = curDist;
                    }
                    if(mnDist <= 20) break;
                }
                if (mnDist > 20) {
                    for (int i = 0; i < 59; i++) {
                        int num = rc.readSharedArray(i);
                        if (num == 0) {
                            num = t;
                            num |= (loc.x << 9);
                            num |= (loc.y << 3);
                            rc.writeSharedArray(i, num);
                            break;
                        }
                    }
                }
            }
        }
    }

}
