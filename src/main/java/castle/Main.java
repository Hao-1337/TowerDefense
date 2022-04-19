package castle;

import arc.Events;
import arc.struct.Seq;
import arc.util.CommandHandler;
import arc.util.Interval;
import castle.CastleRooms.Room;
import castle.ai.AIShell;
import castle.components.*;
import mindustry.content.Blocks;
import mindustry.content.UnitTypes;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;
import mindustry.net.Administration.ActionType;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.production.Drill;
import mindustry.world.blocks.storage.CoreBlock;

import static castle.CastleLogic.*;
import static mindustry.Vars.*;

public class Main extends Plugin {

    public static Interval interval = new Interval();

    @Override
    public void init() {
        ((CoreBlock) Blocks.coreShard).unitType = UnitTypes.poly;
        ((CoreBlock) Blocks.coreNucleus).unitType = UnitTypes.mega;

        CastleLogic.load();
        CastleIcons.load();
        CastleUnits.load();
        CastleRooms.TurretRoom.loadCosts();

        content.units().each(unit -> {
           var parent = unit.defaultController;
           unit.defaultController = () -> new AIShell(parent);
        });

        netServer.admins.addActionFilter(action -> {
            if (action.tile != null && (action.tile.block() instanceof Turret || action.tile.block() instanceof Drill)) return false;
            return action.tile == null || action.type != ActionType.placeBlock || (action.tile.dst(CastleRooms.shardedSpawn) > 64 && action.tile.dst(CastleRooms.blueSpawn) > 64);
        });

        netServer.assigner = (player, players) -> {
            Seq<Player> arr = Seq.with(players);
            int sharded = arr.count(p -> p.team() == Team.sharded);
            return arr.size - sharded >= sharded ? Team.sharded : Team.blue;
        };

        Events.on(PlayerJoin.class, event -> {
            if (Groups.player.size() == 1) event.player.sendMessage(Bundle.get("commands.skip.offer", Bundle.findLocale(event.player)));
            if (PlayerData.datas.containsKey(event.player.uuid())) {
                PlayerData.datas.get(event.player.uuid()).handlePlayerJoin(event.player);
            } else {
                PlayerData.datas.put(event.player.uuid(), new PlayerData(event.player));
            }
        });

        Events.on(BlockDestroyEvent.class, event -> {
            if (isBreak()) return;
            if (event.tile.block() instanceof CoreBlock && event.tile.team().cores().size <= 1)
                gameOver(event.tile.team() == Team.sharded ? Team.blue : Team.sharded);
        });

        Events.on(UnitDestroyEvent.class, event -> {
            int income = CastleUnits.drop(event.unit.type);
            if (income <= 0 || event.unit.spawnedByCore) return;
            PlayerData.datas().each(data -> {
                if (data.player.team() != event.unit.team) {
                    data.money += income;
                    Call.label(data.player.con, "[lime] + [accent]" + income, 2f, event.unit.x, event.unit.y);
                }
            });
        });

        Events.run(Trigger.update, () -> {
            if (isBreak()) return;

            Groups.unit.each(unit -> unit.isFlying() && !unit.spawnedByCore && (unit.tileOn() == null || unit.tileOn().floor() == Blocks.space), Call::unitDespawn);

            PlayerData.datas().each(PlayerData::update);
            CastleRooms.rooms.each(Room::update);

            if (timer <= 0) gameOver(Team.derelict);
            else if (interval.get(60f)) timer--;
        });

        CastleLogic.restart();
        netServer.openServer();
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("hud", "Toggle HUD.", (args, player) -> {
            PlayerData data = PlayerData.datas.get(player.uuid());
            data.hideHud = !data.hideHud;
            if (data.hideHud) Call.hideHudText(player.con);
            Bundle.bundled(player, data.hideHud ? "commands.hud.off" : "commands.hud.on");
        });

        handler.<Player>register("skip", "Skip the current map.", (args, player) -> {
            if (Groups.player.size() == 1) gameOver(Team.derelict);
            else player.sendMessage(Bundle.get("commands.skip.not-alone", Bundle.findLocale(player)));
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.removeCommand("host");
        handler.removeCommand("stop");
        handler.removeCommand("gameover");

        handler.register("gameover", "End the game.", args -> gameOver(Team.derelict));
    }
}
