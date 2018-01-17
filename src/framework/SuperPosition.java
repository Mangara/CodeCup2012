package framework;

public class SuperPosition extends State {

    public SuperPosition twin; // The other simultaneous move
    public SuperPosition parent; // Parent in the Union-Find data structure
    public byte rank; // Rank in the Union-Find data structure

    public SuperPosition(byte position, byte time, boolean meFirst) {
        super(position, time, meFirst);
        parent = null;
        rank = 0;
    }

    public SuperPosition(byte position, byte time, boolean meFirst, SuperPosition parent, byte rank) {
        super(position, time, meFirst);
        this.parent = parent;
        this.rank = rank;
    }

    /**
     * Finds the representative of the connected component this move is part of.
     * Also compresses the search path by linking all intermediate nodes directly to the representative.
     * @return
     */
    public SuperPosition getConnectedComponent() {
        //System.err.println("getConnectedComponent of move " + (isMine ? "M" + time : "H" + time) + " at position " + position);

        if (parent == null) {
            // This node is the representative
            return this;
        } else {
            parent = parent.getConnectedComponent();
            return parent;
        }
    }

    /**
     * Merges the two connected components represented by a and b.
     * Precondition: a and b are the representatives of their connected components, i.e. a.parent == null && b.parent == null.
     * Precondition: a != b.
     * @param a
     * @param b
     * @return the representative of the new component
     */
    public static SuperPosition mergeConnectedComponents(SuperPosition a, SuperPosition b) {
        if (a.rank < b.rank) {
            a.parent = b;
            return b;
        } else if (b.rank < a.rank) {
            b.parent = a;
            return a;
        } else {
            b.parent = a;
            a.rank++;
            return a;
        }
    }
}
