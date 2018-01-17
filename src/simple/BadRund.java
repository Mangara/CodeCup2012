/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package simple;

import framework.QTTTGame;
import java.io.IOException;
import montecarlo.MCPlayer;

/**
 *
 * @author Sander
 */
public class BadRund extends MCPlayer {

    public static void main(String[] args) throws IOException {
        new BadRund();
    }

    public BadRund() throws IOException {
        System.err.println("R BadRund");
        game.run(this);
    }

    @Override
    public byte[] selectFirstMove() {
        return selectMove();
    }

    @Override
    public byte[] selectMove() {
        // Make a random move
        byte move1 = randomMove();
        byte move2 = randomMove();

        while (move1 == move2 && game.currentTurn < 16) {
            move2 = randomMove();
        }

        return new byte[] {move1, move2};
    }

    @Override
    public byte selectCollapse(byte move1, byte move2) {
        // Select one randomly
        if (QTTTGame.random.nextBool()) {
            return move1;
        } else {
            return move2;
        }
    }
}
