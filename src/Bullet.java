import java.awt.*;

public class Bullet {

    public static final int WIDTH = 6;
    public static final int HEIGHT = 16;

    private int x, y;
    private int speed;
    private boolean isPlayer;

    /**
     * Constructor for bullet
     * @param x starting X position
     * @param y starting Y position
     * @param speed movement speed (positive = down, negative = up)
     * @param isPlayer true for player bullets, false for enemy bullets
     */
    public Bullet(int x, int y, int speed, boolean isPlayer) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.isPlayer = isPlayer;
    }

    /**
     * Updates bullet position each frame
     */
    public void update() { y += speed; }

    /**
     * Renders bullet with glow effect
     * Player bullets are cyan, enemy bullets are orange-red
     */
    public void draw(Graphics2D g) {
        if (isPlayer) {
            // Player bullet glow and body
            g.setColor(new Color(0, 255, 255, 80));
            g.fillRoundRect(x - 3, y - 4, WIDTH + 6, HEIGHT + 8, 6, 6);
            g.setColor(new Color(0, 255, 255));
            g.fillRoundRect(x, y, WIDTH, HEIGHT, 4, 4);
        } else {
            // Enemy bullet glow and body
            g.setColor(new Color(255, 50, 50, 80));
            g.fillRoundRect(x - 3, y - 4, WIDTH + 6, HEIGHT + 8, 6, 6);
            g.setColor(new Color(255, 100, 50));
            g.fillRoundRect(x, y, WIDTH, HEIGHT, 4, 4);
        }
    }

    /**
     * Checks if bullet is outside screen boundaries
     */
    public boolean isOffScreen() { return y < -HEIGHT || y > GameWindow.HEIGHT + HEIGHT; }

    public Rectangle getBounds() { return new Rectangle(x, y, WIDTH, HEIGHT); }
    public boolean isPlayerBullet() { return isPlayer; }
    public int getX() { return x; }
    public int getY() { return y; }
}