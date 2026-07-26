package application.datastructures;

public class TrieNode {

    public boolean isEnd;
    public TrieNode[] childs = new TrieNode[26];

    public TrieNode(boolean isEnd) {
        this.isEnd = isEnd;
    }

}
