import java.awt.GraphicsEnvironment;

public class ListFonts {
    public static void main(String[] args) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();

        for (String name : fontNames) {
            System.out.println(name);
        }
    }
}