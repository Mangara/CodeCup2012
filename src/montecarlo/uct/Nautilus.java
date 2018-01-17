/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package montecarlo.uct;

import framework.Pair;
import java.io.IOException;
import montecarlo.MCPlayer;

/**
 *
 * @author Sander
 */
public class Nautilus extends MCPlayer {

    private Node root; // The root of the current game tree; corresponds to the (real) current board position
    private Node[] path = new Node[25]; // pre-allocated array to store the path through the tree. 17 moves and 8 collapses ?
    // Program parameters
    private final static int timePerMoveBase = 500;
    private final static int timePerMoveCoefficient = 0;
    private final static int timePerCollapse = 50;
    private int timePerMove = timePerMoveBase;

    public static void main(String[] args) throws IOException {
        new Nautilus();
    }

    public Nautilus() throws IOException {
        System.err.println("R Nautilus");
        game.run(this);
    }

    @Override
    public byte[] selectFirstMove() {
        return new byte[]{6, 9}; // GJ
    }

    @Override
    public byte[] selectMove() {
        // If the tree is empty, create a root node with the current game position
        if (root == null) {
            root = new MoveNode(game);
        }

        storeCurrentState();

        // Play a ton of random games from the root
        timePerMove -= timePerMoveCoefficient;
        long start = System.currentTimeMillis();
        //while (System.currentTimeMillis() - start < timePerMove) {
        for (int i = 0; i < 1000; i++) {
            playGame();
            resetState();
        }

        //System.err.println("Played a total of " + root.nVisits + " random games in " + timePerMove + " ms");
        System.err.println("Played a total of " + root.nVisits + " random games in " + (System.currentTimeMillis() - start) + " ms");
        //((MoveNode) root).printChildren(game);

        // Return the child of the root that was visited the largest number of times
        return ((MoveNode) root).getMostFrequentlyPlayedMove();
    }

    @Override
    public byte selectCollapse(byte move1, byte move2) {
        storeCurrentState();

        // Play as much random games from the root as possible
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timePerCollapse) {
            playGame();
            resetState();
        }

        System.err.println("Played a total of " + root.nVisits + " random collapses in " + timePerMove + " ms");

        // Return the child of the root that was visited the largest number of times
        return ((CollapseNode) root).getMostFrequentlyPlayedMove();
    }

    /**
     * Plays one game, starting from the root.
     */
    private void playGame() {
        //System.err.println("Playing a UTC game from the current board position.");

        path[0] = root;
        int pathLength = 0;
        boolean leftTree;

        do {
            Pair<Node, Boolean> p = path[pathLength].performNextMove(game);
            pathLength++;
            path[pathLength] = p.getFirst();
            leftTree = p.getSecond();
        } while (!leftTree);

        //System.err.println("Playing remainder of the game randomly.");

        // Play out the rest of the game randomly
        playRandomGame();
        float myScore = (float) (game.computeMyScore() / 30.0); // Divide by 30 to get a score between 0 and 1
        float hisScore = (float) (game.computeHisScore() / 30.0); // Divide by 30 to get a score between 0 and 1

        //System.err.println("Scores: " + myScore + " (" + scores[0] + "), " + hisScore + " (" + scores[1] + ")");

        // Update node scores minimax style
        // The score should reflect the owner of the previous move.
        path[0].nVisits++;

        boolean mine = true;
        for (int i = 1; i <= pathLength; i++) {
            if (mine) {
                path[i].update(myScore, myScore * myScore);
            } else {
                path[i].update(hisScore, hisScore * hisScore);
            }

            if (path[i] instanceof MoveNode) {
                mine = !mine;
            }
        }

        //System.err.println();
    }

    @Override
    /**
     * Update the root of the tree to be the new game state.
     */
    public void notifyCollapse(byte collapse) {
        root = ((CollapseNode) root).getChild(collapse);
    }

    @Override
    /**
     * Update the root of the tree to be the new game state.
     */
    public void notifyMove(byte move1, byte move2) {
        if (root != null) {
            root = ((MoveNode) root).getChild(move1, move2);
        }
    }
}
