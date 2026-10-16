package studio.fantasyit.ether_craft.menu.factory.chip;

import studio.fantasyit.ether_craft.recipe.factory.PathNode;

/**
 * 通道箭头判定。
 *
 * <p>纯计算：不依赖任何客户端类，GameTest 可直接构造数组覆盖。
 *
 * <p>数据来自 {@code EtherProcessFactoryEntity} 的渲染用数组：
 * <ul>
 *     <li>{@code belonging[row][col]}：该格所属通道号，-1 表示不在任何已成型的通道上；</li>
 *     <li>{@code pathDepth[row][col]}：该格在通道中的编号。物品从左侧输入口流入、从右侧输出口流出，
 *     pathDepth 沿前进方向递增，因此编号更小的邻居在本格上游。</li>
 * </ul>
 *
 * <p>规则：通道成型且邻居属于同一通道时，邻居编号更小 -&gt; 入（物品从该方向流入本格），
 * 更大 -&gt; 出；通道不成型、邻居越界或不在同一通道 -&gt; 封闭。
 */
public final class ChipChannelArrows {
    public enum ArrowKind {
        /** 封闭 */
        CLOSED,
        /** 入 */
        IN,
        /** 出 */
        OUT
    }

    private ChipChannelArrows() {
    }

    /**
     * 返回四个方向的箭头类型，顺序与 {@link PathNode.Direction} 的序一致：上、下、左、右。
     */
    public static ArrowKind[] edges(int[][] belonging, int[][] pathDepth, int row, int col) {
        ArrowKind[] result = new ArrowKind[PathNode.Direction.values().length];
        int self = belonging[row][col];
        int selfDepth = pathDepth[row][col];
        for (PathNode.Direction direction : PathNode.Direction.values()) {
            result[direction.ordinal()] = edge(belonging, pathDepth, row, col, direction, self, selfDepth);
        }
        return result;
    }

    private static ArrowKind edge(int[][] belonging, int[][] pathDepth, int row, int col,
                                  PathNode.Direction direction, int self, int selfDepth) {
        if (self < 0) return ArrowKind.CLOSED;
        int rowOffset = direction == PathNode.Direction.UP ? -1 : direction == PathNode.Direction.DOWN ? 1 : 0;
        int colOffset = direction == PathNode.Direction.LEFT ? -1 : direction == PathNode.Direction.RIGHT ? 1 : 0;
        int neighborRow = row + rowOffset;
        int neighborCol = col + colOffset;
        if (neighborRow < 0 || neighborRow >= belonging.length) return ArrowKind.CLOSED;
        if (neighborCol < 0 || neighborCol >= belonging[neighborRow].length) return ArrowKind.CLOSED;
        if (belonging[neighborRow][neighborCol] != self) return ArrowKind.CLOSED;
        if (pathDepth[neighborRow][neighborCol] < selfDepth) return ArrowKind.IN;
        if (pathDepth[neighborRow][neighborCol] > selfDepth) return ArrowKind.OUT;
        return ArrowKind.CLOSED;
    }
}
