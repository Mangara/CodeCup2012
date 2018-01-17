package framework;

import java.util.ArrayList;
import java.util.Collection;

public class SuperPositionList extends ArrayList<SuperPosition> {

    public SuperPositionList() {
    }

    public SuperPositionList(Collection<? extends SuperPosition> c) {
        super(c);
    }

    public SuperPosition getFirst() {
        if (isEmpty()) {
            return null;
        } else {
            return get(0);
        }
    }

    public SuperPosition getLast() {
        if (isEmpty()) {
            return null;
        } else {
            return get(size() - 1);
        }
    }

    public SuperPosition getConnectedComponent() {
        if (isEmpty()) {
            return null;
        } else {
            return get(0).getConnectedComponent();
        }
    }
}
