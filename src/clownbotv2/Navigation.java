package clownbotv2;
import battlecode.common.*;

public class Navigation extends RobotPlayer {
    static MapLocation[] lastLoc = new MapLocation[11];
    static Direction momentum = Direction.CENTER;
    static int dlbLen = 4;
    static MapLocation[] dontLookBack = new MapLocation[dlbLen];
    static int dlbInd = 0;

    static MapLocation target = null;

    static int sz = 0;
    static int dist = 0;

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

    static void go(MapLocation goal) throws GameActionException {
        if(!rc.isActionReady()) return;
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

    static void goPSO(MapLocation goal) throws GameActionException {
        double bestScore = -1e18;
        Direction owo = Direction.CENTER;
        for(Direction dir : directions) {
            double uwu = evaluatePSO(dir,goal);
            if (uwu > bestScore) {
                bestScore = uwu;
                owo = dir;
            }
        }
        if(rc.canMove(owo)) rc.move(owo);
        momentum = owo;
        updateDontLookBack();
    }

    static double evaluatePSO(Direction dir,MapLocation goal) throws GameActionException {
        final double targetCoefficient = -0.6;
        final double terrainCoefficient = -0.03;
        final double momentumCoefficient = 0.01;
        final double dontLookBackCoefficient = -100;
        MapLocation newLocation = rc.getLocation().add(dir);

        if(!rc.canMove(dir)) return -1e18;

        double currentDistance = Math.sqrt(newLocation.distanceSquaredTo(goal));
        double terrainDifficulty = rc.senseRubble(newLocation);
        double momentumAlignment = calculateMomentum(dir);
        double score = currentDistance * targetCoefficient + terrainDifficulty * terrainCoefficient + momentumAlignment * momentumCoefficient + dontLookBackCoefficient * dontLookBackFactor(newLocation);
        return score;
    }

    static int calculateMomentum(Direction dir) {
        // Cross product for calculating how much it deviates from momentum
        return momentum.dx * dir.dx + momentum.dy * dir.dy;
    }
}