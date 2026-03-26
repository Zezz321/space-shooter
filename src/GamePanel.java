import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements KeyListener, MouseListener {

    private static final int FPS = 60;
    private static final int TARGET_DELAY = 1000 / FPS;

    private enum GameState { MENU, PLAYING, WAVE_END, GAME_OVER }
    private GameState state = GameState.MENU;

    private Player player;
    private PlayerStats stats;
    private Boss boss = null;
    private List<Enemy> enemies = new ArrayList<>();
    private List<Bullet> bullets = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();
    private StarField starField;

    private int score = 0, highScore = 0, wave = 1;
    private int enemySpawnTimer = 0, waveTimer = 0;
    private int enemiesKilledThisWave = 0, enemiesPerWave = 8;
    private boolean keyLeft, keyRight, keyUp, keyDown, keyShoot;
    private final Random rand = new Random();

    // Power-up card selection
    private PowerUp[] cardChoices = new PowerUp[3];
    private static final int CARD_W = 180, CARD_H = 120, CARD_GAP = 30;

    private Font titleFont, uiFont, smallFont, cardFont;

    public GamePanel() {
        setPreferredSize(new Dimension(GameWindow.WIDTH, GameWindow.HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        starField = new StarField();
        titleFont = new Font("Monospaced", Font.BOLD, 52);
        uiFont    = new Font("Monospaced", Font.BOLD, 18);
        smallFont = new Font("Monospaced", Font.PLAIN, 14);
        cardFont  = new Font("Monospaced", Font.BOLD, 15);
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
        stats = new PlayerStats();
        player = new Player(GameWindow.WIDTH / 2 - Player.WIDTH / 2, GameWindow.HEIGHT - 80, stats);
        enemies.clear(); bullets.clear(); particles.clear();
        boss = null;
        score = 0; wave = 1;
        enemiesKilledThisWave = 0; enemiesPerWave = 8;
        enemySpawnTimer = 0; waveTimer = 0;
        state = GameState.PLAYING;
    }

    private void startNextWave() {
        wave++;
        stats.doubleScore = false;
        enemiesKilledThisWave = 0;
        enemiesPerWave = 8 + wave * 2;
        enemySpawnTimer = 0;
        waveTimer = 0;
        boss = null;
        state = GameState.PLAYING;
    }

    private void showWaveEndCards() {
        List<PowerUp> all = new ArrayList<>(Arrays.asList(PowerUp.values()));
        Collections.shuffle(all);
        cardChoices[0] = all.get(0);
        cardChoices[1] = all.get(1);
        cardChoices[2] = all.get(2);
        state = GameState.WAVE_END;
    }

    private void applyPowerUp(PowerUp p) {
        switch (p) {
            case SHIELD:       stats.shields++; break;
            case RAPID_FIRE:   stats.rapidFireStacks++; break;
            case SPREAD_SHOT:  stats.spreadShot = true; break;
            case SPEED_BOOST:  stats.speedBoosts++; break;
            case DOUBLE_SCORE: stats.doubleScore = true; break;
        }
        startNextWave();
    }

    private void update() {
        starField.update();
        if (state != GameState.PLAYING) return;

        player.setMovingLeft(keyLeft);   player.setMovingRight(keyRight);
        player.setMovingUp(keyUp);       player.setMovingDown(keyDown);
        player.update();

        if (keyShoot && player.canShoot()) {
            for (Bullet b : player.createBullets()) bullets.add(b);
            player.shoot();
        }

        // Boss wave every 5 waves
        boolean isBossWave = wave % 5 == 0;

        if (!isBossWave) {
            enemySpawnTimer++;
            int spawnRate = Math.max(15, 60 - wave * 3);
            if (enemySpawnTimer >= spawnRate && enemiesKilledThisWave + enemies.size() < enemiesPerWave + 5) {
                spawnEnemy(); enemySpawnTimer = 0;
            }
        } else if (boss == null && enemies.isEmpty()) {
            boss = new Boss(wave);
        }

        // Update enemies
        for (Enemy e : enemies) {
            e.update();
            if (e.canShoot() && rand.nextInt(3) == 0) { bullets.add(e.createBullet()); e.resetShootTimer(); }
        }

        // Update boss
        if (boss != null) {
            boss.update();
            if (boss.canShoot()) {
                for (Bullet b : boss.createBullets()) bullets.add(b);
                boss.resetShootTimer();
            }
        }

        for (Bullet b : bullets) b.update();
        for (Particle p : particles) p.update();

        // Player bullets vs enemies
        Iterator<Bullet> bi = bullets.iterator();
        while (bi.hasNext()) {
            Bullet b = bi.next();
            if (!b.isPlayerBullet()) continue;

            // vs boss
            if (boss != null && b.getBounds().intersects(boss.getBounds())) {
                boss.hit(1);
                spawnParticles(boss.getX() + Boss.WIDTH/2, boss.getY() + Boss.HEIGHT/2, new Color(200, 0, 255), 8);
                bi.remove();
                if (boss.isDead()) {
                    int pts = boss.getScore() * (stats.doubleScore ? 2 : 1);
                    score += pts;
                    spawnParticles(boss.getX() + Boss.WIDTH/2, boss.getY() + Boss.HEIGHT/2, Color.MAGENTA, 40);
                    boss = null;
                }
                continue;
            }

            // vs enemies
            Iterator<Enemy> ei = enemies.iterator();
            boolean hit = false;
            while (ei.hasNext() && !hit) {
                Enemy e = ei.next();
                if (b.getBounds().intersects(e.getBounds())) {
                    e.hit();
                    spawnParticles(e.getX() + Enemy.WIDTH/2, e.getY() + Enemy.HEIGHT/2, new Color(255, 100, 50), 10);
                    bi.remove(); hit = true;
                    if (e.isDead()) {
                        int pts = e.getScore() * (stats.doubleScore ? 2 : 1);
                        score += pts;
                        enemiesKilledThisWave++;
                        spawnParticles(e.getX() + Enemy.WIDTH/2, e.getY() + Enemy.HEIGHT/2, Color.ORANGE, 18);
                        ei.remove();
                    }
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
                spawnParticles(player.getX() + Player.WIDTH/2, player.getY() + Player.HEIGHT/2, new Color(0, 200, 255), 10);
                bi.remove();
            }
        }

        // Enemy body vs player
        for (Enemy e : enemies) if (e.getBounds().intersects(player.getBounds())) player.hit();
        if (boss != null && boss.getBounds().intersects(player.getBounds())) player.hit();

        // Score penalty — enemy passes bottom
        Iterator<Enemy> ei = enemies.iterator();
        while (ei.hasNext()) {
            Enemy e = ei.next();
            if (e.isOffScreen()) {
                score = Math.max(0, score - 50);
                spawnParticles(e.getX() + Enemy.WIDTH/2, GameWindow.HEIGHT - 10, new Color(255, 50, 50), 8);
                ei.remove();
            }
        }

        bullets.removeIf(Bullet::isOffScreen);
        particles.removeIf(Particle::isDead);

        waveTimer++;
        boolean waveCleared = isBossWave ? (boss == null) : (enemies.isEmpty() && enemiesKilledThisWave >= enemiesPerWave);
        if (waveTimer > 180 && waveCleared) showWaveEndCards();

        if (player.getLives() <= 0) {
            if (score > highScore) highScore = score;
            state = GameState.GAME_OVER;
        }
    }

    private void spawnEnemy() {
        int ex = rand.nextInt(GameWindow.WIDTH - Enemy.WIDTH);
        int r = rand.nextInt(100);
        Enemy.Type type;
        if      (wave >= 6 && r < 15) type = Enemy.Type.SHOOTER;
        else if (wave >= 4 && r < 30) type = Enemy.Type.ZIGZAG;
        else if (wave >= 3 && r < 45) type = Enemy.Type.TANK;
        else if (wave >= 2 && r < 60) type = Enemy.Type.FAST;
        else                          type = Enemy.Type.BASIC;
        enemies.add(new Enemy(ex, -Enemy.HEIGHT, type, wave));
    }

    private void spawnParticles(int x, int y, Color color, int count) {
        for (int i = 0; i < count; i++) particles.add(new Particle(x, y, color));
    }

    // ── Drawing ──────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(5, 0, 20));
        g2.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);
        starField.draw(g2);

        if (state == GameState.MENU)     { drawMenu(g2); return; }
        if (state == GameState.WAVE_END) { drawWaveEnd(g2); return; }

        for (Particle p : particles) p.draw(g2);
        if (boss != null) boss.draw(g2);
        for (Enemy e : enemies) e.draw(g2);
        for (Bullet b : bullets) b.draw(g2);
        player.draw(g2);
        drawHUD(g2);

        if (state == GameState.GAME_OVER) drawGameOver(g2);
    }

    private void drawWaveEnd(Graphics2D g) {
        // Dim background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);

        g.setFont(uiFont);
        FontMetrics fm = g.getFontMetrics();
        String title = "WAVE " + wave + " COMPLETE — CHOOSE A POWER-UP";
        g.setColor(new Color(255, 215, 0));
        g.drawString(title, (GameWindow.WIDTH - fm.stringWidth(title)) / 2, 100);

        int totalW = 3 * CARD_W + 2 * CARD_GAP;
        int startX = (GameWindow.WIDTH - totalW) / 2;
        int cardY  = 160;

        for (int i = 0; i < 3; i++) {
            int cx = startX + i * (CARD_W + CARD_GAP);
            drawCard(g, cardChoices[i], cx, cardY);
        }

        // Current stats
        g.setFont(smallFont);
        fm = g.getFontMetrics();
        String statsStr = "Speed +" + stats.speedBoosts +
                "  |  RapidFire x" + stats.rapidFireStacks +
                "  |  Shields: " + stats.shields +
                (stats.spreadShot ? "  |  Spread" : "");
        g.setColor(new Color(150, 200, 255));
        g.drawString(statsStr, (GameWindow.WIDTH - fm.stringWidth(statsStr)) / 2, 360);
    }

    private void drawCard(Graphics2D g, PowerUp p, int x, int y) {
        // Card background
        g.setColor(new Color(20, 20, 50, 220));
        g.fillRoundRect(x, y, CARD_W, CARD_H, 16, 16);
        g.setColor(new Color(80, 120, 255));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x, y, CARD_W, CARD_H, 16, 16);
        g.setStroke(new BasicStroke(1));

        // Icon
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(255, 220, 80));
        g.drawString(p.icon, x + (CARD_W - fm.stringWidth(p.icon)) / 2, y + 42);

        // Name
        g.setFont(cardFont);
        fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(p.name, x + (CARD_W - fm.stringWidth(p.name)) / 2, y + 72);

        // Description
        g.setFont(smallFont);
        fm = g.getFontMetrics();
        g.setColor(new Color(180, 180, 180));
        g.drawString(p.desc, x + (CARD_W - fm.stringWidth(p.desc)) / 2, y + 95);
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
        String[] lines = {"Arrow Keys / WASD — Move", "SPACE — Shoot", "Pick a power-up after each wave!"};
        for (int i = 0; i < lines.length; i++)
            g.drawString(lines[i], (GameWindow.WIDTH - fm.stringWidth(lines[i])) / 2, 330 + i * 26);

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
        boolean isBossWave = wave % 5 == 0;
        String waveStr = isBossWave ? "!! BOSS WAVE " + wave + " !!" : "WAVE: " + wave;
        g.setColor(isBossWave ? new Color(255, 80, 255) : new Color(255, 200, 80));
        g.drawString(waveStr, (GameWindow.WIDTH - fm.stringWidth(waveStr)) / 2, 28);

        // Lives
        for (int i = 0; i < player.getLives(); i++) {
            g.setColor(new Color(255, 60, 100));
            g.fillOval(GameWindow.WIDTH - 30 - i * 28, 10, 20, 20);
        }

        // Active power-up indicators
        g.setFont(smallFont);
        int px = 15;
        if (stats.shields > 0)      { g.setColor(new Color(0, 200, 255)); g.drawString("SHIELD x" + stats.shields, px, 50); px += 110; }
        if (stats.rapidFireStacks > 0) { g.setColor(new Color(255, 255, 0)); g.drawString("RAPID x" + stats.rapidFireStacks, px, 50); px += 100; }
        if (stats.spreadShot)       { g.setColor(new Color(100, 255, 100)); g.drawString("SPREAD", px, 50); px += 80; }
        if (stats.speedBoosts > 0)  { g.setColor(new Color(255, 150, 0));  g.drawString("SPD +" + stats.speedBoosts, px, 50); px += 80; }
        if (stats.doubleScore)      { g.setColor(new Color(255, 215, 0));  g.drawString("2x SCORE", px, 50); }

        // Score penalty warning
        g.setFont(smallFont);
        g.setColor(new Color(255, 80, 80, 160));
        g.drawString("-50pts if enemy escapes!", GameWindow.WIDTH - 210, GameWindow.HEIGHT - 10);
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
        if (score > 0 && score == highScore) {
            String hs = "NEW HIGH SCORE!";
            g.setColor(new Color(255, 215, 0));
            g.drawString(hs, (GameWindow.WIDTH - fm.stringWidth(hs)) / 2, 330);
        }
        g.setFont(smallFont); fm = g.getFontMetrics();
        String restart = "Press ENTER to Play Again  |  ESC for Menu";
        g.setColor(new Color(180, 180, 180));
        g.drawString(restart, (GameWindow.WIDTH - fm.stringWidth(restart)) / 2, 380);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public void mouseClicked(MouseEvent e) {
        if (state != GameState.WAVE_END) return;
        int totalW = 3 * CARD_W + 2 * CARD_GAP;
        int startX = (GameWindow.WIDTH - totalW) / 2;
        int cardY  = 160;
        for (int i = 0; i < 3; i++) {
            int cx = startX + i * (CARD_W + CARD_GAP);
            if (e.getX() >= cx && e.getX() <= cx + CARD_W &&
                    e.getY() >= cardY && e.getY() <= cardY + CARD_H) {
                applyPowerUp(cardChoices[i]);
                return;
            }
        }
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
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}