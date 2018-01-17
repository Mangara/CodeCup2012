/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package montecarlo.uct;

import framework.Pair;
import framework.QTTTGame;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public class CollapseNode extends Node {

    private byte position1, position2; // The two possible collapse positions
    private Node child1, child2; // The children representing the two resulting game states

    public CollapseNode(byte position1, byte position2) {
        this.position1 = position1;
        this.position2 = position2;
    }

    public Node getChild(byte collapse) {
        if (collapse == position1) {
            return child1;
        } else {
            return child2;
        }
    }

    @Override
    public Pair<Node, Boolean> performNextMove(QTTTGame game) {
        double priority1, priority2;
        float sqrtlogn = (float) Math.sqrt(Math.log(nVisits));

        if (child1 == null) {
            priority1 = 1;
        } else {
            priority1 = child1.getPriority(sqrtlogn);
        }

        if (child2 == null) {
            priority2 = 1;
        } else {
            priority2 = child2.getPriority(sqrtlogn);
        }

        Boolean newChild = Boolean.FALSE;

        if (priority1 > priority2) {
            game.collapse(position1);

            if (child1 == null) {
                if (game.gameOver() || game.currentTurn == 17) {
                    child1 = new TerminalNode();
                } else {
                    child1 = new MoveNode(game);
                }

                newChild = Boolean.TRUE;
            } else if (child1 instanceof TerminalNode) {
                newChild = Boolean.TRUE;
            }

            return new Pair<Node, Boolean>(child1, newChild);
        } else {
            game.collapse(position2);

            if (child2 == null) {
                if (game.gameOver() || game.currentTurn == 17) {
                    child2 = new TerminalNode();
                } else {
                    child2 = new MoveNode(game);
                }

                newChild = Boolean.TRUE;
            } else if (child2 instanceof TerminalNode) {
                newChild = Boolean.TRUE;
            }

            return new Pair<Node, Boolean>(child2, newChild);
        }
    }

    byte getMostFrequentlyPlayedMove() {
        if (child1 == null) {
            if (child2 == null) {
                // Neither was played; return the first
                return position1;
            } else {
                // Only 2 was played
                return position2;
            }
        } else {
            if (child2 == null) {
                // Only 1 was played
                return position1;
            } else {
                return (child1.nVisits > child2.nVisits ? position1 : position2);
            }
        }
    }
}
