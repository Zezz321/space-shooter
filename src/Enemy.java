import java.awt.*;
import java.util.Random;

public class Enemy {
    public static final int WIDTH = 36;
    public static final int HEIGHT = 36;
    private static final Random RAND = new Random();

    public enum Type { BASIC, FAST, TANK, ZIGZAG, SHOOTER }

    private int x, y;
    private float speedX, speedY;
    private int health, maxHealth;
    private Type type;
    private int shootTimer, shootCooldown;
    private Color primaryColor, accentColor;
    private int zigzagTimer = 0;

    /**
     * Enemy constructor with difficulty scaling based on wave
     * First 5 waves are easier, difficulty increases gradually
     */
    public Enemy(int x, int y, Type type, int wave) {
        this.x = x; this.y = y; this.type = type;

        // Difficulty scaling - first 5 waves are significantly easier
        float waveScale;
        if (wave <= 3) {
            waveScale = 0.5f; // Waves 1-3: 50% speed
        } else if (wave <= 5) {
            waveScale = 0.7f; // Waves 4-5: 70% speed
        } else {
            waveScale = 1 + (wave - 5) * 0.08f; // Gradual increase after wave 5
            waveScale = Math.min(waveScale, 2.2f); // Cap at 2.2x
        }

        switch (type) {
            case BASIC:
                health = maxHealth = 1;
                speedY = Math.min(2.0f * waveScale, 5f);
                speedX = RAND.nextFloat() * 1.5f - 0.75f;
                shootCooldown = Math.max(50, 100 - wave * 2);
                primaryColor = new Color(255, 80, 80);
                accentColor  = new Color(255, 160, 80);
                break;
            case FAST:
                health = maxHealth = 1;
                speedY = Math.min(3.5f * waveScale, 7f);
                speedX = RAND.nextFloat() * 2f - 1f;
                shootCooldown = 150;
                primaryColor = new Color(255, 200, 0);
                accentColor  = new Color(255, 255, 100);
                break;
            case TANK:
                health = maxHealth = Math.max(1, 1 + wave / 4);
                speedY = Math.min(1.0f * waveScale, 3.5f);
                speedX = RAND.nextFloat() * 1f - 0.5f;
                shootCooldown = Math.max(40, 80 - wave);
                primaryColor = new Color(180, 50, 200);
                accentColor  = new Color(220, 120, 255);
                break;
            case ZIGZAG:
                health = maxHealth = 1;
                speedY = Math.min(2.2f * waveScale, 6f);
                speedX = 2 + wave * 0.1f;
                shootCooldown = 999;
                primaryColor = new Color(0, 200, 100);
                accentColor  = new Color(100, 255, 160);
                break;
            case SHOOTER:
                health = maxHealth = Math.max(1, 1 + wave / 5);
                speedY = 0.2f; // VERY SLOW - stays near top
                speedX = 1f;
                shootCooldown = Math.max(35, 70 - wave);
                primaryColor = new Color(255, 120, 0);
                accentColor  = new Color(255, 200, 80);
                break;
        }
        shootTimer = RAND.nextInt(shootCooldown);
    }

    /**
     * Updates enemy position and behavior
     */
    public void update() {
        if (type == Type.ZIGZAG) {
            zigzagTimer++;
            if (zigzagTimer % 30 == 0) speedX = -speedX;
        }

        x += speedX;
        y += speedY;

        if (x < 0 || x > GameWindow.WIDTH - WIDTH) speedX = -speedX;

        // SHOOTER stops after reaching y > 120 (stays near top)
        if (type == Type.SHOOTER && y > 120) {
            speedY = 0;
        }

        if (shootTimer > 0) shootTimer--;
    }

    /**
     * Renders enemy with glow and unique shape per type
     */
    public void draw(Graphics2D g) {
        // Glow effect
        g.setColor(new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), 60));
        g.fillOval(x - 4, y - 4, WIDTH + 8, HEIGHT + 8);

        // Main triangular body
        int[] xp = {x + WIDTH/2, x, x + WIDTH};
        int[] yp = {y + HEIGHT, y, y};
        g.setColor(primaryColor);
        g.fillPolygon(xp, yp, 3);

        // Highlight area
        int[] hx = {x + WIDTH/2, x + 8, x + WIDTH - 8};
        int[] hy = {y + HEIGHT - 5, y + 5, y + 5};
        g.setColor(accentColor);
        g.fillPolygon(hx, hy, 3);

        // Eye
        g.setColor(Color.WHITE);
        g.fillOval(x + WIDTH/2 - 6, y + HEIGHT/2 - 2, 12, 12);
        g.setColor(primaryColor.darker());
        g.fillOval(x + WIDTH/2 - 4, y + HEIGHT/2, 8, 8);

        // Health bar for tank and shooter enemies
        if (type == Type.TANK || type == Type.SHOOTER) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(x, y - 8, WIDTH, 4);
            g.setColor(new Color(100, 255, 100));
            g.fillRect(x, y - 8, (int)(WIDTH * (float) health / maxHealth), 4);
        }
    }

    public boolean canShoot()      { return shootTimer == 0; }
    public void resetShootTimer()  { shootTimer = shootCooldown; }
    public Bullet createBullet()   { return new Bullet(x + WIDTH/2 - Bullet.WIDTH/2, y + HEIGHT, 5, false); }
    public void hit()              { health--; }
    public boolean isDead()        { return health <= 0; }
    public boolean isOffScreen()   { return y > GameWindow.HEIGHT + HEIGHT; }
    public Rectangle getBounds()   { return new Rectangle(x + 4, y + 4, WIDTH - 8, HEIGHT - 8); }
    public int getX()  { return x; }
    public int getY()  { return y; }
    public Type getType() { return type; }

    /**
     * Returns score value based on enemy type
     */
    public int getScore() {
        switch (type) {
            case FAST:    return 200;
            case TANK:    return 300;
            case ZIGZAG:  return 250;
            case SHOOTER: return 350;
            default:      return 100;
        }
    }
}