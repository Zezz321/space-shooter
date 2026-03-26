//enum - a special Java type that defines a fixed set of constants.
public enum PowerUp {
    SHIELD("Shield", "Absorbs 1 hit", "🛡"),
    RAPID_FIRE("Rapid Fire", "Shoot 2x faster", "⚡"),
    SPREAD_SHOT("Spread Shot", "3 bullets at once", "✦"),
    SPEED_BOOST("Speed Boost", "Move faster", "▶"),
    DOUBLE_SCORE("Double Score", "2x points this wave", "★");

    public final String name, desc, icon;
    PowerUp(String name, String desc, String icon) {
        this.name = name; this.desc = desc; this.icon = icon;
    }
}