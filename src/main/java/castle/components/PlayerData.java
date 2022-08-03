package castle.components;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Strings;
import castle.CastleRooms;
import mindustry.entities.Units;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;

import java.util.Locale;

import static castle.CastleLogic.timer;
import static castle.Main.findLocale;

public class PlayerData {

    public static ObjectMap<String, PlayerData> datas = new ObjectMap<>();

    public Player player;
    public Interval interval;

    public int money = 0;
    public int income = 15;

    public boolean hideHud = false;
    public Locale locale;

    public static Seq<PlayerData> datas() {
        return datas.values().toSeq();
    }

    public PlayerData(Player player) {
        handlePlayerJoin(player);
    }

    public void update() {
        if (!player.con.isConnected()) return;

        if (interval.get(60f)) money += income * getBonus();

        if (player.shooting) CastleRooms.rooms.each(room -> room.check(player.mouseX, player.mouseY) && room.canBuy(this), room -> room.buy(this));

        if (hideHud) return;
        StringBuilder hud = new StringBuilder(Bundle.format("ui.hud.balance", locale, money, income));

        if (getBonus() > 1f) hud.append(Strings.format(" [lightgray](x@)", getStringBonus()));
        if (Units.getCap(player.team()) <= player.team().data().unitCount) hud.append(Bundle.format("ui.hud.unit-limit", locale, Units.getCap(player.team())));

        hud.append(Bundle.format("ui.hud.timer", locale, timer));
        Call.setHudText(player.con, hud.toString());
    }

    public void handlePlayerJoin(Player player) {
        this.player = player;
        this.locale = findLocale(player);
        this.interval = new Interval();
    }

    public float getBonus() {
        return Math.max((float) Groups.player.size() / Groups.player.count(p -> p.team() == player.team()) / 2f, 1f);
    }

    public String getStringBonus() {
        String bonus = String.valueOf(getBonus());
        return bonus.substring(0, Math.min(4, bonus.length()));
    }
}
