/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package montecarlo.uct;

import framework.Pair;
import framework.QTTTGame;
import java.io.IOException;
import java.util.Arrays;
import montecarlo.MCPlayer;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class FickleFish extends MCPlayer {
    // Program parameters

    private final static int timePerMoveBase = 1150;
    private final static int timePerMoveCoefficient = 125;
    private final static int timePerCollapse = 50;
    private int timePerMove = timePerMoveBase;
    // Pre-allocated nodes for storing the scores of each move
    private UCBNode[][] scores = new UCBNode[16][16];

    public static void main(String[] args) throws IOException {
        new FickleFish();
    }

    public FickleFish() throws IOException {
        System.err.println("R FickleFish");

        for (int i = 0; i < 16; i++) {
            for (int j = 0; j < 16; j++) {
                scores[i][j] = new UCBNode();
            }
        }

        game.run(this);
    }

    @Override
    public byte[] selectFirstMove() {
        return new byte[]{5, 6}; // FG
        //return selectMove();
    }

    @Override
    public byte[] selectMove() {
        storeCurrentState();

        // Find all legal moves
        byte nFree = 0;
        byte[] legalMoves = new byte[16];

        for (byte i = 0; i < 16; i++) {
            if (game.board[i] == null) {
                legalMoves[nFree] = i;
                nFree++;
            }
        }

        if (nFree == 1) {
            return new byte[]{legalMoves[0], legalMoves[0]};
        }

        // Reset the scores for all legal moves
        // Use the first part of the array, instead of letting the indices map to the actual moves
        // Might be faster due to cache optimization for sequential access?
        for (int i = 0; i < nFree - 1; i++) {
            for (int j = i + 1; j < nFree; j++) {
                scores[i][j].reset();
            }
        }

        // Variables that will be used a lot inside the loop
        int k;
        byte move1;
        byte move2;
        float maxPriority;
        float sqrtlogn;
        float priority;

        long total = 0;

        // Play random games for a while, trying legal moves according to UCB
        timePerMove -= timePerMoveCoefficient;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timePerMove) {
            for (k = 0; k < 10; k++) {
                // Find the highest priority move
                move1 = -1;
                move2 = -1;
                maxPriority = 0;

                sqrtlogn = (float) Math.sqrt(Math.log(total));

                for (byte i = 0; i < nFree; i++) {
                    for (byte j = (byte) (i + 1); j < nFree; j++) {
                        if (scores[i][j].nVisits == 0) {
                            priority = Float.POSITIVE_INFINITY;
                        } else {
                            priority = scores[i][j].getPriority(sqrtlogn);
                        }

                        if (priority > maxPriority) {
                            maxPriority = priority;
                            move1 = i;
                            move2 = j;
                        }
                    }
                }

                game.move(legalMoves[move1], legalMoves[move2]);
                playRandomGame();
                float score = game.computeMyScore() / (float) 30;
                total++;

                // Process score
                scores[move1][move2].update(score, (float) Math.sqrt(score));

                resetState();
            }
        }

        System.err.println("Played a total of " + total + " random games in " + timePerMove + " ms");

        /*List<Pair<String, Node>> moves = new ArrayList<Pair<String, Node>>();

        for (byte i = 0; i < legalMoves.length; i++) {
        for (byte j = (byte) (i + 1); j < legalMoves.length; j++) {
        moves.add(new Pair<String, Node>(Character.toString(game.getCharacter(legalMoves[i])) + Character.toString(game.getCharacter(legalMoves[j])), scores[i][j]));
        }
        }

        Collections.sort(moves, new Comparator<Pair<String, Node>>() {

        public int compare(Pair<String, Node> o1, Pair<String, Node> o2) {
        return Float.compare(o1.getSecond().averageScore, o2.getSecond().averageScore);
        }
        });

        for (Pair<String, Node> pair : moves) {
        System.err.println(pair.getFirst() + " has been played " + pair.getSecond().nVisits + " times. It has an average score of " + pair.getSecond().averageScore);
        }*/

        // Return the most frequently played move
        int max = 0;
        byte maxMove1 = -1, maxMove2 = -1;

        for (byte i = 0; i < nFree; i++) {
            for (byte j = (byte) (i + 1); j < nFree; j++) {
                if (scores[i][j].nVisits > max) {
                    max = scores[i][j].nVisits;
                    maxMove1 = i;
                    maxMove2 = j;
                }
            }
        }

        System.err.println("Best move: " + Character.toString(game.getCharacter(legalMoves[maxMove1])) + Character.toString(game.getCharacter(legalMoves[maxMove2])) + " with " + max + " (" + (100 * max / (double) total) + "%) plays and average score " + scores[maxMove1][maxMove2].averageScore);

        return new byte[]{legalMoves[maxMove1], legalMoves[maxMove2]};
    }

    @Override
    public byte selectCollapse(byte move1, byte move2) {
        storeCurrentState();

        // See if either collapse leads to 4 in a row and decide from that
        int score1 = -1, score2 = -1;
        boolean lose1 = false, lose2 = false;

        game.collapse(move1);

        if (game.gameOver() || game.currentTurn == 17) {
            // TODO: optimize: reuse result of getOutcome to compute score myself
            score1 = game.computeMyScore();
            lose1 = game.getOutcome() < 0;
        }

        resetBoard();

        game.collapse(move2);

        if (game.gameOver() || game.currentTurn == 17) {
            score2 = game.computeMyScore();
            lose2 = game.getOutcome() < 0;
        }

        resetBoard();

        // Never give up
        if (lose1 && !lose2) {
            System.err.println("If I collapse to " + Character.toString(game.getCharacter(move1)) + " I lose, so collapse to " + Character.toString(game.getCharacter(move2)));
            return move2;
        } else if (lose2 && !lose1) {
            System.err.println("If I collapse to " + Character.toString(game.getCharacter(move2)) + " I lose, so collapse to " + Character.toString(game.getCharacter(move1)));
            return move1;
        }

        if (score1 > -1) {
            if (score2 > -1) {
                // Both collapses lead to an end of game: pick the best
                System.err.println("Both collapse options end the game, but " + (score1 > score2 ? Character.toString(game.getCharacter(move1)) : Character.toString(game.getCharacter(move2))) + " gives me " + (score1 > score2 ? score1 : score2) + " points, while " + (score1 > score2 ? Character.toString(game.getCharacter(move2)) : Character.toString(game.getCharacter(move1))) + " only gives me " + (score1 > score2 ? score2 : score1));
                return (score1 > score2 ? move1 : move2);
            } else {
                // Only collapsing move1 leads to an end of game; this must be a win
                System.err.println("If I collapse to " + Character.toString(game.getCharacter(move1)) + " I win, so do it.");
                return move1;
            }
        } else if (score2 > -1) {
            // Only collapsing move2 leads to an end of game; this must be a win
            System.err.println("If I collapse to " + Character.toString(game.getCharacter(move2)) + " I win, so do it.");
            return move2;
        }

        long n1 = 0, n2 = 0, s1 = 0, s2 = 0;
        boolean collapse1 = true;

        // Play random games for a while, trying both oprions alternatingly
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timePerCollapse) {
            game.collapse(collapse1 ? move1 : move2);
            playRandomGame();
            int score = game.computeMyScore();

            // Process score
            if (collapse1) {
                s1 += score;
                n1++;
            } else {
                s2 += score;
                n2++;
            }

            collapse1 = !collapse1;

            // Restore the game state
            resetState();
        }

        System.err.println("Tried both colapse options roughly " + n1 + " times");

        // Pick the best of these options
        if (s1 / (double) n1 > s2 / (double) n2) {
            System.err.println("Best option: " + Character.toString(game.getCharacter(move1)) + " with average score " + (s1 / (double) (n1 * 30)));
            return move1;
        } else {
            System.err.println("Best option: " + Character.toString(game.getCharacter(move2)) + " with average score " + (s2 / (double) (n2 * 30)));
            return move2;
        }
    }

    class UCBNode extends Node {

        @Override
        public Pair<Node, Boolean> performNextMove(QTTTGame game) {
            return null;
        }
    }
}
