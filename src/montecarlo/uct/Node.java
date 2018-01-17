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
public abstract class Node {

    private static final float SQRT2 = (float) Math.sqrt(2);
    protected int nVisits;
    private float sqrtVisits;
    float averageScore;
    protected float variance; // averageSquaredScore - averageScore * averageScore;

    /**
     * Returns the highest priority child of this node and performs the corresponding move.
     * The boolean is true if this is the first time this move was performed and a new node was created for the child.
     * @return
     */
    public abstract Pair<Node, Boolean> performNextMove(QTTTGame game);

    /**
     * Returns the priority of visiting this node, as given by the UCB1-TUNED
     * formula from:
     * Modifications of UCT and sequence-like simulations for Monte-Carlo Go
     * by Yizao Wang and Sylvain Gelly.
     * @param sqrtLognVisitsParent - the square root of the natural logarithm of the number of times the parent has been visited
     * @return
     */
    public float getPriority(float sqrtLognVisitsParent) {
        float sqrtLognDivS = sqrtLognVisitsParent / sqrtVisits;
        float varEstimation = variance + SQRT2 * sqrtLognDivS;

        if (varEstimation < 0.25) {
            return averageScore + sqrtLognDivS * (float) Math.sqrt(varEstimation);
        } else {
            return averageScore + sqrtLognDivS * 0.5f;
        }
    }

    /**
     * Updates the fields of this node with the score obtained from a game playing this move.
     * The score should be between 0 and 1.
     * @param score01
     */
    public void update(float score01, float score01Sq) {
        float sumScores = averageScore * nVisits + score01;
        float sumSquaredScores = (variance + averageScore * averageScore) * nVisits + score01Sq;

        nVisits++;
        sqrtVisits = (float) Math.sqrt(nVisits);
        averageScore = sumScores / nVisits;
        variance = (sumSquaredScores / nVisits) - averageScore * averageScore;
        //System.err.println("Updated: " + toString());
    }

    public void reset() {
        nVisits = 0;
        sqrtVisits = 0;
        averageScore = 0;
        variance = 0;
    }

    @Override
    public String toString() {
        return "Node{" + "nVisits=" + nVisits + "sqrtVisits=" + sqrtVisits + "averageScore=" + averageScore + "variance=" + variance + '}';
    }
}
