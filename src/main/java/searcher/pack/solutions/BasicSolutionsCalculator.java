package searcher.pack.solutions;

import searcher.pack.*;
import common.buildup.BuildUp;
import common.datastore.OperationWithKey;
import core.action.reachable.OnGrandOnlyReachable;
import core.column_field.ColumnField;
import core.column_field.ColumnSmallField;
import core.field.Field;
import core.field.SmallField;
import pack.separable_mino.SeparableMino;

import java.util.*;

/**
 * マルチスレッド非対応
 */
public class BasicSolutionsCalculator {
    public static final int FIELD_WIDTH = 10;

    private final SizedBit sizedBit;
    private final BasicReference reference;
    private final OnGrandOnlyReachable grandOnlyReachable = new OnGrandOnlyReachable();

    private HashMap<ColumnField, Set<MinoField>> resultsMap = new HashMap<>();
    private LinkedList<SeparableMino> minos = new LinkedList<>();
    private HashSet<MinoField> results = new HashSet<>();
    private SmallField wallField = new SmallField();

    public BasicSolutionsCalculator(SeparableMinos separableMinos, SizedBit sizedBit) {
        assert sizedBit.getHeight() <= 10;
        this.sizedBit = sizedBit;
        this.reference = new BasicReference(sizedBit, separableMinos);
    }

    public Map<ColumnField, Set<MinoField>> calculate() {
        List<ColumnSmallField> basicFields = reference.getBasicFields();
        return calculateResults(basicFields);
    }

    private HashMap<ColumnField, Set<MinoField>> calculateResults(List<ColumnSmallField> basicFields) {
        this.resultsMap = new HashMap<>();
        for (ColumnField columnField : basicFields) {
            HashSet<MinoField> results = calculate(columnField);
            resultsMap.put(columnField, results);
        }
        return this.resultsMap;
    }

    private HashSet<MinoField> calculate(ColumnField columnField) {
        // 初期化
        this.minos = new LinkedList<>();
        this.results = new HashSet<>();
        this.wallField = createWallField(columnField);

        ColumnSmallField initOuterField = new ColumnSmallField();
        calculateResult(columnField, initOuterField);
        return results;
    }

    // innerと探索に関係ないブロックが埋まっているフィールド
    private SmallField createWallField(ColumnField columnField) {
        Field innerField = reference.parseInnerField(columnField);

        SmallField wallField = new SmallField();
        // 横向きIをおいたとき、3ブロック分あふれる
        for (int y = 0; y < sizedBit.getHeight(); y++)
            for (int x = sizedBit.getWidth() + 3; x < FIELD_WIDTH; x++)
                wallField.setBlock(x, y);
        wallField.merge(innerField);

        return wallField;
    }

    // columnField = inner + outer
    // outerColumnField = outer only
    private void calculateResult(ColumnField columnField, ColumnField outerColumnField) {
        ColumnFieldConnections connections = reference.getConnections(columnField);

        // 全てが埋まったとき、それまでの手順を解とする
        if (ColumnFieldConnections.isFilled(connections)) {
            // これからブロックをおく場所以外を、すでにブロックで埋めたフィールドを作成
            Field freeze = wallField.freeze(sizedBit.getHeight());
            Field invertedOuterField = reference.parseInvertedOuterField(outerColumnField);
            freeze.merge(invertedOuterField);

            List<OperationWithKey> operations = toOperationWithKeys(minos);

            // 置くブロック以外がすでに埋まっていると仮定したとき、正しく接着できる順があるか確認
            if (existsValidBuildPattern(freeze, operations))
                recordResult(operations, outerColumnField.freeze(sizedBit.getHeight()));

            return;
        }

        // すでに探索済みのフィールドなら、その情報を利用する
        Set<MinoField> minoFieldSet = resultsMap.getOrDefault(columnField, null);
        if (minoFieldSet != null) {
            for (MinoField minoField : minoFieldSet) {
                // outerで、最終的に使用されるブロック と すでに使っているブロックが重ならないことを確認
                ColumnField lastOuterField = minoField.getOuterField();
                if (lastOuterField.canMerge(outerColumnField)) {
                    List<OperationWithKey> operations = toOperationWithKeys(this.minos);
                    operations.addAll(minoField.getOperations());

                    // 使用されるブロックを算出
                    ColumnField usingBlock = lastOuterField.freeze(sizedBit.getHeight());
                    usingBlock.merge(outerColumnField);

                    // これからブロックをおく場所以外を、すでにブロックで埋めたフィールドを作成
                    Field freeze = wallField.freeze(sizedBit.getHeight());
                    Field invertedOuterField = reference.parseInvertedOuterField(usingBlock);
                    freeze.merge(invertedOuterField);

                    // 置くブロック以外がすでに埋まっていると仮定したとき、正しく接着できる順があるか確認
                    if (existsValidBuildPattern(freeze, operations))
                        recordResult(operations, usingBlock);
                }
            }

            return;
        }

        // まだ探索したことのないフィールドのとき
        // innerに対しておける可能性がある手順を取得
        for (ColumnFieldConnection connection : connections.getConnections()) {
            // outerで重なりがないか確認する
            ColumnField nextOuterField = connection.getOuterField();
            if (nextOuterField.canMerge(outerColumnField)) {
                // フィールドとミノ順を進める
                minos.addLast(connection.getMino());
                nextOuterField.merge(outerColumnField);

                // 新しいフィールドを基に探索
                ColumnField innerField = connection.getInnerField();
                calculateResult(innerField, nextOuterField);

                // フィールドとミノ順を戻す
                nextOuterField.reduce(outerColumnField);
                minos.pollLast();
            }
        }
    }

    private List<OperationWithKey> toOperationWithKeys(List<SeparableMino> minos) {
        ArrayList<OperationWithKey> operations = new ArrayList<>();
        for (SeparableMino mino : minos) {
            operations.add(mino.toOperation());
        }
        return operations;
    }

    // 置くブロック以外がすでに埋まっていると仮定したとき、正しく接着できる順があるか確認
    // ただし、フィールドをブロックで埋めると回転入れなどができない場合があるため、判定は下に地面があるかだけを判定
    // (部分的には回転入れできなくても、左右のSolutionパターン次第では入れられる可能性がある)
    private boolean existsValidBuildPattern(Field freeze, List<OperationWithKey> operations) {
        return BuildUp.existsValidBuildPattern(freeze, operations, sizedBit.getHeight(), grandOnlyReachable);
    }

    private void recordResult(List<OperationWithKey> operations, ColumnField outerField) {
        MinoField result = new MinoField(operations, outerField, sizedBit.getHeight());
        results.add(result);
    }
}