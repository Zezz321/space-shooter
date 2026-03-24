import java.awt.*;
import java.util.Random;

public class Enemy {

    public static final int WIDTH = 36;
    public static final int HEIGHT = 36;
    private static final Random RAND = new Random();

    public enum Type { BASIC, FAST, TANK }

    private int x, y;
    private float speedX, speedY;
    private int health, maxHealth;
    private Type type;
    private int shootTimer, shootCooldown;
    private Color primaryColor, accentColor;

    public Enemy(int x, int y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;

        switch (type) {
            case BASIC:
                health = maxHealth = 1; speedY = 2;
                speedX = RAND.nextFloat() * 2 - 1;
                shootCooldown = 80 + RAND.nextInt(40);
                primaryColor = new Color(255, 80, 80);
                accentColor  = new Color(255, 160, 80);
                break;
            case FAST:
                health = maxHealth = 1; speedY = 4;
                speedX = RAND.nextFloat() * 3 - 1.5f;
                shootCooldown = 120;
                primaryColor = new Color(255, 200, 0);
                accentColor  = new Color(255, 255, 100);
                break;
            case TANK:
                health = maxHealth = 3; speedY = 1;
                speedX = RAND.nextFloat() - 0.5f;
                shootCooldown = 60 + RAND.nextInt(30);
                primaryColor = new Color(180, 50, 200);
                accentColor  = new Color(220, 120, 255);
                break;
        }
        shootTimer = RAND.nextInt(shootCooldown);
    }

    public void update() {
        x += speedX;
        y += speedY;
        if (x < 0 || x > GameWindow.WIDTH - WIDTH) speedX = -speedX;
        if (shootTimer > 0) shootTimer--;
    }

    public void draw(Graphics2D g) {
        g.setColor(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), 60));
        g.fillOval(x - 4, y - 4, WIDTH + 8, HEIGHT + 8);

        int[] xp = {x + WIDTH / 2, x, x + WIDTH};
        int[] yp = {y + HEIGHT, y, y};
        g.setColor(primaryColor);
        g.fillPolygon(xp, yp, 3);

        int[] hx = {x + WIDTH / 2, x + 8, x + WIDTH - 8};
        int[] hy = {y + HEIGHT - 5, y + 5, y + 5};
        g.setColor(accentColor);
        g.fillPolygon(hx, hy, 3);

        g.setColor(Color.WHITE);
        g.fillOval(x + WIDTH / 2 - 6, y + HEIGHT / 2 - 2, 12, 12);
        g.setColor(primaryColor.darker());
        g.fillOval(x + WIDTH / 2 - 4, y + HEIGHT / 2, 8, 8);

        if (type == Type.TANK) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x, y - 8, WIDTH, 4);
            g.setColor(new Color(100, 255, 100));
            g.fillRect(x, y - 8, (int)(WIDTH * (float) health / maxHealth), 4);
        }
    }

    public boolean canShoot() { return shootTimer == 0; }
    public void resetShootTimer() { shootTimer = shootCooldown; }
    public Bullet createBullet() { return new Bullet(x + WIDTH / 2 - Bullet.WIDTH / 2, y + HEIGHT, 5, false); }
    public void hit() { health--; }
    public boolean isDead() { return health <= 0; }
    public boolean isOffScreen() { return y > GameWindow.HEIGHT + HEIGHT; }
    public Rectangle getBounds() { return new Rectangle(x + 4, y + 4, WIDTH - 8, HEIGHT - 8); }
    public int getX() { return x; }
    public int getY() { return y; }
    public Type getType() { return type; }
    public int getScore() {
        switch (type) {
            case FAST: return 200;
            case TANK: return 300;
            default:   return 100;
        }
    }
}