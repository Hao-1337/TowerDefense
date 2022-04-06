package castle;

import arc.math.Mathf;
import arc.math.geom.Position;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Interval;
import castle.components.Bundle;
import castle.components.CastleIcons;
import castle.components.PlayerData;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Iconc;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class CastleRooms {

    public static final Seq<Room> rooms = new Seq<>();
    public static final ObjectMap<Block, Integer> blockCosts = new ObjectMap<>();

    public static final int size = 8;
    public static Tile shardedSpawn, blueSpawn;

    public static void load() {
        // TODO сделать формулу для высчета стоимости турели по ее урону, хп, размеру?
        blockCosts.putAll(
                Blocks.duo, 100,
                Blocks.scatter, 250,
                Blocks.scorch, 200,
                Blocks.hail, 450,
                Blocks.wave, 300,
                Blocks.lancer, 350,
                Blocks.arc, 150,
                Blocks.parallax, 500,
                Blocks.swarmer, 1250,
                Blocks.salvo, 500,
                Blocks.segment, 750,
                Blocks.tsunami, 850,
                Blocks.fuse, 1500,
                Blocks.ripple, 1500,
                Blocks.cyclone, 1750,
                Blocks.foreshadow, 4000,
                Blocks.spectre, 3000,
                Blocks.meltdown, 3000,

                Blocks.commandCenter, 750,
                Blocks.repairPoint, 300,
                Blocks.repairTurret, 1200
        );
    }

    public static class Room implements Position {
        public int x;
        public int y;

        public int startx;
        public int starty;
        public int endx;
        public int endy;

        public int cost;
        public int size;

        public Tile tile;
        public String label = "";

        public Room(int x, int y, int cost, int size) {
            this.x = x;
            this.y = y;

            this.startx = x - size / 2;
            this.starty = y - size / 2;
            this.endx = x + size / 2;
            this.endy = y + size / 2;

            this.cost = cost;
            this.size = size;
            this.tile = world.tile(x, y);

            rooms.add(this);
        }

        public void update() {}

        public void buy(PlayerData data) {
            data.money -= cost;
            Groups.player.each(p -> Call.label(p.con, Bundle.format("events.buy", Bundle.findLocale(p), data.player.coloredName()), 4f, getX(), getY()));
        }

        public boolean canBuy(PlayerData data) {
            return showLabel(data) && data.money >= cost;
        }

        public boolean showLabel(PlayerData data) {
            return true;
        }

        public boolean check(float x, float y) {
            return x > this.startx * tilesize && y > this.starty * tilesize && x < this.endx * tilesize && y < this.endy * tilesize;
        }

        public float getX() {
            return x * tilesize;
        }

        public float getY() {
            return y * tilesize;
        }
    }

    public static class BlockRoom extends Room {
        public Block block;
        public Team team;

        public boolean bought;

        public BlockRoom(Block block, Team team, int x, int y, int cost, int size) {
            super(x, y, cost, size);

            this.block = block;
            this.team = team;
            this.label = CastleIcons.get(block) + " :[white] " + cost;
        }

        public BlockRoom(Block block, Team team, int x, int y, int cost) {
            this(block, team, x, y, cost, block.size + 1);
        }

        /** special for cores */
        public BlockRoom(Team team, int x, int y, int cost) {
            this(Blocks.coreNucleus, team, x, y, cost, Blocks.coreShard.size + 1);
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            bought = true;

            tile.setNet(block, team, 0);
            if (block instanceof CoreBlock) return;
            tile.build.health(Float.MAX_VALUE);
        }

        @Override
        public boolean canBuy(PlayerData data) {
            return super.canBuy(data) && showLabel(data);
        }

        @Override
        public boolean showLabel(PlayerData data) {
            return data.player.team() == team && !bought;
        }

        @Override
        public void update() {
            if (world.build(x, y) == null) bought = false;
        }
    }

    public static class MinerRoom extends BlockRoom {
        public ItemStack stack;
        public Interval interval = new Interval();

        public MinerRoom(ItemStack stack, Team team, int x, int y, int cost) {
            super(Blocks.laserDrill, team, x, y, cost);

            this.stack = stack;
            this.label = "[" + CastleIcons.get(stack.item) + "] " + CastleIcons.get(block) + " :[white] " + cost;
        }

        @Override
        public void update() {
            super.update();

            // TODO прокачка скорости добычи?
            if (bought && interval.get(300f)) {
                Call.effect(Fx.mineHuge, x * tilesize, y * tilesize, 0f, team.color);
                Call.transferItemTo(null, stack.item, stack.amount, x * tilesize, y * tilesize, team.core());
            }
        }
    }

    public static class UnitRoom extends Room {

        public enum UnitRoomType {
            attack, defend
        }

        public UnitType unitType;
        public UnitRoomType roomType;

        public int income;

        public UnitRoom(UnitType unitType, UnitRoomType roomType, int income, int x, int y, int cost) {
            super(x, y, cost, 4);
            this.unitType = unitType;
            this.roomType = roomType;
            this.income = income;

            // TODO упростить?
            StringBuilder str = new StringBuilder();

            str.append(" ".repeat(Math.max(0, (String.valueOf(income).length() + String.valueOf(cost).length() + 2) / 2))).append(CastleIcons.get(unitType));

            if (roomType == UnitRoomType.attack) str.append(" [accent]").append(Iconc.modeAttack);
            else str.append(" [scarlet]").append(Iconc.defense);

            str.append("\n[gray]").append(cost).append("\n[white]").append(Iconc.blockPlastaniumCompressor).append(" : ");

            this.label = str.append(income < 0 ? "[crimson]" : income > 0 ? "[lime]+" : "[gray]").append(income).toString();
        }

        @Override
        public void buy(PlayerData data) {
            data.money -= cost;
            data.income += income;

            if (roomType == UnitRoomType.attack) {
                unitType.spawn(data.player.team(), (data.player.team() == Team.sharded ? blueSpawn.drawx() : shardedSpawn.drawx()) + Mathf.random(-40, 40), (data.player.team() == Team.sharded ? blueSpawn.drawy() : shardedSpawn.drawy()) + Mathf.random(-40, 40));
            } else if (data.player.team().core() != null) {
                unitType.spawn(data.player.team(), data.player.team().core().x + 40, data.player.team().core().y + Mathf.random(-40, 40));
            }
        }

        @Override
        public boolean canBuy(PlayerData data) {
            return super.canBuy(data) && (income > 0 || data.income + income > 0) && Units.getCap(data.player.team()) > data.player.team().data().unitCount;
        }
    }
}
