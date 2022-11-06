package tower;

import mindustry.ai.Pathfinder;
import mindustry.gen.PathTile;
import mindustry.world.Tile;

import static mindustry.Vars.pathfinder;
import static tower.Main.isPath;

public class TowerPathfinder extends Pathfinder {

    public static final int impassable = -1;

    public static void load() {
        pathfinder = new TowerPathfinder();

        costTypes.set(costGround, (team, tile) -> (PathTile.allDeep(tile) || PathTile.solid(tile)) ? impassable : 1 +
                (PathTile.nearSolid(tile) ? 25 : 0) +
                (PathTile.nearLiquid(tile) ? 25 : 0) +
                (PathTile.deep(tile) ? 6000 : 0) +
                (PathTile.damages(tile) ? 30 : 0));

        costTypes.set(costLegs, (team, tile) -> (PathTile.allDeep(tile) || PathTile.legSolid(tile)) ? impassable : 1 +
                (PathTile.deep(tile) ? 6000 : 0) +
                (PathTile.damages(tile) ? 30 : 0));

        costTypes.set(costNaval, (team, tile) -> (PathTile.solid(tile) || !PathTile.liquid(tile) ? 6000 : 1) +
                (PathTile.nearGround(tile) || PathTile.nearSolid(tile) ? 25 : 0) +
                (PathTile.damages(tile) ? 100 : 0));
    }

    @Override
    public int packTile(Tile tile) {
        boolean nearLiquid = false, nearSolid = false, nearGround = false, allDeep = tile.floor().isDeep();

        for (int i = 0; i < 4; i++) {
            var other = tile.nearby(i);
            if (other == null) continue;

            if (other.floor().isLiquid) nearLiquid = true;
            if (other.solid() || !isPath(other)) nearSolid = true;
            if (!other.floor().isLiquid) nearGround = true;
            if (!other.floor().isDeep()) allDeep = false;
        }

        return PathTile.get(
                0,
                tile.getTeamID(),
                tile.solid(),
                tile.floor().isLiquid,
                tile.staticDarkness() > 1,
                nearLiquid,
                nearGround,
                nearSolid,
                tile.floor().isDeep() || !isPath(tile),
                isPath(tile),
                allDeep,
                tile.block().teamPassable
        );
    }
}