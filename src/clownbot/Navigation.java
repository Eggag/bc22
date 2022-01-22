package clownbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import java.util.Arrays;

public class Navigation extends RobotPlayer {
    static int[] allRubbles = new int[101];
    static int[][] map = new int[9][9];
    static int[][] dist = new int[9][9];
    static int[] rubbles = new int[10];
    static int[] queueSizes = new int[10];
    static int[] where = new int[10];
    static MapLocation[][] queues = new MapLocation[10][81];
    static Direction[][] bestDirection = new Direction[9][9];
    static int rs = 0;
    static int maxVis = 4;
    static int maxVisSquared = 20;

    static void go(MapLocation goal) throws GameActionException {
        rc.setIndicatorString("STAGE -1");
        for(int i = 0;i < 10;++i) {
            queueSizes[i] = 0;
            where[i] = 0;
        }
        for(int i = 0;i <= 100;++i) {
            allRubbles[i] = 0;
        }
        int rubbleAverage = 0;
        int rubbleCnt = 0;
        rc.setIndicatorString("STAGE 0");
        MapLocation uwu = rc.getLocation();
        for(int i = -maxVis;i <= maxVis;++i) {
            for(int j = -maxVis;j <= maxVis;++j) {
//                rc.setIndicatorString("STAGE 0 " + i + " " + j);
                dist[i + maxVis][j + maxVis] = 100000000;
                MapLocation owo = new MapLocation(uwu.x + i,uwu.y + j);
                if(owo.distanceSquaredTo(uwu) <= maxVisSquared && rc.onTheMap(owo)) {
                    int r = rc.senseRubble(owo);
                    map[i + maxVis][j + maxVis] = 10 + r;
                    rubbleAverage += 10 + r;
                    rubbleCnt++;
                    allRubbles[r] = 1;
                }else{
                    map[i + maxVis][j + maxVis] = 100000000;
                }
            }
        }
        rc.setIndicatorString("STAGE 1");
        rubbleAverage /= rubbleCnt;
        rs = 0;
        for(int i = 0;i <= 100;++i) {
            if(allRubbles[i] != 0 && rs < 10) {
                rubbles[rs++] = i;
            }
            allRubbles[i] = rs;
        }
        rc.setIndicatorString("STAGE 3");
        dist[0][0] = 0;
        for(Direction dir : directions) {
            if(rc.canMove(dir)) {
                dist[dir.dx + maxVis][dir.dy + maxVis] = map[dir.dx + maxVis][dir.dy + maxVis];
                int cur = allRubbles[map[dir.dx + maxVis][dir.dy + maxVis]];
                if(cur != 0) {
                    cur--;
                    queues[cur][queueSizes[cur]++] = uwu.add(dir);
                    bestDirection[dir.dx + maxVis][dir.dy + maxVis] = dir;
                }
            }
        }
        rc.setIndicatorString("STAGE 4");

        while(true) {
            int bst = 1000000000;
            MapLocation best = null;
            for(int i = 0;i < rs;++i) {
                if(where[i] == queueSizes[i]) continue;
                MapLocation owo = queues[i][where[i]];
                if(dist[owo.x - uwu.x + maxVis][owo.y - uwu.y + maxVis] < bst) {
                    bst = dist[owo.x - uwu.x + maxVis][owo.y - uwu.y + maxVis];
                    best = owo;
                }
            }
            if(best == null) break;
            for(Direction dir : directions) {
                MapLocation owo = best.add(dir);
                int curDist = dist[best.x - uwu.x + maxVis][best.y - uwu.y + maxVis];
                int r = map[owo.x - uwu.x + maxVis][owo.y - uwu.y + maxVis];
                int newDist = curDist + r;
                if(rc.onTheMap(owo) && newDist < dist[owo.x - uwu.x + maxVis][owo.y - uwu.y + maxVis]) {
                    dist[owo.x - uwu.x + maxVis][owo.y - uwu.y + maxVis] = newDist;
                    bestDirection[owo.x - uwu.x + maxVis][owo.y - uwu.y + maxVis] = bestDirection[best.x - uwu.x + maxVis][best.y - uwu.y + maxVis];
                    int cur = allRubbles[r];
                    if(cur != 0) {
                        cur--;
                        queues[cur][queueSizes[cur]++] = owo;
                    }
                }
            }
        }
        rc.setIndicatorString("STAGE 5");

        int best = 1000000000;
        MapLocation bst = uwu;
        for(int i = -maxVis;i <= maxVis;++i) {
            for(int j = -maxVis;j <= maxVis;++j) {
                MapLocation owo = new MapLocation(uwu.x + i,uwu.y + j);
                if(rc.onTheMap(owo)) {
                    dist[i + maxVis][j + maxVis] += 2 * rubbleAverage * Math.sqrt(goal.distanceSquaredTo(owo));
                    if(dist[i + maxVis][j + maxVis] < best) {
                        best = dist[i + maxVis][j + maxVis];
                        bst = owo;
                    }
                }
            }
        }
        rc.setIndicatorString("STAGE 6");

        Direction bestDir = bestDirection[bst.x - uwu.x + maxVis][bst.y - uwu.y + maxVis];
        if(rc.canMove(bestDir)) rc.move(bestDir);
    }
}
