package application.model;

import java.util.ArrayList;

public class BoardCell implements Cloneable {

    public Tile tile;
    public boolean isOccupied;
    public int speciality;
    public boolean[] crossChecks = new boolean[26];
    public ArrayList<Integer> anchors = new ArrayList<>();

    public BoardCell(int sp) {
        speciality = sp;
        isOccupied = false;
        tile = null;
        for(int i = 0; i < 26; i++) {
            crossChecks[i] = true;
        }
    }

    public void setTileOnCell(Tile tile) {
        this.tile = tile;
        isOccupied = true;
    }

    public void unsetTileOnCell() {
        tile = null;
        isOccupied = false;
    }

    @Override
    public Object clone() {
        try {
            BoardCell cloned = (BoardCell)super.clone();
            cloned.anchors = new ArrayList<>(this.anchors);
            cloned.crossChecks = new boolean[26];
            for(int i = 0; i < 26; i++) {
                cloned.crossChecks[i] = true;
            }
            if(tile != null) {
                cloned.tile = (Tile)tile.clone();
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
