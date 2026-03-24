import java.awt.*;

public class Player {

    public static final int WIDTH = 40;
    public static final int HEIGHT = 40;
    private static final int SPEED = 5;
    private static final int SHOOT_COOLDOWN = 15;

    private int x, y;
    private boolean movingLeft, movingRight, movingUp, movingDown;
    private int shootTimer = 0;
    private int lives = 3;
    private boolean invincible = false;
    private int invincibleTimer = 0;

    public Player(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public void update() {
        if (movingLeft  && x > 0) x -= SPEED;
        if (movingRight && x < GameWindow.WIDTH - WIDTH) x += SPEED;
        if (movingUp    && y > 0) y -= SPEED;
        if (movingDown  && y < GameWindow.HEIGHT - HEIGHT) y += SPEED;

        if (shootTimer > 0) shootTimer--;

        if (invincible) {
            invincibleTimer--;
            if (invincibleTimer <= 0) invincible = false;
        }
    }

    public void draw(Graphics2D g) {
        if (invincible && (invincibleTimer / 5) % 2 == 0) return;

        g.setColor(new Color(0, 200, 255, 120));
        g.fillOval(x + 10, y + HEIGHT - 10, 20, 20);

        int[] xPoints = {x + WIDTH / 2, x, x + WIDTH};
        int[] yPoints = {y, y + HEIGHT, y + HEIGHT};
        g.setColor(new Color(0, 220, 255));
        g.fillPolygon(xPoints, yPoints, 3);

        int[] hxPoints = {x + WIDTH / 2, x + 10, x + WIDTH - 10};
        int[] hyPoints = {y + 5, y + HEIGHT - 5, y + HEIGHT - 5};
        g.setColor(new Color(150, 240, 255));
        g.fillPolygon(hxPoints, hyPoints, 3);

        g.setColor(new Color(0, 100, 200));
        g.fillOval(x + WIDTH / 2 - 8, y + 10, 16, 16);
    }

    public boolean canShoot() { return shootTimer == 0; }
    public void shoot() { shootTimer = SHOOT_COOLDOWN; }

    public Bullet createBullet() {
        return new Bullet(x + WIDTH / 2 - Bullet.WIDTH / 2, y, -12, true);
    }

    public void hit() {
        if (!invincible) {
            lives--;
            invincible = true;
            invincibleTimer = 120;
        }
    }

    public Rectangle getBounds() { return new Rectangle(x + 5, y + 5, WIDTH - 10, HEIGHT - 10); }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getLives() { return lives; }
    public boolean isInvincible() { return invincible; }
    public void setMovingLeft(boolean b)  { movingLeft  = b; }
    public void setMovingRight(boolean b) { movingRight = b; }
    public void setMovingUp(boolean b)    { movingUp    = b; }
    public void setMovingDown(boolean b)  { movingDown  = b; }
}