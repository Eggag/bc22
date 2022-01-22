package clownbot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import java.awt.*;

public class SwarmInfo extends RobotPlayer {
    static int index = -1;
    static int size = 0;
    static MapLocation leader;
    static MapLocation attack;
    static int mode = 0;

    static void get() throws GameActionException {
        int message = (rc.readSharedArray(index)) | (rc.readSharedArray(index + 1) << 16);
        size = (message) & (0b111111);
        leader = new MapLocation((message >> 6) & (0b111111),(message >> 12) & (0b111111));
        attack = new MapLocation((message >> 18) & (0b111111),(message >> 24) & (0b111111));
        mode = (message >> 30) & (0b11);
    }

    static void write() throws  GameActionException {
        int newMessage = size | (leader.x << 6) | (leader.y << 12) | (attack.x << 18) | (attack.y << 24) | (mode << 30);
        rc.writeSharedArray(index,newMessage & (0b1111111111111111));
        rc.writeSharedArray(index + 1,(newMessage >> 16) & (0b1111111111111111));
    }

}
