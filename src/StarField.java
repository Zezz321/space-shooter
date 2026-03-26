import java.awt.*;
import java.util.Random;

public class StarField {

    private static final int STAR_COUNT = 120;
    private int[] x, y, size, speed, alpha;
    private static final Random RAND = new Random();

    /**
     * Initializes star positions with random properties
     */
    public StarField() {
        x = new int[STAR_COUNT]; y = new int[STAR_COUNT];
        size = new int[STAR_COUNT]; speed = new int[STAR_COUNT];
        alpha = new int[STAR_COUNT];
        for (int i = 0; i < STAR_COUNT; i++) {
            x[i] = RAND.nextInt(GameWindow.WIDTH);
            y[i] = RAND.nextInt(GameWindow.HEIGHT);
            size[i]  = 1 + RAND.nextInt(2);
            speed[i] = 1 + RAND.nextInt(2);
            alpha[i] = 80 + RAND.nextInt(175);
        }
    }

    /**
     * Updates star positions (scrolling effect)
     */
    public void update() {
        for (int i = 0; i < STAR_COUNT; i++) {
            y[i] += speed[i];
            if (y[i] > GameWindow.HEIGHT) { y[i] = 0; x[i] = RAND.nextInt(GameWindow.WIDTH); }
        }
    }

    /**
     * Renders stars with varying brightness
     */
    public void draw(Graphics2D g) {
        for (int i = 0; i < STAR_COUNT; i++) {
            g.setColor(new Color(255, 255, 255, alpha[i]));
            g.fillOval(x[i], y[i], size[i], size[i]);
        }
    }
}