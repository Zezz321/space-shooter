import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements KeyListener, MouseListener {

    private static final int FPS = 60;
    private static final int TARGET_DELAY = 1000 / FPS;

    private enum GameState { MENU, PLAYING, WAVE_END, GAME_OVER, VICTORY }
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
    private int enemiesKilledThisWave = 0, enemiesPerWave = 6;
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

    /**
     * Initializes a new game - resets all stats and starts wave 1
     */
    private void initGame() {
        stats = new PlayerStats();
        player = new Player(GameWindow.WIDTH / 2 - Player.WIDTH / 2, GameWindow.HEIGHT - 80, stats);
        enemies.clear(); bullets.clear(); particles.clear();
        boss = null;
        score = 0; wave = 1;
        enemiesKilledThisWave = 0; enemiesPerWave = 6;
        enemySpawnTimer = 0; waveTimer = 0;
        state = GameState.PLAYING;
    }

    /**
     * Starts the next wave, resets wave-specific counters
     */
    private void startNextWave() {
        wave++;
        stats.doubleScore = false;
        enemiesKilledThisWave = 0;
        enemiesPerWave = 6 + (wave / 2) * 2;
        enemiesPerWave = Math.min(enemiesPerWave, 25);
        enemySpawnTimer = 0;
        waveTimer = 0;
        boss = null;
        state = GameState.PLAYING;
    }

    /**
     * Shows power-up selection cards at the end of a wave
     */
    private void showWaveEndCards() {
        // Check if player completed wave 15 (victory condition)
        if (wave == 15) {
            state = GameState.VICTORY;
            if (score > highScore) highScore = score;
            return;
        }

        // Get available power-ups (not maxed out)
        List<PowerUp> available = new ArrayList<>();
        for (PowerUp p : PowerUp.values()) {
            switch (p) {
                case SHIELD:
                    available.add(p);
                    break;
                case RAPID_FIRE:
                    if (stats.rapidFireStacks < 4) available.add(p);
                    break;
                case SPREAD_SHOT:
                    if (stats.spreadShotStacks < 2) available.add(p);
                    break;
                case SPEED_BOOST:
                    if (stats.speedBoosts < 3) available.add(p);
                    break;
                case DOUBLE_SCORE:
                    available.add(p);
                    break;
            }
        }

        // If all power-ups are maxed, show only shields and double score
        if (available.isEmpty()) {
            available.add(PowerUp.SHIELD);
            available.add(PowerUp.DOUBLE_SCORE);
            available.add(PowerUp.SHIELD);
        }

        Collections.shuffle(available);
        cardChoices[0] = available.get(0);
        cardChoices[1] = available.get(1 % available.size());
        cardChoices[2] = available.get(2 % available.size());

        state = GameState.WAVE_END;
    }

    /**
     * Applies selected power-up with stack limits
     */
    private void applyPowerUp(PowerUp p) {
        switch (p) {
            case SHIELD:
                stats.shields++;
                break;
            case RAPID_FIRE:
                if (stats.rapidFireStacks < 4) {
                    stats.rapidFireStacks++;
                }
                break;
            case SPREAD_SHOT:
                if (stats.spreadShotStacks < 2) {
                    stats.spreadShotStacks++;
                }
                stats.spreadShot = stats.spreadShotStacks > 0;
                break;
            case SPEED_BOOST:
                if (stats.speedBoosts < 3) {
                    stats.speedBoosts++;
                }
                break;
            case DOUBLE_SCORE:
                stats.doubleScore = true;
                break;
        }
        startNextWave();
    }

    /**
     * Main game update loop - handles all game logic
     */
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

        // Boss wave every 5 waves (wave 5, 10, 15)
        boolean isBossWave = (wave % 5 == 0);

        if (!isBossWave) {
            enemySpawnTimer++;
            int spawnRate;
            if (wave <= 3) {
                spawnRate = 45;
            } else if (wave <= 6) {
                spawnRate = 35;
            } else {
                spawnRate = Math.max(20, 50 - wave);
            }

            if (enemySpawnTimer >= spawnRate && enemiesKilledThisWave + enemies.size() < enemiesPerWave + 3) {
                spawnEnemy(); enemySpawnTimer = 0;
            }
        } else if (boss == null && enemies.isEmpty()) {
            boss = new Boss(wave);
        }

        // Update enemies and their shooting
        for (Enemy e : enemies) {
            e.update();
            int shootChance = wave <= 3 ? 6 : (wave <= 6 ? 4 : 3);
            if (e.canShoot() && rand.nextInt(shootChance) == 0) {
                bullets.add(e.createBullet());
                e.resetShootTimer();
            }
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

        // Player bullets vs enemies and boss
        Iterator<Bullet> bi = bullets.iterator();
        while (bi.hasNext()) {
            Bullet b = bi.next();
            if (!b.isPlayerBullet()) continue;

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

        // Enemy body collision with player
        for (Enemy e : enemies) if (e.getBounds().intersects(player.getBounds())) player.hit();
        if (boss != null && boss.getBounds().intersects(player.getBounds())) player.hit();

        // Score penalty when enemies escape past bottom
        Iterator<Enemy> ei = enemies.iterator();
        while (ei.hasNext()) {
            Enemy e = ei.next();
            if (e.isOffScreen()) {
                int penalty = wave <= 3 ? 20 : 50;
                score = Math.max(0, score - penalty);
                spawnParticles(e.getX() + Enemy.WIDTH/2, GameWindow.HEIGHT - 10, new Color(255, 50, 50), 8);
                ei.remove();
            }
        }

        bullets.removeIf(Bullet::isOffScreen);
        particles.removeIf(Particle::isDead);

        waveTimer++;
        boolean waveCleared;
        if (isBossWave) {
            waveCleared = (boss == null);
        } else {
            waveCleared = (enemies.isEmpty() && enemiesKilledThisWave >= enemiesPerWave);
        }

        int endDelay = wave <= 3 ? 60 : 180;
        if (waveTimer > endDelay && waveCleared) {
            showWaveEndCards();
        }

        if (player.getLives() <= 0) {
            if (score > highScore) highScore = score;
            state = GameState.GAME_OVER;
        }
    }

    /**
     * Spawns a new enemy with type based on wave progression
     */
    private void spawnEnemy() {
        int ex = rand.nextInt(GameWindow.WIDTH - Enemy.WIDTH);
        int r = rand.nextInt(100);
        Enemy.Type type;

        if (wave >= 8 && r < 15) {
            type = Enemy.Type.SHOOTER;
        } else if (wave >= 6 && r < 30) {
            type = Enemy.Type.ZIGZAG;
        } else if (wave >= 5 && r < 45) {
            type = Enemy.Type.TANK;
        } else if (wave >= 3 && r < 65) {
            type = Enemy.Type.FAST;
        } else {
            type = Enemy.Type.BASIC;
        }
        enemies.add(new Enemy(ex, -Enemy.HEIGHT, type, wave));
    }

    /**
     * Creates explosion particles at given position
     */
    private void spawnParticles(int x, int y, Color color, int count) {
        for (int i = 0; i < count; i++) particles.add(new Particle(x, y, color));
    }

    // ── Drawing Methods ──────────────────────────────────────────────────────────────

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
        if (state == GameState.VICTORY)  { drawVictory(g2); return; }

        for (Particle p : particles) p.draw(g2);
        if (boss != null) boss.draw(g2);
        for (Enemy e : enemies) e.draw(g2);
        for (Bullet b : bullets) b.draw(g2);
        player.draw(g2);
        drawHUD(g2);

        if (state == GameState.GAME_OVER) drawGameOver(g2);
    }

    /**
     * Draws victory screen when player beats wave 15
     */
    private void drawVictory(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, GameWindow.WIDTH, GameWindow.HEIGHT);

        g.setFont(titleFont);
        FontMetrics fm = g.getFontMetrics();
        String victory = "VICTORY!";
        int vx = (GameWindow.WIDTH - fm.stringWidth(victory)) / 2;
        g.setColor(new Color(255, 215, 0, 80));
        g.drawString(victory, vx + 3, 243);
        g.setColor(new Color(255, 215, 0));
        g.drawString(victory, vx, 240);

        g.setFont(uiFont);
        fm = g.getFontMetrics();
        String congrats = "You defeated the final boss!";
        g.setColor(Color.WHITE);
        g.drawString(congrats, (GameWindow.WIDTH - fm.stringWidth(congrats)) / 2, 300);

        String scoreMsg = "Final Score: " + score;
        g.drawString(scoreMsg, (GameWindow.WIDTH - fm.stringWidth(scoreMsg)) / 2, 340);

        g.setFont(smallFont);
        fm = g.getFontMetrics();
        String restart = "Press ENTER to Play Again  |  ESC for Menu";
        g.setColor(new Color(180, 180, 180));
        g.drawString(restart, (GameWindow.WIDTH - fm.stringWidth(restart)) / 2, 400);
    }

    /**
     * Draws power-up selection screen between waves
     */
    private void drawWaveEnd(Graphics2D g) {
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

        // Current stats display
        g.setFont(smallFont);
        fm = g.getFontMetrics();
        String statsStr = "Speed +" + stats.speedBoosts + "/3" +
                "  |  RapidFire x" + stats.rapidFireStacks + "/4" +
                "  |  Shields: " + stats.shields +
                (stats.spreadShotStacks > 0 ? "  |  Spread x" + stats.spreadShotStacks + "/2" : "");
        g.setColor(new Color(150, 200, 255));
        g.drawString(statsStr, (GameWindow.WIDTH - fm.stringWidth(statsStr)) / 2, 360);
    }

    private void drawCard(Graphics2D g, PowerUp p, int x, int y) {
        g.setColor(new Color(20, 20, 50, 220));
        g.fillRoundRect(x, y, CARD_W, CARD_H, 16, 16);
        g.setColor(new Color(80, 120, 255));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x, y, CARD_W, CARD_H, 16, 16);
        g.setStroke(new BasicStroke(1));

        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(new Color(255, 220, 80));
        g.drawString(p.icon, x + (CARD_W - fm.stringWidth(p.icon)) / 2, y + 42);

        g.setFont(cardFont);
        fm = g.getFontMetrics();
        g.setColor(Color.WHITE);
        g.drawString(p.name, x + (CARD_W - fm.stringWidth(p.name)) / 2, y + 72);

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
        String[] lines = {"Arrow Keys / WASD — Move", "SPACE — Shoot", "Pick a power-up after each wave!", "Defeat wave 15 to win!"};
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

        // Lives as hearts
        for (int i = 0; i < player.getLives(); i++) {
            g.setColor(new Color(255, 60, 100));
            g.fillOval(GameWindow.WIDTH - 30 - i * 28, 10, 20, 20);
        }

        // Active power-up indicators
        g.setFont(smallFont);
        int px = 15;
        if (stats.shields > 0)      { g.setColor(new Color(0, 200, 255)); g.drawString("SHIELD x" + stats.shields, px, 50); px += 110; }
        if (stats.rapidFireStacks > 0) { g.setColor(new Color(255, 255, 0)); g.drawString("RAPID x" + stats.rapidFireStacks + "/4", px, 50); px += 110; }
        if (stats.spreadShotStacks > 0) { g.setColor(new Color(100, 255, 100)); g.drawString("SPREAD x" + stats.spreadShotStacks + "/2", px, 50); px += 100; }
        if (stats.speedBoosts > 0)  { g.setColor(new Color(255, 150, 0));  g.drawString("SPD +" + stats.speedBoosts + "/3", px, 50); px += 100; }
        if (stats.doubleScore)      { g.setColor(new Color(255, 215, 0));  g.drawString("2x SCORE", px, 50); }

        // Score penalty warning
        g.setFont(smallFont);
        g.setColor(new Color(255, 80, 80, 160));
        int penalty = wave <= 3 ? 20 : 50;
        g.drawString("-" + penalty + "pts if enemy escapes!", GameWindow.WIDTH - 210, GameWindow.HEIGHT - 10);
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

    // ── Input Handling ─────────────────────────────────────────────────────────────────

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
        if (k == KeyEvent.VK_ENTER && (state == GameState.MENU || state == GameState.GAME_OVER || state == GameState.VICTORY)) initGame();
        if (k == KeyEvent.VK_ESCAPE && (state == GameState.GAME_OVER || state == GameState.VICTORY)) state = GameState.MENU;
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