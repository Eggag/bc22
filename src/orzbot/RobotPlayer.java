package orzbot;

import battlecode.common.*;
import java.util.Random;

public strictfp class RobotPlayer {
    static RobotController rc;

    static int turnCount = 0;
    static final Random rng = new Random();

    static int NUM_SOLDIERS_IND = 63;
    static int NUM_MINERS_IND = 62;
    static int NUM_ARCHONS_IND = 61;
    static int NUM_ARCHONS_IND_2 = 60;
    static int INCOME_IND = 59;
    static int AGGRO_IND = 58;
    static int MAX_MSG = 58;

    static int OUR_ARCHON_CODE = 1;
    static int ARCHON_DRECTION = 2;
    static int HOTSPOT = 3;

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
        Direction.CENTER,
    };

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        while (true) {
            turnCount += 1;
            try{
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

    static void updateAlive(int index) throws GameActionException{
        int nm = rc.readSharedArray(index);
        rc.writeSharedArray(index, nm + 1);
    }

}
