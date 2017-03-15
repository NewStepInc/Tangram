package com.olimex.tangram.engine;

import java.util.ArrayList;

public class RightPosition {
    private float rightX;
    private float rightY;
    private ArrayList<Integer> aryIds = new ArrayList<Integer>();

    public float getRightX() {
        return rightX;
    }

    public RightPosition setRightX(float rightX) {
        this.rightX = rightX;
        return this;
    }

    public float getRightY() {
        return rightY;
    }

    public RightPosition setRightY(float rightY) {
        this.rightY = rightY;
        return this;
    }

    public RightPosition addID(int id) {
        aryIds.add(id);
        return this;
    }

    public boolean hasID(int id) {
        return aryIds.contains(id);
    }

    public int getFirstID() {
        if (aryIds.size() > 0)
            return aryIds.get(0);

        return -1;
    }
}
