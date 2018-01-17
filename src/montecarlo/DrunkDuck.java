/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package montecarlo;

import java.io.IOException;

/**
 *
 * @author Sander
 */
public class DrunkDuck extends MCPlayer {

    // Program parameters
    private final static int timePerMoveBase = 1150;
    private final static int timePerMoveCoefficient = 125;
    private final static int timePerCollapse = 50;
    private int timePerMove = timePerMoveBase;

    public static void main(String[] args) throws IOException {
        new DrunkDuck();
    }

    public DrunkDuck() throws IOException {
        System.err.println("R DrunkDuck");

        game.run(this);
    }

    @Override
    public byte[] selectFirstMove() {
        return new byte[]{5, 6};
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

        byte i1 = 0, i2 = 1;
        long[][] number = new long[16][16];
        long[][] sum = new long[16][16];
        long total = 0;

        // Play random games for a while, trying every legal move 1 by 1
        timePerMove -= timePerMoveCoefficient;
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timePerMove) {
            // Find the next legal move
            byte move1 = legalMoves[i1];
            byte move2 = legalMoves[i2];

            i2++;

            if (i2 == nFree) {
                i1 = (byte) ((i1 + 1) % (nFree - 1));
                i2 = (byte) (i1 + 1);
            }

            game.move(move1, move2);
            playRandomGame();
            int score = game.computeMyScore();

            // Process score
            sum[move1][move2] += score;
            number[move1][move2]++;
            total++;

            resetState();
        }

        System.err.println("Tried every available move roughly " + number[legalMoves[0]][legalMoves[1]] + " times, for a total of " + total + " random games in " + timePerMove + " ms");

        /*for (byte b1 = 0; b1 < nFree; b1++) {
            for (byte b2 = (byte) (b1 + 1); b2 < nFree; b2++) {
                System.err.println(Character.toString(game.getCharacter(legalMoves[b1])) + Character.toString(game.getCharacter(legalMoves[b2])) + " = " + sum[legalMoves[b1]][legalMoves[b2]] / (double) number[legalMoves[b1]][legalMoves[b2]]);
            }
        }*/

        // Return the best move
        double max = 0;
        byte maxMove1 = -1, maxMove2 = -1;

        for (byte i = 0; i < 16; i++) {
            for (byte j = (byte) (i + 1); j < 16; j++) {
                double avg = sum[i][j] / (double) number[i][j];

                if (avg > max) {
                    max = avg;
                    maxMove1 = i;
                    maxMove2 = j;
                }
            }
        }

        System.err.println("Best move: " + Character.toString(game.getCharacter(maxMove1)) + Character.toString(game.getCharacter(maxMove2)) + " with average score " + max);

        return new byte[]{maxMove1, maxMove2};
    }

    @Override
    public byte selectCollapse(byte move1, byte move2) {
        storeCurrentBoard();

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
            resetBoard();
        }

        System.err.println("Tried both colapse options roughly " + n1 + " times");

        // Pick the best of these options
        if (s1 / (double) n1 > s2 / (double) n2) {
            System.err.println("Best option: " + Character.toString(game.getCharacter(move1)) + " with average score " + (s1 / (double) n1));
            return move1;
        } else {
            System.err.println("Best option: " + Character.toString(game.getCharacter(move2)) + " with average score " + (s2 / (double) n2));
            return move2;
        }
    }
}
