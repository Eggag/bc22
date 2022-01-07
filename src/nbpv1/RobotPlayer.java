package nbpv1;

import battlecode.common.*;
import java.util.Random;

public strictfp class RobotPlayer {
    static RobotController rc;

    static int turnCount = 0;
    static final Random rng = new Random(6195);

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
        while (true) {
            turnCount += 1;
            try {
                switch (rc.getType()) {
                    case ARCHON:     Archon.runArchon(rc);  break;
                    case MINER:      Miner.runMiner(rc);   break;
                    case SOLDIER:    Soldier.runSoldier(rc); break;
                    case LABORATORY: Laboratory.runLaboratory(rc); break;
                    case WATCHTOWER: Watchtower.runWatchtower(rc); break;
                    case BUILDER:    Builder.runBuilder(rc); break;
                    case SAGE:       Sage.runSage(rc); break;
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
}
