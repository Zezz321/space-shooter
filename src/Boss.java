import java.awt.*;

public class Boss {
    public static final int WIDTH = 80;
    public static final int HEIGHT = 60;

    private int x, y;
    private float speedX = 2;
    private int health, maxHealth;
    private int shootTimer = 0;
    private int shootCooldown = 40;
    private int wave;
    private boolean isFinalBoss;

    /**
     * Constructor - creates boss with stats based on wave number
     * Wave 15 is the final boss with increased difficulty
     */
    public Boss(int wave) {
        this.wave = wave;
        this.isFinalBoss = (wave == 15);
        this.x = GameWindow.WIDTH / 2 - WIDTH / 2;
        this.y = 60;

        if (isFinalBoss) {
            // Final boss has much more health and shoots faster
            maxHealth = health = 100;
            shootCooldown = 25;
            speedX = 3;
        } else {
            // Regular boss health scales with wave
            maxHealth = health = 10 + (wave / 5) * 8;
        }
    }

    /**
     * Updates boss position and shoot cooldown
     * Boss bounces horizontally between screen edges
     */
    public void update() {
        x += speedX;
        if (x < 0 || x > GameWindow.WIDTH - WIDTH) speedX = -speedX;
        if (shootTimer > 0) shootTimer--;
    }

    /**
     * Renders the boss with visual effects
     * Final boss has red/pink color scheme vs purple for regular
     */
    public void draw(Graphics2D g) {
        // Glow effect - different color for final boss
        if (isFinalBoss) {
            g.setColor(new Color(255, 0, 100, 80));
        } else {
            g.setColor(new Color(200, 0, 255, 50));
        }
        g.fillOval(x - 8, y - 8, WIDTH + 16, HEIGHT + 16);

        // Main body shape (hexagonal/arrow-like)
        int[] xp = {x + WIDTH/2, x, x + 10, x + WIDTH/2, x + WIDTH - 10, x + WIDTH};
        int[] yp = {y, y + HEIGHT/2, y + HEIGHT, y + HEIGHT - 10, y + HEIGHT, y + HEIGHT/2};

        if (isFinalBoss) {
            g.setColor(new Color(255, 50, 100));
        } else {
            g.setColor(new Color(160, 0, 220));
        }
        g.fillPolygon(xp, yp, 6);

        // Highlight area on boss body
        if (isFinalBoss) {
            g.setColor(new Color(255, 150, 200));
        } else {
            g.setColor(new Color(220, 100, 255));
        }
        g.fillOval(x + WIDTH/2 - 15, y + 8, 30, 25);

        // Core (eye) of the boss
        if (isFinalBoss) {
            g.setColor(new Color(255, 200, 100));
            g.fillOval(x + WIDTH/2 - 10, y + 12, 20, 20);
            g.setColor(new Color(255, 50, 50));
            g.fillOval(x + WIDTH/2 - 6, y + 16, 12, 12);
        } else {
            g.setColor(Color.WHITE);
            g.fillOval(x + WIDTH/2 - 10, y + 12, 20, 20);
            g.setColor(new Color(255, 0, 200));
            g.fillOval(x + WIDTH/2 - 6, y + 16, 12, 12);
        }

        // Health bar above boss
        g.setColor(new Color(60, 60, 60));
        g.fillRect(x, y - 14, WIDTH, 7);
        float ratio = (float) health / maxHealth;
        // Color changes based on remaining health
        g.setColor(ratio > 0.5f ? new Color(100, 255, 100) : ratio > 0.25f ? new Color(255, 200, 0) : new Color(255, 60, 60));
        g.fillRect(x, y - 14, (int)(WIDTH * ratio), 7);

        // Label - different text for final boss
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        if (isFinalBoss) {
            g.drawString("FINAL BOSS", x + WIDTH/2 - 28, y - 18);
        } else {
            g.drawString("BOSS", x + WIDTH/2 - 16, y - 18);
        }
    }

    public boolean canShoot() { return shootTimer == 0; }
    public void resetShootTimer() { shootTimer = shootCooldown; }

    /**
     * Creates three bullets in a spread pattern
     */
    public Bullet[] createBullets() {
        return new Bullet[]{
                new Bullet(x + WIDTH/2 - Bullet.WIDTH/2, y + HEIGHT, 5, false),
                new Bullet(x + WIDTH/2 - Bullet.WIDTH/2 - 20, y + HEIGHT, 5, false),
                new Bullet(x + WIDTH/2 - Bullet.WIDTH/2 + 20, y + HEIGHT, 5, false)
        };
    }

    public void hit(int dmg) { health -= dmg; }
    public boolean isDead()  { return health <= 0; }
    public int getScore()    { return 1000 + (wave / 5) * 500; }
    public Rectangle getBounds() { return new Rectangle(x + 6, y + 6, WIDTH - 12, HEIGHT - 12); }
    public int getX() { return x; }
    public int getY() { return y; }
}