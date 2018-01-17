package simple;

import framework.QTTTPlayer;
import java.io.IOException;

public class BadEend extends QTTTPlayer {

    public static void main(String[] args) throws IOException {
        new BadEend();
    }

    public BadEend() throws IOException {
        game.run(this);
    }

    @Override
    public byte[] selectFirstMove() {
        return new byte[]{0, 1};
    }

    @Override
    public byte[] selectMove() {
        // Pick the first legal move
        byte move1 = -1, move2 = -1;

        for (byte i = 0; i < 16; i++) {
            if (game.board[i] == null) {
                move1 = i;
                break;
            }
        }

        for (byte i = (byte) (move1 + 1); i < 16; i++) {
            if (game.board[i] == null) {
                move2 = i;
                break;
            }
        }

        if (move2 == -1) {
            // Only 1 empty square left
            move2 = move1;
        }

        return new byte[] {move1, move2};
    }

    @Override
    public byte selectCollapse(byte move1, byte move2) {
        return move1;
    }
}
