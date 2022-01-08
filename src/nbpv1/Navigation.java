package nbpv1;
import battlecode.common.*;

public class Navigation extends RobotPlayer {
    static MapLocation[] lastLoc = new MapLocation[11];
    static int sz = 0;
    static void go(MapLocation goal) throws GameActionException {
        if(sz < 11){
            lastLoc[sz] = rc.getLocation();
            sz++;
        }
        else{
            for(int i = 0; i < 10; i++) lastLoc[i] = lastLoc[i + 1];
            lastLoc[10] = rc.getLocation();
        }
        Direction bst = Direction.CENTER;
        int mn = 10000000;
        int curDirStart = (int) (Math.random() * directions.length);
        for (int i = 0; i < 8; i++) {
            Direction dir = directions[(curDirStart + i) % 8];
            MapLocation nxt = rc.getLocation().add(dir);
            int f = 1;
            for (int j = 0; j < sz; j++)
                if (lastLoc[j].equals(nxt)) {
                    f = 0;
                    break;
                }
            if (rc.canMove(dir) && f > 0) {
                if (goal.distanceSquaredTo(nxt) < mn) {
                    bst = dir;
                    mn = goal.distanceSquaredTo(nxt);
                }
            }
        }
        if(bst != Direction.CENTER && rc.canMove(bst)) rc.move(bst);
    }
}
