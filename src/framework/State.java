package framework;

public class State {
    public boolean isMine;
    public byte position;
    public byte time;

    public State(byte position, byte time, boolean meFirst) {
        this.position = position;
        this.time = time;

        if (meFirst) {
            isMine = time % 2 == 1;
        } else {
            isMine = time % 2 == 0;
        }
    }

    public void switchOwner() {
        isMine = !isMine;
    }
}
