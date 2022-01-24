package clownbotv2;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import java.awt.*;

public class SwarmInfo extends RobotPlayer {
    static int index = -1;
    static int size = 0;
    static MapLocation leader;
    static int parity = 0;
    static int mode = 0;

    static void get() throws GameActionException {
        if(index < 0 || index >= 30) return;
        int message = (rc.readSharedArray(index)) | (rc.readSharedArray(index + 1) << 16);
        size = (message) & (0b111111);
        leader = new MapLocation((message >> 6) & (0b111111),(message >> 12) & (0b111111));
        parity = ((message >> 18) & (0b1));
        mode = (message >> 30) & (0b11);
    }

    static void write() throws GameActionException {
        if(index < 0 || index >= 30) return;
        int newMessage = size | (leader.x << 6) | (leader.y << 12) | (parity << 18) | (mode << 30);
        rc.writeSharedArray(index,newMessage & (0b1111111111111111));
        rc.writeSharedArray(index + 1,(newMessage >> 16) & (0b1111111111111111));
    }

    static void clear() throws GameActionException {
        if(index == -1) return;
        rc.writeSharedArray(index,0);
        rc.writeSharedArray(index + 1,0);
        index = -1;
    }

    static int getNewLeader() throws GameActionException {
        if(mode == 3) {
            return rc.readSharedArray(index);
        }
        return -1;
    }

    static void resignWithNewLeader(int ind) throws GameActionException {
        rc.writeSharedArray(index,ind);
        rc.writeSharedArray(index + 1,3 << 14);
    }

}
