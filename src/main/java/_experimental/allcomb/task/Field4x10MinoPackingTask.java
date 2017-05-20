package _experimental.allcomb.task;

import _experimental.allcomb.*;
import _experimental.allcomb.memento.MementoFilter;
import _experimental.allcomb.memento.MinoFieldMemento;
import common.datastore.OperationWithKey;
import core.mino.Block;
import core.mino.Mino;
import core.srs.Rotate;

import java.util.Collections;
import java.util.stream.Stream;

// フィールドの探索範囲が4x10のとき限定のTask。最後のパターンが決まっているため少し高速に動作
public class Field4x10MinoPackingTask implements TaskResultHelper {
    private static final MinoField LEFT_I_ONLY = new MinoField(
            Collections.singletonList(
                    new OperationWithKey(new Mino(Block.I, Rotate.Left), 0, 0L, 1074791425L, 0)
            ),
            new ColumnSmallField(),
            4
    );

    // 高さが4・最後の1列がのこる場合で、パフェできるパターンは2つしか存在しない
    @Override
    public Stream<Result> fixResult(ListUpSearcher searcher, MementoFilter mementoFilter, ColumnField lastOuterField, MinoFieldMemento nextMemento) {
        Bit bit = searcher.getBit();
        long board = lastOuterField.getBoard(0) >> bit.maxBit;
        if (board == bit.fillBoard) {
            if (mementoFilter.testLast(nextMemento))
                return Stream.of(createResult(nextMemento));
        } else if (board == 0b111111110000L) {
            MinoFieldMemento concatILeft = nextMemento.concat(LEFT_I_ONLY);
            if (mementoFilter.testLast(concatILeft))
                return Stream.of(createResult(nextMemento));
        }
        return Stream.empty();
    }

    private Result createResult(MinoFieldMemento memento) {
        return new Result(memento);
    }
}
