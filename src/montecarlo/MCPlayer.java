package montecarlo;

import framework.QTTTGame;
import framework.QTTTPlayer;
import framework.State;
import framework.SuperPosition;

/**
 *
 * @author Sander Verdonschot <sander.verdonschot at gmail.com>
 */
public abstract class MCPlayer extends QTTTPlayer {

    // Variables to store one previous game state
    private State[] realBoard = new State[16];
    private int realBinaryBoard;
    private byte realCurrentTurn;
    private SuperPosition[] realComponents = new SuperPosition[16];
    // Variables to store an additional previous board state
    private State[] realBoard2 = new State[16];
    private int realBinaryBoard2;

    public void playRandomGame() {
        while (game.currentTurn < 17 && !game.gameOver()) {
            // Make a random move
            byte move1 = randomMove();
            byte move2 = randomMove();

            while (move1 == move2 && game.currentTurn < 16) {
                move2 = randomMove();
            }

            if (game.move(move1, move2)) {
                // We created a cycle; choose which position to collapse
                //selectSimulationCollapse(move1, move2);
                if (QTTTGame.random.nextBool()) {
                    game.collapse(move1);
                } else {
                    game.collapse(move2);
                }
            }
        }
    }

    private void selectSimulationCollapse(byte move1, byte move2) {
        // try to avoid collapsing into a win for the opponent if possible (based on who gets to decide ofc)
        boolean myDecision = (game.meFirst ? game.currentTurn % 2 == 1 : game.currentTurn % 2 == 0);

        storeCurrentBoard2();

        game.collapse(move1);
        if (game.gameOver()) {
            int outcome = game.getOutcome();

            if (outcome > 0) {
                if (myDecision) {
                    // I won; this is the right decision, so do nothing
                } else {
                    // My opponent lost; he wouldn't let this happen if he could avoid it, so pick the other option
                    resetBoard2();
                    game.collapse(move2);
                }

                return;
            } else if (outcome < 0) {
                if (myDecision) {
                    // I lost; I wouldn't let this happen if I could avoid it, so pick the other option
                    resetBoard2();
                    game.collapse(move2);
                } else {
                    // My opponent won; this is the right decision, so do nothing
                }

                return;
            }
        }

        resetBoard2();
        game.collapse(move2);

        if (game.gameOver()) {
            int outcome = game.getOutcome();

            if (outcome > 0) {
                if (myDecision) {
                    // I won; this is the right decision, so do nothing
                } else {
                    // My opponent lost; he wouldn't let this happen if he could avoid it, so pick the other option
                    resetBoard2();
                    game.collapse(move1);
                }

                return;
            } else if (outcome < 0) {
                if (myDecision) {
                    // I lost; I wouldn't let this happen if I could avoid it, so pick the other option
                    resetBoard2();
                    game.collapse(move1);
                } else {
                    // My opponent won; this is the right decision, so do nothing
                }

                return;
            }
        }

        // Neither ends the game; pick one randomly
        if (QTTTGame.random.nextBool()) {
            game.collapse(move1);
        } else {
            game.collapse(move2);
        }
    }

    /**
     * Stores the current game state. The last stored state can be recovered using resetState()
     */
    public void storeCurrentState() {
        System.arraycopy(game.board, 0, realBoard, 0, 16);
        realBinaryBoard = game.binaryBoard;
        realCurrentTurn = game.currentTurn;

        for (int i = 0; i < 16; i++) {
            realComponents[i] = game.superpositions[i].getConnectedComponent();
        }
    }

    /**
     * Stores only the current board. The game can be reset to this board using resetBoard().
     */
    public void storeCurrentBoard() {
        System.arraycopy(game.board, 0, realBoard, 0, 16);
        realBinaryBoard = game.binaryBoard;
    }

    /**
     * Stores only the current board, using different variables than for storeCurrentState or storeCurrentBoard. The game can be reset to this board using resetBoard2().
     */
    public void storeCurrentBoard2() {
        System.arraycopy(game.board, 0, realBoard2, 0, 16);
        realBinaryBoard2 = game.binaryBoard;
    }

    /**
     * Resets the game state to the last stored state.
     */
    public void resetState() {
        System.arraycopy(realBoard, 0, game.board, 0, 16);
        game.binaryBoard = realBinaryBoard;
        game.currentTurn = realCurrentTurn;

        for (int i = 0; i < 16; i++) {
            game.superpositions[i].clear();
        }

        // Add the original moves to the correct superposition lists and to the correct components
        // TODO: optimize. (both moves are always in the same component)
        // Actually, I only need to remove moves that were done after the current turn and reset all connected components
        // Components of moves that have collapsed are never used and don't need to be inserted?
        for (int i = 1; i < game.currentTurn; i++) {
            byte pos1 = game.moves1[i].position;
            SuperPosition comp1 = realComponents[pos1];

            if (comp1 != null) {
                // This move has not collapsed yet
                game.superpositions[game.moves1[i].position].add(game.moves1[i]);

                if (comp1 == game.moves1[i]) {
                    game.moves1[i].parent = null;
                } else {
                    game.moves1[i].parent = comp1;
                }
            } else {
                System.out.println("Component is null!");
            }

            byte pos2 = game.moves2[i].position;
            SuperPosition comp2 = realComponents[pos2];

            if (comp2 != null) {
                // This move has not collapsed yet
                game.superpositions[game.moves2[i].position].add(game.moves2[i]);

                if (comp2 == game.moves2[i]) {
                    game.moves2[i].parent = null;
                } else {
                    game.moves2[i].parent = comp2;
                }
            } else {
                System.out.println("Component is null!");
            }
        }
    }

    /**
     * Resets the current board to the last stored one, using either storeCurrentState or storeCurrentBoard.
     */
    public void resetBoard() {
        System.arraycopy(realBoard, 0, game.board, 0, 16);
        game.binaryBoard = realBinaryBoard;
    }

    /**
     * Resets the current board to the last stored one, using storeCurrentBoard2.
     */
    public void resetBoard2() {
        System.arraycopy(realBoard2, 0, game.board, 0, 16);
        game.binaryBoard = realBinaryBoard2;
    }

    /**
     * Returns a random free square on the board.
     * @return
     */
    public byte randomMove() {
        byte pos = QTTTGame.random.next16();

        while (game.board[pos] != null) {
            pos = QTTTGame.random.next16();
        }

        return pos;
    }
}
