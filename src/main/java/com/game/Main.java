package com.game;

import com.system.Engine;

/**
 *
 * @author evan.gabb
 */

public class Main {
    private static int[] res = {800, 600};
    private static int[] ring = {500, 500};
    private static int frameScale = 1;
    private static int players = 3;
    private static int[] initialPos = {};
    public static void main(String[] args) {
    	try {
    		parseArgs(args);
            	Engine engine = new Engine(res, ring, frameScale, players, initialPos);
            	engine.start();
    		if (false) {
    			throw new IllegalArgumentException();
    		}
            	System.out.println("Funcionou!");
    	} catch (Exception e) {
    		System.out.print("IO Error: " + e.getMessage());
    	}
    }
    private static void parseArgs (String[] args) {
        /*
        boolean definedRes = false;
        boolean definedNPlayers = false;

        this.players = ...
        pos = new int[players]

        if (!definedRes) {
            res[0] = ring[0] + 100;
            res[1] = ring[1] + 100;
        }
        else if (ring[0] > res[0] or ring[1] > res[1]) {
            throw new IllegalArgumentException("Display precisa ser maior que o ringue");
        }

        (int) frameScale;
        if (frameScale < 1) {
            throw new IllegalArgumentException("Scale deve ser maior do que 0");
        }

        */
        return;
    }
}
