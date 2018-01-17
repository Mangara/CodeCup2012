/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package framework;

/**
 *
 * @author Sander
 */
public abstract class QTTTPlayer {
    protected QTTTGame game;

    public QTTTPlayer() {
        game = new QTTTGame();
    }

    public abstract byte[] selectFirstMove();
    public abstract byte[] selectMove();
    public abstract byte selectCollapse(byte move1, byte move2);
    
    /**
     * Called every time directly after a move is actually made in the game.
     * Default implementation is empty. Subclasses can override it to keep track of game state.
     * @param move1
     * @param move2
     */
    public void notifyMove(byte move1, byte move2) {

    }

    /**
     * Called every time directly after a collapse actually happens in the game.
     * Default implementation is empty. Subclasses can override it to keep track of game state.
     * @param collapse
     */
    public void notifyCollapse(byte collapse) {

    }
}
