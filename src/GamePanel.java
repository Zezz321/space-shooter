import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements KeyListener {

    private static final int FPS = 60;
    private static final int TARGET_DELAY = 1000 / FPS;

    private enum GameState { MENU, PLAYING, GAME_OVER }
    private GameState state = GameState.MENU;

    private Player player;
    private List<Enemy> enemies = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();
    private StarField starField;

    private int score = 0, highScore = 0, wave = 1;
    private int enemySpawnTimer = 0, waveTimer = 0;
    private int enemiesKilledThisWave = 0, enemiesPerWave = 8;
    private boolean keyLeft, keyRight, keyUp, keyDown, keyShoot;
    private final Random rand = new Random();

    private Font titleFont, uiFont, smallFont;

    public GamePanel() {
        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        starField = new StarField();
        titleFont = new Font("Monospaced", Font.BOLD, 52);
        uiFont    = new Font("Monospaced", Font.BOLD, 18);
        smallFont = new Font("Monospaced", Font.PLAIN, 14);
    }

    public void startGame() {
        new Thread(() -> {
            while (true) {
                long start = System.currentTimeMillis();
                update();
                repaint();
                long elapsed = System.currentTimeMillis() - start;
                long sleep = TARGET_DELAY - elapsed;
                if (sleep > 0) try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    private void initGame() {
        player = new Player(GameWindow.WIDTH / 2 - Player.WIDTH / 2, GameWindow.HEIGHT - 80);
        enemies.clear(); bullets.clear(); particles.clear();
        score = 0; wave = 1; enemiesKilledThisWave = 0;
        enemiesPerWave = 8; enemySpawnTimer = 0; waveTimer = 0;
        state = GameState.PLAYING;
    }

    private void update() {
        starField.update();
        if (state != GameState.PLAYING) return;

        player.setMovingLeft(keyLeft); player.setMovingRight(keyRight);
        player.setMovingUp(keyUp);     player.setMovingDown(keyDown);
        player.update();

        if (keyShoot && player.canShoot()) { bullets.add(player.createBullet()); player.shoot(); }

        enemySpawnTimer++;
        int spawnRate = Math.max(20, 60 - wave * 3);
        if (enemySpawnTimer >= spawnRate && enemiesKilledThisWave + enemies.size() < enemiesPerWave + 5) {
            spawnEnemy(); enemySpawnTimer = 0;
        }

        for (Enemy e : enemies) {
            e.update();
            if (e.canShoot() && rand.nextInt(3) == 0) { bullets.add(e.createBullet()); e.resetShootTimer(); }
        }
        for (Bullet b : bullets) b.update();
        for (Particle p : particles) p.update();

        // Player bullets vs enemies
        Iterator<Bullet> bi = bullets.iterator();
        while (bi.hasNext()) {
            Bullet b = bi.next();
            if (!b.isPlayerBullet()) continue;
            Iterator<Enemy> ei = enemies.iterator();
            while (ei.hasNext()) {
                Enemy e = ei.next();
                if (b.getBounds().intersects(e.getBounds())) {
                    e.hit();
                    spawnParticles(e.getX() + Enemy.WIDTH / 2, e.getY() + Enemy.HEIGHT / 2,
                            e.getType() == Enemy.Type.TANK ? new Color(180, 50, 200) : new Color(255, 100, 50), 12);
                    bi.remove();
                    if (e.isDead()) {
                        score += e.getScore();
                        enemiesKilledThisWave++;
                        spawnParticles(e.getX() + Enemy.WIDTH / 2, e.getY() + Enemy.HEIGHT / 2, Color.ORANGE, 20);
                        ei.remove();
                    }
                    break;
                }
            }
        }

        // Enemy bullets vs player
        bi = bullets.iterator();
        while (bi.hasNext()) {
            Bullet b = bi.next();
            if (b.isPlayerBullet()) continue;
            if (b.getBounds().intersects(player.getBounds())) {
                player.hit();
                spawnParticles(player.getX() + Player.WIDTH / 2, player.getY() + Player.HEIGHT / 2, new Color(0, 200, 255), 10);
                bi.remove();
            }
        }

        // Enemy body vs player
        for (Enemy e : enemies) if (e.getBounds().intersects(player.getBounds())) player.hit();

        bullets.removeIf(Bullet::isOffScreen);
        enemies.removeIf(Enemy::isOffScreen);
        particles.removeIf(Particle::isDead);

        waveTimer++;
        if (waveTimer > 300 && enemies.isEmpty() && enemiesKilledThisWave >= enemiesPerWave) {
            wave++; enemiesKilledThisWave = 0; enemiesPerWave = 8 + wave * 2; waveTimer = 0;
        }

        if (player.getLives() <= 0) {
            if (score > highScore) highScore = score;
            state = GameState.GAME_OVER;
        }
    }

    private void spawnEnemy() {
        int ex = rand.nextInt(GameWindow.WIDTH - Enemy.WIDTH);
        int r = rand.nextInt(100);
        Enemy.Type type = (wave >= 3 && r < 20) ? Enemy.Type.TANK : (wave >= 2 && r < 40) ? Enemy.Type.FAST : Enemy.Type.BASIC;
        enemies.add(new Enemy(ex, -Enemy.HEIGHT, type));
    }

    private void spawnParticles(int x, int y, Color color, int count) {
        for (int i = 0; i < count; i++) particles.add(new Particle(x, y, color));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(5, 0, 20));
        g2.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);
        starField.draw(g2);

        if (state == GameState.MENU) { drawMenu(g2); return; }

        for (Particle p : particles) p.draw(g2);
        for (Enemy e : enemies) e.draw(g2);
        for (Bullet b : bullets) b.draw(g2);
        player.draw(g2);
        drawHUD(g2);

        if (state == GameState.GAME_OVER) drawGameOver(g2);
    }

    private void drawMenu(Graphics2D g) {
        g.setFont(titleFont);
        FontMetrics fm = g.getFontMetrics();
        String title = "SPACE SHOOTER";
        int tx = (GameWindow.WIDTH - fm.stringWidth(title)) / 2;
        g.setColor(new Color(0, 150, 255, 60));
        g.drawString(title, tx + 3, 183);
        g.setColor(new Color(0, 220, 255));
        g.drawString(title, tx, 180);

        g.setFont(uiFont); fm = g.getFontMetrics();
        String sub = "Press ENTER to Start";
        g.setColor(new Color(200, 200, 200));
        g.drawString(sub, (GameWindow.WIDTH - fm.stringWidth(sub)) / 2, 260);

        g.setFont(smallFont); fm = g.getFontMetrics();
        g.setColor(new Color(120, 180, 255));
        String[] lines = {"Arrow Keys / WASD — Move", "SPACE — Shoot", "Survive all waves!"};
        for (int i = 0; i < lines.length; i++)
            g.drawString(lines[i], (GameWindow.WIDTH - fm.stringWidth(lines[i])) / 2, 340 + i * 26);

        g.setFont(smallFont); fm = g.getFontMetrics();
        String[] legend = {"● Basic: 100pts", "● Fast: 200pts", "● Tank: 300pts (3 HP)"};
        Color[] lc = {new Color(255, 80, 80), new Color(255, 200, 0), new Color(200, 80, 255)};
        for (int i = 0; i < legend.length; i++) {
            g.setColor(lc[i]);
            g.drawString(legend[i], (GameWindow.WIDTH - fm.stringWidth(legend[i])) / 2, 460 + i * 24);
        }

        if (highScore > 0) {
            g.setFont(uiFont); fm = g.getFontMetrics();
            String hs = "High Score: " + highScore;
            g.setColor(new Color(255, 215, 0));
            g.drawString(hs, (GameWindow.WIDTH - fm.stringWidth(hs)) / 2, 555);
        }
    }

    private void drawHUD(Graphics2D g) {
        g.setFont(uiFont);
        g.setColor(new Color(0, 220, 255));
        g.drawString("SCORE: " + score, 15, 28);
        FontMetrics fm = g.getFontMetrics();
        String waveStr = "WAVE: " + wave;
        g.setColor(new Color(255, 200, 80));
        g.drawString(waveStr, (GameWindow.WIDTH - fm.stringWidth(waveStr)) / 2, 28);
        for (int i = 0; i < player.getLives(); i++) {
            g.setColor(new Color(255, 60, 100));
            g.fillOval(GameWindow.WIDTH - 30 - i * 28, 10, 20, 20);
        }
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);
        g.setFont(titleFont);
        FontMetrics fm = g.getFontMetrics();
        String go = "GAME OVER";
        int gx = (GameWindow.WIDTH - fm.stringWidth(go)) / 2;
        g.setColor(new Color(255, 50, 50, 80));
        g.drawString(go, gx + 3, 243);
        g.setColor(new Color(255, 80, 80));
        g.drawString(go, gx, 240);
        g.setFont(uiFont); fm = g.getFontMetrics();
        String sc = "Score: " + score;
        g.setColor(Color.WHITE);
        g.drawString(sc, (GameWindow.WIDTH - fm.stringWidth(sc)) / 2, 300);
        if (score == highScore && score > 0) {
            String hs = "NEW HIGH SCORE!";
            g.setColor(new Color(255, 215, 0));
            g.drawString(hs, (GameWindow.WIDTH - fm.stringWidth(hs)) / 2, 330);
        }
        g.setFont(smallFont); fm = g.getFontMetrics();
        String restart = "Press ENTER to Play Again  |  ESC to Menu";
        g.setColor(new Color(180, 180, 180));
        g.drawString(restart, (GameWindow.WIDTH - fm.stringWidth(restart)) / 2, 380);
    }

    @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_A) keyLeft  = true;
        if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) keyRight = true;
        if (k == KeyEvent.VK_UP    || k == KeyEvent.VK_W) keyUp    = true;
        if (k == KeyEvent.VK_DOWN  || k == KeyEvent.VK_S) keyDown  = true;
        if (k == KeyEvent.VK_SPACE) keyShoot = true;
        if (k == KeyEvent.VK_ENTER && (state == GameState.MENU || state == GameState.GAME_OVER)) initGame();
        if (k == KeyEvent.VK_ESCAPE && state == GameState.GAME_OVER) state = GameState.MENU;
    }
    @Override public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_A) keyLeft  = false;
        if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) keyRight = false;
        if (k == KeyEvent.VK_UP    || k == KeyEvent.VK_W) keyUp    = false;
        if (k == KeyEvent.VK_DOWN  || k == KeyEvent.VK_S) keyDown  = false;
        if (k == KeyEvent.VK_SPACE) keyShoot = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
}