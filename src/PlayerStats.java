public class PlayerStats {
    public int speedBoosts = 0;      // Max 3 stacks
    public int rapidFireStacks = 0;  // Max 4 stacks
    public int spreadShotStacks = 0; // Max 2 stacks
    public boolean spreadShot = false; // Kept for compatibility
    public int shields = 0;
    public boolean doubleScore = false;

    /**
     * Calculates player speed based on speed boost stacks
     * Base speed 6, max speed 10 with 3 stacks
     */
    public int getSpeed() {
        // Base speed 6, each stack adds ~1.33, max 10
        return 6 + Math.min(speedBoosts * 2, 4);
    }

    /**
     * Calculates shoot cooldown based on rapid fire stacks
     * Each stack reduces cooldown significantly
     */
    public int getShootCooldown() {
        // Base cooldown 15, each stack reduces by ~2
        int cooldown = 15 - (rapidFireStacks * 2);
        return Math.max(7, cooldown);
    }
}