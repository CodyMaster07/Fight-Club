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
    private static int[] initialPos = new int[players];
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
        boolean definedRes = false;
        boolean definedNPlayers = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                // Ajuda
                case "-h":
                case "--help":
                    System.out.println("Usage: java -cp target/classes com.game.Main [options]\n");
                    System.out.println("\t-h\n\t--help\n\t\tExibe esta mensagem de ajuda.");
                    System.out.println("\t-d <largura> <altura>\n\t--display <largura> <altura>\n\t\tConfigura a resolução total da janela. Não pode ser menor que o tamanho do ringue. Padrão = 800x600.");
                    System.out.println("\t-r <largura> <altura>\n\t--ringue <largura> <altura>\n\t\tConfigura o tamanho do ringue. Padrão = 500x500.");
                    System.out.println("\t-p <quantidade> [quadrantes]\n\t--players <quantidade> [quadrantes]\n\t\tConfigura o número de jogadores em cada time. Pode-se também definir o quadrante do ringue em que cada um irá começar. Pode ser também alterado no menu (FIX). Padrão = 3. Quadrante 0 será um quadrante aleatório (preferencialmente não ocupado).");
                    System.out.println("\t-s\n\t--scale\n\t\tDefine a escala (~zoom) da janela. Tem que ser um valor inteiro. Padrão = 1.");
                    System.exit(0);
                    break;
                // Resolução
                case "-d":
                case "--display":
                    if (i + 2 >= args.length) {
                        throw new IllegalArgumentException("Erro: Argumento faltando após --display.");
                    }
                    for (int j = 0; j < 2; j++) {
                        try {
                            res[j] = Integer.parseInt(args[++i]);
                        } catch (Exception e) {
                            throw new IllegalArgumentException(String.format("Erro: Argumento inválido após --display: \"%s\" não é um inteiro.", args[i]));
                        }
                    }
                    definedRes = true;
                    break;
                // Tamanho do ringue
                case "-r":
                case "--ringue":
                    if (i + 2 >= args.length) {
                        throw new IllegalArgumentException("Erro: Argumento faltando após --ringue.");
                    }
                    for (int j = 0; j < 2; j++) {
                        try {
                            ring[j] = Integer.parseInt(args[++i]);
                        } catch (Exception e) {
                            throw new IllegalArgumentException(String.format("Erro: Argumento inválido após --ring: \"%s\" não é um inteiro.", args[i]));
                        }
                    }
                    // Se não definiu a resolução, colocar uma borda de 50px
                    if (!definedRes) {
                        res[0] = ring[0] + 100;
                        res[1] = ring[1] + 100;
                    } else if (ring[0] > res[0] || ring[1] > res[1]) {
                        throw new IllegalArgumentException("Display precisa ser maior que o ringue.");
                    }
                    break;
                // Nº de jogadores e posições
                case "-p":
                case "--players":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Erro: Argumento faltando após --players.");
                    }
                    try {
                        players = Integer.parseInt(args[++i]);
                        initialPos = new int[players];
                        for (int j = 0; j < players && ++i < args.length && args[i].charAt(0) != '-'; j++) {
                            initialPos[j] = Integer.parseInt(args[i]);
                        }
                    } catch (Exception e) {
                        throw new IllegalArgumentException(String.format("Erro: Argumento inválido após --players: \"%s\" não é um inteiro.", args[i]));
                    }
                    for (int j = 0; j < players; j++) {
                        if (initialPos[j] < 0 || initialPos[j] > 4) {
                            throw new IllegalArgumentException(String.format("Erro: Posição inválida após --players: \"%d\" não é um quadrante válido.", initialPos[j]));
                        }
                    }
                    break;
                // Escala
                // TODO: Permitir float?
                case "-s":
                case "--scale":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Erro: Argumento faltando após --scale.");
                    }
                    try {
                        frameScale = Integer.parseInt(args[++i]);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(String.format("Erro: Argumento inválido após --scale: \"%s\" não é um inteiro.", args[i]));
                    }
                    if (frameScale < 1) {
                        throw new IllegalArgumentException(String.format("Erro: Escala inválida em --scale: \"%d\" não é um valor válido.", frameScale));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Erro: Argumento não reconhecido\nUtilize -h para ver as opções.");
            }
        }
        return;
    }
}
