package com.entities;

/**
 *
 * @author evan.gabb
 */
public enum Team {
    RED(0), 
    BLUE(2);
    
    private final int rowOffset;
    
    Team(int rowOffset) {
        this.rowOffset = rowOffset;
    }
    
    public int getRowOffset() {
        return rowOffset;
    }
}
