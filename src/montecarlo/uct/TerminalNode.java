/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package montecarlo.uct;

import framework.Pair;
import framework.QTTTGame;

/**
 *
 * @author Sander
 */
public class TerminalNode extends Node {

    public TerminalNode() {
        variance = -1;
    }

    @Override
    public Pair<Node, Boolean> performNextMove(QTTTGame game) {
        throw new UnsupportedOperationException("The game is over.");
    }

    @Override
    public float getPriority(float lognVisitsParent) {
        return averageScore;
    }

    @Override
    public void update(float score01, float score01Sq) {
        if (averageScore != score01 && averageScore != 0) {
            System.err.println("Average score of terminal node (" + averageScore + ") differs from update score (" + score01 + ")");
        } else {
            averageScore = score01;
        }
        
        nVisits++;
    }

    @Override
    public String toString() {
        return "TerminalNode{" + "nVisits=" + nVisits + "averageScore=" + averageScore + "variance=" + variance + '}';
    }

}
