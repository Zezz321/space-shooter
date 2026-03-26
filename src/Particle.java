import java.awt.*;
import java.util.Random;

public class Particle {

    private float x, y, vx, vy;
    private int life, maxLife;
    private Color color;
    private float size;
    private static final Random RAND = new Random();

    /**
     * Constructor for explosion particle effect
     * @param x starting X position
     * @param y starting Y position
     * @param color particle color
     */
    public Particle(int x, int y, Color color) {
        this.x = x; this.y = y; this.color = color;
        // Random direction and speed
        float angle = RAND.nextFloat() * (float)(Math.PI * 2);
        float speed = 1 + RAND.nextFloat() * 4;
        vx = (float) Math.cos(angle) * speed;
        vy = (float) Math.sin(angle) * speed;
        maxLife = life = 30 + RAND.nextInt(20);
        size = 3 + RAND.nextFloat() * 4;
    }

    /**
     * Updates particle position - applies gravity effect
     */
    public void update() {
        x += vx; y += vy;
        vy += 0.1f; // Gravity
        life--;
    }

    /**
     * Renders particle with fading alpha based on remaining life
     */
    public void draw(Graphics2D g) {
        float alpha = (float) life / maxLife;
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 220)));
        int s = (int)(size * alpha);
        g.fillOval((int)x - s/2, (int)y - s/2, s, s);
    }

    public boolean isDead() { return life <= 0; }
}