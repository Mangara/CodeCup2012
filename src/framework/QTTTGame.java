package framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Sander
 */
public class QTTTGame {

    private static final boolean DEBUG_GAME = true;
    public static final XorShiftRandom random = new XorShiftRandom(System.nanoTime());
    // State variables
    public State[] board = new State[16];
    public int binaryBoard;
    public SuperPositionList[] superpositions = new SuperPositionList[16];
    public byte currentTurn;
    public boolean meFirst;
    // Pre-allocated SuperPositions for every move
    public SuperPosition[] moves1 = new SuperPosition[17];
    public SuperPosition[] moves2 = new SuperPosition[17];
    // Pre-allocated array for use as queue in collapse()
    private SuperPosition[] toCollapse = new SuperPosition[32];

    public QTTTGame() {
        initialize();
    }

    public final void initialize() {
        currentTurn = 1;
        meFirst = false;

        binaryBoard = 0;

        for (int i = 0; i < 16; i++) {
            board[i] = null;
            superpositions[i] = new SuperPositionList();

            moves1[i + 1] = new SuperPosition((byte) 0, (byte) (i + 1), meFirst);
            moves2[i + 1] = new SuperPosition((byte) 0, (byte) (i + 1), meFirst);
            moves1[i + 1].twin = moves2[i + 1];
            moves2[i + 1].twin = moves1[i + 1];
        }
    }

    public void run(QTTTPlayer player) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        String input = in.readLine();
        String output = "";

        while (input != null && !input.equals("Quit")) {
            if (DEBUG_GAME) {
                System.err.println("Input: " + input + " at turn " + currentTurn);
            }

            if (input.equals("Start")) {
                meFirst = true;

                // Switch owners of all pre-allocated moves
                for (int i = 1; i < 17; i++) {
                    moves1[i].switchOwner();
                    moves2[i].switchOwner();
                }

                // Print my first move
                byte[] myMove = player.selectFirstMove();

                move(myMove[0], myMove[1]);
                player.notifyMove(myMove[0], myMove[1]);

                output += Character.toString(getCharacter(myMove[0]));
                output += Character.toString(getCharacter(myMove[1]));

                if (DEBUG_GAME) {
                    System.err.println("Board after move " + Character.toString(getCharacter(myMove[0])) + Character.toString(getCharacter(myMove[1])));
                    printBoard();
                }
            } else {
                // Regular turn taking
                if (input.endsWith("!")) {
                    input = input.substring(0, input.length() - 1);
                }

                if (input.length() == 1 || input.length() == 3) {
                    // Oppenent made a decision about a collapse
                    byte position = getPosition(input.charAt(0));
                    input = input.substring(1);

                    collapse(position);
                    player.notifyCollapse(position);

                    if (DEBUG_GAME) {
                        System.err.println("Board after collapse of (" + (position / 4) + ", " + (position % 4) + ")");
                        printBoard();
                    }
                }

                if (input.length() > 0) {
                    byte move1 = getPosition(input.charAt(0));
                    byte move2 = getPosition(input.charAt(1));

                    boolean collapse = move(move1, move2);
                    player.notifyMove(move1, move2);

                    if (DEBUG_GAME) {
                        System.err.println("Board after move " + input);
                        printBoard();
                        System.err.println("cycle: " + collapse);
                    }

                    if (collapse) {
                        // Opponent's move created a cycle; decide how it will collapse
                        byte collapsePos = player.selectCollapse(move1, move2);

                        output += getCharacter(collapsePos);
                        collapse(collapsePos);
                        player.notifyCollapse(collapsePos);

                        if (DEBUG_GAME) {
                            System.err.println("Board after collapse of " + Character.toString(getCharacter(collapsePos)));
                            printBoard();
                        }
                    }

                    if (currentTurn < 17 && !gameOver()) {
                        // Pick a move
                        byte[] myMove = player.selectMove();

                        // Perform this move on the board
                        move(myMove[0], myMove[1]);
                        player.notifyMove(myMove[0], myMove[1]);

                        if (DEBUG_GAME) {
                            System.err.println("Board after move " + Character.toString(getCharacter(myMove[0])) + Character.toString(getCharacter(myMove[1])));
                            printBoard();
                        }

                        // Output the move
                        output += Character.toString(getCharacter(myMove[0]));
                        output += Character.toString(getCharacter(myMove[1]));
                    }
                }
            }

            System.out.println(output);
            System.out.flush();
            System.err.flush();

            output = "";
            input = in.readLine();
        }
    }

    public void printBoard() {
        String[] text = new String[16];

        for (int i = 0; i < 16; i++) {
            if (board[i] == null) {
                text[i] = "[";

                for (SuperPosition s : superpositions[i]) {
                    text[i] += (s.isMine ? "M" + s.time : "H" + s.time);
                    text[i] += ", ";
                }

                text[i] += "]";
                text[i] = text[i].replaceAll(", \\]", "]");
            } else {
                text[i] = (board[i].isMine ? "M" + board[i].time : "H" + board[i].time);
            }
        }

        System.err.println(String.format("%s | %s | %s | %s", text[0], text[1], text[2], text[3]));
        System.err.println(String.format("%s | %s | %s | %s", text[4], text[5], text[6], text[7]));
        System.err.println(String.format("%s | %s | %s | %s", text[8], text[9], text[10], text[11]));
        System.err.println(String.format("%s | %s | %s | %s", text[12], text[13], text[14], text[15]));
        System.err.println();
    }

    /**
     * Performs the given move on the current board.
     * Returns true if this move created a cycle, false otherwise.
     * @param pos1
     * @param pos2
     * @return
     */
    public boolean move(byte pos1, byte pos2) {
        boolean cycle = (pos1 == pos2);

        SuperPosition move1 = moves1[currentTurn];
        SuperPosition move2 = moves2[currentTurn];

        move1.position = pos1;
        move2.position = pos2;

        if (superpositions[pos1].isEmpty()) {
            if (superpositions[pos2].isEmpty()) {
                // Both squares are empty; make the first the parent
                move1.parent = null;
                move1.rank = 1;
                move2.parent = move1;
            } else {
                // Only the first square is empty; add to the component
                SuperPosition component = superpositions[pos2].getConnectedComponent();
                move1.parent = component;
                move2.parent = component;
            }
        } else {
            if (superpositions[pos2].isEmpty()) {
                // Only the second square is empty; add to the component
                SuperPosition component = superpositions[pos1].getConnectedComponent();
                move1.parent = component;
                move2.parent = component;
            } else {
                // Neither square is empty; see if this is a cycle
                SuperPosition component1 = superpositions[pos1].getConnectedComponent();
                SuperPosition component2 = superpositions[pos2].getConnectedComponent();

                if (component1 == component2) {
                    // Cycle
                    cycle = true;

                    // Add to the component
                    move1.parent = component1;
                    move2.parent = component1;
                } else {
                    // No cycle; merge these components
                    SuperPosition newComponent = SuperPosition.mergeConnectedComponents(component1, component2);
                    move1.parent = newComponent;
                    move2.parent = newComponent;
                }
            }
        }

        superpositions[pos1].add(move1);
        superpositions[pos2].add(move2);

        currentTurn++;

        return cycle;
    }

    /**
     * Collapse the connected component containing pos.
     * The last move that was made to pos collapses there.
     * @param pos
     */
    public void collapse(byte pos) {
        toCollapse[0] = superpositions[pos].getLast();
        int i = 0;
        int size = 1;

        while (i < size) {
            byte p = toCollapse[i].position;

            // Add the final move to the board
            board[p] = toCollapse[i];

            if (toCollapse[i].isMine) {
                binaryBoard |= (0x8000 >>> p);
            } else {
                binaryBoard |= (0x80000000 >>> p);
            }

            // All other moves here need to collapse to their twins
            for (int j = 0; j < superpositions[p].size(); j++) {
                SuperPosition move = superpositions[p].get(j);

                if (move != toCollapse[i] && board[move.twin.position] == null) {
                    toCollapse[size] = move.twin;
                    size++;
                }
            }

            i++;
        }
    }

    /**
     * Checks whether either player has 4 in a row.
     * @return
     */
    public boolean gameOver() {
        return // Horizontal
                (binaryBoard & 0xF) == 0xF || (binaryBoard & 0xF0) == 0xF0 || (binaryBoard & 0xF00) == 0xF00 || (binaryBoard & 0xF000) == 0xF000
                || (binaryBoard & 0xF0000) == 0xF0000 || (binaryBoard & 0xF00000) == 0xF00000 || (binaryBoard & 0xF000000) == 0xF000000 || (binaryBoard & 0xF0000000) == 0xF0000000
                // Vertical
                || (binaryBoard & 0x8888) == 0x8888 || (binaryBoard & 0x4444) == 0x4444 || (binaryBoard & 0x2222) == 0x2222 || (binaryBoard & 0x1111) == 0x1111
                || (binaryBoard & 0x88880000) == 0x88880000 || (binaryBoard & 0x44440000) == 0x44440000 || (binaryBoard & 0x22220000) == 0x22220000 || (binaryBoard & 0x11110000) == 0x11110000
                // Diagonal
                || (binaryBoard & 0x8421) == 0x8421 || (binaryBoard & 0x1248) == 0x1248
                || (binaryBoard & 0x84210000) == 0x84210000 || (binaryBoard & 0x12480000) == 0x12480000;
    }

    /**
     * Computes my score for this final board position.
     * @return 2 if I have 4 in a row and my opponent doesn't.
     * 1 if we both have 4 in a row, but mine was first.
     * 0 if noone has 4 in a row (draw).
     * -1 if we both have 4 in a row, but his was first.
     * -2 if he has 4 in a row and I don't.
     */
    public int getOutcome() {
        if (gameOver()) {
            int iWon = iWon();
            int heWon = heWon();

            if (iWon < heWon) {
                if (heWon == Integer.MAX_VALUE) {
                    return 2;
                } else {
                    return 1;
                }
            } else {
                if (iWon == Integer.MAX_VALUE) {
                    return -2;
                } else {
                    return -1;
                }
            }
        } else {
            // A draw
            return 0;
        }
    }

    /**
     * Computes my score for this final board position.
     * @return
     */
    public int computeMyScore() {
        int myBonus = myBonus();

        if (gameOver()) {
            int iWon = iWon();
            int heWon = heWon();

            if (iWon < heWon) {
                // I won! =)
                if (heWon == Integer.MAX_VALUE) {
                    return 20 + myBonus;
                } else {
                    return 15 + myBonus;
                }
            } else {
                // I lost =(
                if (iWon == Integer.MAX_VALUE) {
                    return myBonus;
                } else {
                    return 5 + myBonus;
                }
            }
        } else {
            // A draw
            return 10 + myBonus;
        }
    }

    /**
     * Computes my opponents score for this final board position
     * @return
     */
    public int computeHisScore() {
        int bonus = hisBonus();

        if (gameOver()) {
            int iWon = iWon();
            int heWon = heWon();

            if (iWon < heWon) {
                if (heWon == Integer.MAX_VALUE) {
                    return bonus;
                } else {
                    return 5 + bonus;
                }
            } else {
                if (iWon == Integer.MAX_VALUE) {
                    return 20 + bonus;
                } else {
                    return 15 + bonus;
                }
            }
        } else {
            // A draw
            return 10 + bonus;
        }
    }

    /**
     * Returns the turn that I won (the last turn that any move related in the first of my 4-in-a-rows was placed), or Integer.MAX_VALUE if I have no 4 in a row (or column or diagonal).
     * @return
     */
    public int iWon() {
        // Horizontal
        if ((binaryBoard & 0xF000) == 0xF000) {
            return Math.max(board[0].time, Math.max(board[1].time, Math.max(board[2].time, board[3].time)));
        }
        if ((binaryBoard & 0xF00) == 0xF00) {
            return Math.max(board[4].time, Math.max(board[5].time, Math.max(board[6].time, board[7].time)));
        }
        if ((binaryBoard & 0xF0) == 0xF0) {
            return Math.max(board[8].time, Math.max(board[9].time, Math.max(board[10].time, board[11].time)));
        }
        if ((binaryBoard & 0xF) == 0xF) {
            return Math.max(board[12].time, Math.max(board[13].time, Math.max(board[14].time, board[15].time)));
        }

        // Vertical
        if ((binaryBoard & 0x8888) == 0x8888) {
            return Math.max(board[0].time, Math.max(board[4].time, Math.max(board[8].time, board[12].time)));
        }
        if ((binaryBoard & 0x4444) == 0x4444) {
            return Math.max(board[1].time, Math.max(board[5].time, Math.max(board[9].time, board[13].time)));
        }
        if ((binaryBoard & 0x2222) == 0x2222) {
            return Math.max(board[2].time, Math.max(board[6].time, Math.max(board[10].time, board[14].time)));
        }
        if ((binaryBoard & 0x1111) == 0x1111) {
            return Math.max(board[3].time, Math.max(board[7].time, Math.max(board[11].time, board[15].time)));
        }

        // Diagonal
        if ((binaryBoard & 0x8421) == 0x8421) {
            return Math.max(board[0].time, Math.max(board[5].time, Math.max(board[10].time, board[15].time)));
        }
        if ((binaryBoard & 0x1248) == 0x1248) {
            return Math.max(board[3].time, Math.max(board[6].time, Math.max(board[9].time, board[12].time)));
        }

        return Integer.MAX_VALUE;
    }

    /**
     * Returns the turn that my opponent won (the last turn that any move related in the first of my 4-in-a-rows was placed), or Integer.MAX_VALUE if I have no 4 in a row (or column or diagonal).
     * @return
     */
    public int heWon() {
        // Horizontal
        if ((binaryBoard & 0xF0000000) == 0xF0000000) {
            return Math.max(board[0].time, Math.max(board[1].time, Math.max(board[2].time, board[3].time)));
        }
        if ((binaryBoard & 0xF000000) == 0xF000000) {
            return Math.max(board[4].time, Math.max(board[5].time, Math.max(board[6].time, board[7].time)));
        }
        if ((binaryBoard & 0xF00000) == 0xF00000) {
            return Math.max(board[8].time, Math.max(board[9].time, Math.max(board[10].time, board[11].time)));
        }
        if ((binaryBoard & 0xF0000) == 0xF0000) {
            return Math.max(board[12].time, Math.max(board[13].time, Math.max(board[14].time, board[15].time)));
        }

        // Vertical
        if ((binaryBoard & 0x88880000) == 0x88880000) {
            return Math.max(board[0].time, Math.max(board[4].time, Math.max(board[8].time, board[12].time)));
        }
        if ((binaryBoard & 0x44440000) == 0x44440000) {
            return Math.max(board[1].time, Math.max(board[5].time, Math.max(board[9].time, board[13].time)));
        }
        if ((binaryBoard & 0x22220000) == 0x22220000) {
            return Math.max(board[2].time, Math.max(board[6].time, Math.max(board[10].time, board[14].time)));
        }
        if ((binaryBoard & 0x11110000) == 0x11110000) {
            return Math.max(board[3].time, Math.max(board[7].time, Math.max(board[11].time, board[15].time)));
        }

        // Diagonal
        if ((binaryBoard & 0x84210000) == 0x84210000) {
            return Math.max(board[0].time, Math.max(board[5].time, Math.max(board[10].time, board[15].time)));
        }
        if ((binaryBoard & 0x12480000) == 0x12480000) {
            return Math.max(board[3].time, Math.max(board[6].time, Math.max(board[9].time, board[12].time)));
        }

        return Integer.MAX_VALUE;
    }

    /**
     * Returns the bonus points I receive from the given final board position.
     * @return
     */
    public int myBonus() {
        int bonus = 0;

        // Check for 2x2 squares
        if ((binaryBoard & 0xCC00) == 0xCC00) {
            bonus += 2;
        }
        if ((binaryBoard & 0x0CC0) == 0x0CC0) {
            bonus += 2;
        }
        if ((binaryBoard & 0x00CC) == 0x00CC) {
            bonus += 2;
        }
        if ((binaryBoard & 0x6600) == 0x6600) {
            bonus += 2;
        }
        if ((binaryBoard & 0x0660) == 0x0660) {
            bonus += 2;
        }
        if ((binaryBoard & 0x0066) == 0x0066) {
            bonus += 2;
        }
        if ((binaryBoard & 0x3300) == 0x3300) {
            bonus += 2;
        }
        if ((binaryBoard & 0x0330) == 0x0330) {
            bonus += 2;
        }
        if ((binaryBoard & 0x0033) == 0x0033) {
            bonus += 2;
        }

        // Check for rows of length 3
        if ((binaryBoard & 0xE000) == 0xE000) {
            bonus++;
        }
        if ((binaryBoard & 0x0E00) == 0x0E00) {
            bonus++;
        }
        if ((binaryBoard & 0x00E0) == 0x00E0) {
            bonus++;
        }
        if ((binaryBoard & 0x000E) == 0x000E) {
            bonus++;
        }
        if ((binaryBoard & 0x7000) == 0x7000) {
            bonus++;
        }
        if ((binaryBoard & 0x0700) == 0x0700) {
            bonus++;
        }
        if ((binaryBoard & 0x0070) == 0x0070) {
            bonus++;
        }
        if ((binaryBoard & 0x0007) == 0x0007) {
            bonus++;
        }

        // Check for columns of length 3
        if ((binaryBoard & 0x8880) == 0x8880) {
            bonus++;
        }
        if ((binaryBoard & 0x0888) == 0x0888) {
            bonus++;
        }
        if ((binaryBoard & 0x4440) == 0x4440) {
            bonus++;
        }
        if ((binaryBoard & 0x0444) == 0x0444) {
            bonus++;
        }
        if ((binaryBoard & 0x2220) == 0x2220) {
            bonus++;
        }
        if ((binaryBoard & 0x0222) == 0x0222) {
            bonus++;
        }
        if ((binaryBoard & 0x1110) == 0x1110) {
            bonus++;
        }
        if ((binaryBoard & 0x0111) == 0x0111) {
            bonus++;
        }

        // Check for diagonals of length 3
        // Down and to the right
        if ((binaryBoard & 0x8420) == 0x8420) {
            bonus++;
        }
        if ((binaryBoard & 0x0842) == 0x0842) {
            bonus++;
        }
        if ((binaryBoard & 0x4210) == 0x4210) {
            bonus++;
        }
        if ((binaryBoard & 0x0421) == 0x0421) {
            bonus++;
        }

        // Down and to the left
        if ((binaryBoard & 0x1240) == 0x1240) {
            bonus++;
        }
        if ((binaryBoard & 0x0124) == 0x0124) {
            bonus++;
        }
        if ((binaryBoard & 0x2480) == 0x2480) {
            bonus++;
        }
        if ((binaryBoard & 0x0248) == 0x0248) {
            bonus++;
        }

        return bonus;
    }

    /**
     * Returns the bonus points my opponent receives from the given final board position.
     * @return
     */
    public int hisBonus() {
        int bonus = 0;

        // Check for 2x2 squares
        if ((binaryBoard & 0xCC000000) == 0xCC000000) {
            bonus += 2;
        }
        if ((binaryBoard & 0x0CC00000) == 0x0CC00000) {
            bonus += 2;
        }
        if ((binaryBoard & 0x00CC0000) == 0x00CC0000) {
            bonus += 2;
        }
        if ((binaryBoard & 0x66000000) == 0x66000000) {
            bonus += 2;
        }
        if ((binaryBoard & 0x06600000) == 0x06600000) {
            bonus += 2;
        }
        if ((binaryBoard & 0x00660000) == 0x00660000) {
            bonus += 2;
        }
        if ((binaryBoard & 0x33000000) == 0x33000000) {
            bonus += 2;
        }
        if ((binaryBoard & 0x03300000) == 0x03300000) {
            bonus += 2;
        }
        if ((binaryBoard & 0x00330000) == 0x00330000) {
            bonus += 2;
        }

        // Check for rows of length 3
        if ((binaryBoard & 0xE0000000) == 0xE0000000) {
            bonus++;
        }
        if ((binaryBoard & 0x0E000000) == 0x0E000000) {
            bonus++;
        }
        if ((binaryBoard & 0x00E00000) == 0x00E00000) {
            bonus++;
        }
        if ((binaryBoard & 0x000E0000) == 0x000E0000) {
            bonus++;
        }
        if ((binaryBoard & 0x70000000) == 0x70000000) {
            bonus++;
        }
        if ((binaryBoard & 0x07000000) == 0x07000000) {
            bonus++;
        }
        if ((binaryBoard & 0x00700000) == 0x00700000) {
            bonus++;
        }
        if ((binaryBoard & 0x00070000) == 0x00070000) {
            bonus++;
        }

        // Check for columns of length 3
        if ((binaryBoard & 0x88800000) == 0x88800000) {
            bonus++;
        }
        if ((binaryBoard & 0x08880000) == 0x08880000) {
            bonus++;
        }
        if ((binaryBoard & 0x44400000) == 0x44400000) {
            bonus++;
        }
        if ((binaryBoard & 0x04440000) == 0x04440000) {
            bonus++;
        }
        if ((binaryBoard & 0x22200000) == 0x22200000) {
            bonus++;
        }
        if ((binaryBoard & 0x02220000) == 0x02220000) {
            bonus++;
        }
        if ((binaryBoard & 0x11100000) == 0x11100000) {
            bonus++;
        }
        if ((binaryBoard & 0x01110000) == 0x01110000) {
            bonus++;
        }

        // Check for diagonals of length 3
        // Down and to the right
        if ((binaryBoard & 0x84200000) == 0x84200000) {
            bonus++;
        }
        if ((binaryBoard & 0x08420000) == 0x08420000) {
            bonus++;
        }
        if ((binaryBoard & 0x42100000) == 0x42100000) {
            bonus++;
        }
        if ((binaryBoard & 0x04210000) == 0x04210000) {
            bonus++;
        }

        // Down and to the left
        if ((binaryBoard & 0x12400000) == 0x12400000) {
            bonus++;
        }
        if ((binaryBoard & 0x01240000) == 0x01240000) {
            bonus++;
        }
        if ((binaryBoard & 0x24800000) == 0x24800000) {
            bonus++;
        }
        if ((binaryBoard & 0x02480000) == 0x02480000) {
            bonus++;
        }

        return bonus;
    }

    /**
     * Translates capital letters used for moves to the board position.
     * @param move
     * @return
     */
    public byte getPosition(char move) {
        return (byte) (move - 'A');
    }

    /**
     * Reverse of getPosition
     * @param pos
     * @return
     */
    public char getCharacter(byte pos) {
        return (char) ('A' + pos);
    }
}
