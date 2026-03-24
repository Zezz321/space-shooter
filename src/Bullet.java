import java.awt.*;

public class Bullet {

    public static final int WIDTH = 6;
    public static final int HEIGHT = 16;

    private int x, y;
    private int speed;
    private boolean isPlayer;

    public Bullet(int x, int y, int speed, boolean isPlayer) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.isPlayer = isPlayer;
    }

    public void update() { y += speed; }

    public void draw(Graphics2D g) {
        if (isPlayer) {
            g.setColor(new Color(0, 255, 255, 80));
            g.fillRoundRect(x - 3, y - 4, WIDTH + 6, HEIGHT + 8, 6, 6);
            g.setColor(new Color(0, 255, 255));
            g.fillRoundRect(x, y, WIDTH, HEIGHT, 4, 4);
        } else {
            g.setColor(new Color(255, 50, 50, 80));
            g.fillRoundRect(x - 3, y - 4, WIDTH + 6, HEIGHT + 8, 6, 6);
            g.setColor(new Color(255, 100, 50));
            g.fillRoundRect(x, y, WIDTH, HEIGHT, 4, 4);
        }
    }

    public boolean isOffScreen() { return y < -HEIGHT || y > GameWindow.HEIGHT + HEIGHT; }
    public Rectangle getBounds() { return new Rectangle(x, y, WIDTH, HEIGHT); }
    public boolean isPlayerBullet() { return isPlayer; }
    public int getX() { return x; }
    public int getY() { return y; }
}