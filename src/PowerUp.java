//enum to sztywna lista wyboru, z której nie da się wyjść poza szereg.
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