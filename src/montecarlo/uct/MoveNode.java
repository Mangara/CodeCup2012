/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package montecarlo.uct;

import framework.Pair;
import framework.QTTTGame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class MoveNode extends Node {

    private byte[] legalMoves; // All legal moves from the current board systuation
    private Node[][] children; // All combinations of legal moves
    private float[] fieldPriority;

    public MoveNode(QTTTGame game) {
        // Find all legal moves
        byte nFree = 0;

        for (byte i = 0; i < 16; i++) {
            if (game.board[i] == null) {
                nFree++;
            }
        }

        legalMoves = new byte[nFree];
        fieldPriority = new float[nFree];
        children = new Node[nFree][nFree];
        byte n = 0;

        for (byte i = 0; i < 16; i++) {
            if (game.board[i] == null) {
                legalMoves[n] = i;
                fieldPriority[n] = initialPriority[i] + (game.superpositions[i].isEmpty() ? 0 : 0.1f);
                n++;
            }
        }
    }

    public Node getChild(byte move1, byte move2) {
        byte i1 = -1, i2 = -1;

        for (byte i = 0; i < legalMoves.length; i++) {
            if (legalMoves[i] == move1) {
                i1 = i;
            }
            if (legalMoves[i] == move2) {
                i2 = i;
            }
        }

        return children[i1][i2];
    }

    byte[] getMostFrequentlyPlayedMove() {
        byte move1 = -1, move2 = -1;
        long maxVisits = 0;

        for (byte i = 0; i < legalMoves.length; i++) {
            for (byte j = (byte) (i + 1); j < legalMoves.length; j++) {
                if (children[i][j] != null) {
                    if (children[i][j].nVisits > maxVisits) {
                        maxVisits = children[i][j].nVisits;
                        move1 = i;
                        move2 = j;
                    }
                }
            }
        }

        if (move1 == -1) {
            if (legalMoves.length == 1) {
                return new byte[]{legalMoves[0], legalMoves[0]};
            } else {
                return new byte[]{legalMoves[0], legalMoves[1]};
            }
        } else {
            return new byte[]{legalMoves[move1], legalMoves[move2]};
        }
    }

    @Override
    public Pair<Node, Boolean> performNextMove(QTTTGame game) {
        byte move1 = -1, move2 = -1;

        if (legalMoves.length == 1) {
            move1 = 0;
            move2 = 0;
        } else {
            // Find the highest priority move
            double maxPriority = 0;

            float sqrtlogn = (float) Math.sqrt(Math.log(nVisits));

            for (byte i = 0; i < legalMoves.length; i++) {
                for (byte j = (byte) (i + 1); j < legalMoves.length; j++) {
                    float priority;

                    if (children[i][j] == null) {
                        priority = fieldPriority[i] + fieldPriority[j];
                    } else {
                        priority = children[i][j].getPriority(sqrtlogn);
                    }

                    if (priority > maxPriority) {
                        maxPriority = priority;
                        move1 = i;
                        move2 = j;
                    }
                }
            }
        }

        // Perform this move and create a new tree node if it has not been done yet
        if (children[move1][move2] == null) {
            boolean cycle = game.move(legalMoves[move1], legalMoves[move2]);

            if (cycle) {
                children[move1][move2] = new CollapseNode(legalMoves[move1], legalMoves[move2]);
                return new Pair<Node, Boolean>(children[move1][move2], Boolean.FALSE); // Never stop on a collapse
            } else {
                children[move1][move2] = new MoveNode(game);
                return new Pair<Node, Boolean>(children[move1][move2], Boolean.TRUE);
            }
        } else {
            game.move(legalMoves[move1], legalMoves[move2]);
            return new Pair<Node, Boolean>(children[move1][move2], Boolean.FALSE);
        }
    }

    void printChildren(QTTTGame game) {
        List<Pair<String, Node>> moves = new ArrayList<Pair<String, Node>>();

        for (byte i = 0; i < legalMoves.length; i++) {
            for (byte j = (byte) (i + 1); j < legalMoves.length; j++) {
                moves.add(new Pair<String, Node>(Character.toString(game.getCharacter(legalMoves[i])) + Character.toString(game.getCharacter(legalMoves[j])), children[i][j]));
            }
        }

        Collections.sort(moves, new Comparator<Pair<String, Node>>() {

            public int compare(Pair<String, Node> o1, Pair<String, Node> o2) {
                return Float.compare(o1.getSecond().averageScore, o2.getSecond().averageScore);
            }
        });

        for (Pair<String, Node> pair : moves) {
            System.err.println(pair.getFirst() + " has been played " + pair.getSecond().nVisits + " times. It has an average score of " + pair.getSecond().averageScore);
        }
    }
    private static final float[] initialPriority = new float[]{
        0.55f, 0.5f, 0.5f, 0.55f,
        0.5f, 0.6f, 0.6f, 0.5f,
        0.5f, 0.6f, 0.6f, 0.5f,
        0.55f, 0.5f, 0.5f, 0.55f
    };
}
