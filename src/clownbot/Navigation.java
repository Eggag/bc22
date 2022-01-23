package clownbot;
import battlecode.common.*;

public class Navigation extends RobotPlayer {
    static MapLocation[] lastLoc = new MapLocation[11];

    static MapLocation target = null;

    static int sz = 0;
    static int dist = 0;

    static void go(MapLocation goal) throws GameActionException {
        target = goal;
        dist = rc.getLocation().distanceSquaredTo(goal);
        Direction bst = Direction.CENTER;
        int mn = Integer.MAX_VALUE;
        for(int i = 0; i < 8; i++) if(rc.onTheMap(rc.getLocation().add(directions[i]))){
            MapLocation cur = rc.getLocation().add(directions[i]);
            if(cur.distanceSquaredTo(target) > dist) continue;
            int cost = rc.senseRubble(rc.getLocation());
            for(int j = 0; j < 8; j++) if(rc.onTheMap(cur.add(directions[j]))){
                MapLocation uwu = cur.add(directions[j]);
                if(uwu.distanceSquaredTo(target) > dist) continue;
                cost += rc.senseRubble(cur);
                if(cost + uwu.distanceSquaredTo(target) / 20 < mn){
                    mn = cost + uwu.distanceSquaredTo(target) / 20;
                    bst = directions[i];
                }
            }
        }
        fuzzy(rc.getLocation().add(bst));
    }

    static void fuzzy(MapLocation goal) throws GameActionException{
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

    static void goOP(MapLocation danger) throws GameActionException {
        MapLocation bst = null;
        int sc = 0;
        for(int i = -5; i <= 5; i++){
            for(int j = -5; j <= 5; j++) {
                MapLocation pos = new MapLocation(rc.getLocation().x + i, rc.getLocation().y + j);
                if(danger.distanceSquaredTo(pos) > sc){
                    sc = danger.distanceSquaredTo(pos);
                    bst = pos;
                }
            }
        }
        if(bst != null) Navigation.go(bst);
    }
}