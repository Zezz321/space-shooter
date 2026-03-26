public class PlayerStats {
    public int speedBoosts = 0;
    public int rapidFireStacks = 0;
    public boolean spreadShot = false;
    public int shields = 0;
    public boolean doubleScore = false;

    public int getSpeed()        { return 5 + speedBoosts * 2; }
    public int getShootCooldown(){ return Math.max(4, 15 - rapidFireStacks * 3); }
}