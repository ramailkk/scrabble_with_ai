package application.model;

import java.util.ArrayList;

public class Move {
    public ArrayList<Tile> tiles;
    public ArrayList<Integer> positions;
    public ArrayList<ArrayList<Integer>> everyPositions;
    public ArrayList<String> everyWords;
    public int score;
    
    public void setEveryWords(ArrayList<String> everyWords) {
        this.everyWords = everyWords;
    }

    public void setEveryPositions(ArrayList<ArrayList<Integer>> everyPositions) {
        this.everyPositions = everyPositions;
    }

    public Move(ArrayList<Tile> ts, ArrayList<Integer> poses, int s) {
        tiles = ts;
        positions = poses;
        score = s;
    }
}
