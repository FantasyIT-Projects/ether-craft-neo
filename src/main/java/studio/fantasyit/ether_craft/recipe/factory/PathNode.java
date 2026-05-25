package studio.fantasyit.ether_craft.recipe.factory;

import org.joml.Vector2i;

import java.util.Set;

public record PathNode(Vector2i pos, int depth, int next) {
    public PathNode(int x, int y, int depth, Set<Vector2i> next) {
        int a = 0;
        for (Vector2i vector2i : next) {
            if (vector2i.x == x && vector2i.y == y - 1)
                a |= getDirectionBit(Direction.UP);
            if (vector2i.x == x && vector2i.y == y + 1)
                a |= getDirectionBit(Direction.DOWN);
            if (vector2i.x == x + 1 && vector2i.y == y)
                a |= getDirectionBit(Direction.RIGHT);
            if (vector2i.x == x - 1 && vector2i.y == y)
                a |= getDirectionBit(Direction.LEFT);
        }
        if (next.isEmpty())
            a = getDirectionBit(Direction.LEFT);
        this(new Vector2i(x, y), depth, a);
    }


    private static int getDirectionBit(Direction direction) {
        return 1 << direction.ordinal();
    }

    public static boolean isDirect(Direction direction, int pathDirection) {
        return (pathDirection & getDirectionBit(direction)) != 0;
    }

    public enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }
}
