package com.game;

import com.system.Engine;
        
/**
 *
 * @author evan.gabb
 */

public class Main {
    public static void main(String[] args) {
        Engine engine = new Engine(2);
        engine.start();
        System.out.println("Funcionou!");
    }
}
