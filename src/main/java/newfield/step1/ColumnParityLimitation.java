package newfield.step1;

import core.mino.Block;
import newfield.BlockCounter;
import newfield.ParityField;

import java.util.ArrayList;
import java.util.List;

// 各ブロックの個数から、可能性のある偶奇数列の変化量をすべて列挙
public class ColumnParityLimitation {
    private final BlockCounter blockCounter;
    private final ParityField parityField;
    private final int maxClearLine;
    private final List<EstimateBuilder> builders = new ArrayList<>();

    public ColumnParityLimitation(BlockCounter blockCounter, ParityField parityField, int maxClearLine) {
        this.blockCounter = blockCounter;
        this.parityField = parityField;
        this.maxClearLine = maxClearLine;
    }

    public List<EstimateBuilder> enumerate() {
        assert builders.isEmpty();

        int evenParity = maxClearLine * 5 - parityField.calculateEvenColumnParity();  // 奇数列
        int oddParity = maxClearLine * 5 - parityField.calculateOddColumnParity();   // 偶数列

        // 必要以上にミノを入れていないかチェックするassert
        assert blockCounter.getAllBlock() * 4 == evenParity + oddParity;

        // SZO対応: どの置き方でも 2:2 で減少
        int SZOCount = blockCounter.getCount(Block.S) + blockCounter.getCount(Block.Z) + blockCounter.getCount(Block.O);
        evenParity -= 2 * SZOCount;
        oddParity -= 2 * SZOCount;

        assert 0 <= evenParity && 0 <= oddParity;

        // LJ対応: どの置き方でも 3:1 で減少
        // 最低でも LJCount　減少し、最大で LJCount + LJCount * 2 減少
        int LJCount = blockCounter.getCount(Block.L) + blockCounter.getCount(Block.J);
        for (int count = -LJCount; count <= LJCount; count++) {
            int oddLJ = 2 * LJCount + count;
            int evenLJ = 2 * LJCount - count;
            int leastCount = LJCount - (0 <= count ? count : -count);
            if (leastCount % 2 == 0)
                enumerateAfterSZOLJ(evenParity - oddLJ, oddParity - evenLJ, count);
        }

        return builders;
    }

    private void enumerateAfterSZOLJ(int evenParity, int oddParity, int oddCountLJ) {
        // Tは必ず1以上減少するため、0の場合は終了
        if (oddParity <= 0 || evenParity <= 0)
            return;

        // T対応: 横3:1,1:3 と 縦:2:2 の3種
        int TCount = blockCounter.getCount(Block.T);
        for (int count = -TCount; count <= TCount; count++) {
            int oddT = 2 * TCount + count;
            int evenT = 2 * TCount - count;
            enumerateAfterSZOLJT(evenParity - oddT, oddParity - evenT, oddCountLJ, count);
        }
    }

    private void enumerateAfterSZOLJT(int evenParity, int oddParity, int oddCountLJ, int oddCountT) {
        // Iは2 or 4ずつ減少するので、奇数の場合は終了
        // 縦置きの場合は減少しないため、0の場合は継続
        if (oddParity < 0 || evenParity < 0 || oddParity % 2 != 0 || evenParity % 2 != 0)
            return;

        // I対応: 横2:2 と 縦:4:0,0:4 の3種
        int ICount = blockCounter.getCount(Block.I);
        for (int count = -ICount; count <= ICount; count++) {
            int oddI = 2 * (ICount + count);
            int evenI = 2 * (ICount - count);

            // パリティが正しいときは記録する
            if (evenParity - oddI == 0 && oddParity - evenI == 0)
                enumerateAfterAll(oddCountLJ, oddCountT, count);
        }
    }

    private void enumerateAfterAll(int oddCountLJ, int oddCountT, int oddCountI) {
        // 偶奇数列の差を相殺する置き方に注意
        // oddCountLJ: 偶数列を3にするLJの置き方の総数 // マイナスの場合は、奇数列を3にするLJの置き方の総数
        // oddCountT : 偶数列を3にするTの置き方の総数  // マイナスの場合は、奇数列を3にするTの置き方の総数
        // oddCountI : 偶数列を4にするIの置き方の総数  // マイナスの場合は、奇数列を4にするIの置き方の総数

        EstimateBuilder estimateBuilder = new EstimateBuilder(blockCounter, oddCountLJ, oddCountT, oddCountI);
        builders.add(estimateBuilder);
    }
}
