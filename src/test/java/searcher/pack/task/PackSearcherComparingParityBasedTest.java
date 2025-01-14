package searcher.pack.task;

import common.datastore.PieceCounter;
import common.datastore.blocks.LongPieces;
import common.datastore.blocks.Pieces;
import common.iterable.CombinationIterable;
import common.parser.BlockInterpreter;
import concurrent.ILockedReachableThreadLocal;
import core.column_field.ColumnField;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.srs.MinoRotation;
import entry.common.kicks.factory.SRSMinoRotationFactory;
import org.junit.jupiter.api.Test;
import searcher.pack.InOutPairField;
import searcher.pack.SeparableMinos;
import searcher.pack.SizedBit;
import searcher.pack.calculator.BasicSolutions;
import searcher.pack.memento.SolutionFilter;
import searcher.pack.memento.UsingBlockAndValidKeySolutionFilter;
import searcher.pack.mino_fields.RecursiveMinoFields;
import searcher.pack.solutions.BasicSolutionsCalculator;
import searcher.pack.solutions.MappedBasicSolutions;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PackSearcherComparingParityBasedTest {
    private static class TestData {
        private final Pieces pieces;
        private final long count;

        private TestData(Pieces pieces, long count) {
            this.pieces = pieces;
            this.count = count;
        }

        public List<Piece> getPieces() {
            return pieces.getPieces();
        }

        public long getCount() {
            return count;
        }
    }

    private static final int FIELD_WIDTH = 10;

    // 高さ4: パリティベースとの探索結果を比較する (同一ミノは2つまで)
    @Test
    void testAllSRSValidPacksHeight4() throws Exception {
        int width = 3;
        int height = 4;

        String resultPath = ClassLoader.getSystemResource("perfects/pack_height4.txt").getPath();
        List<String> lines = Files.readAllLines(Paths.get(resultPath));
        Collections.shuffle(lines);
        List<TestData> testCases = lines.subList(0, 50).stream()
                .map(line -> line.split("//")[0])
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> line.split("="))
                .map(split -> {
                    Stream<Piece> blocks = BlockInterpreter.parse(split[0]);
                    LongPieces pieces = new LongPieces(blocks);
                    int count = Integer.parseInt(split[1]);
                    return new TestData(pieces, count);
                })
                .collect(Collectors.toList());

        compareCount(width, height, testCases);
    }

    // 高さ5: パリティベースとの探索結果を比較する (同一ミノは2つまで)
    @Test
    void testAllSRSValidPacksHeight5() throws Exception {
        int width = 2;
        int height = 5;

        String resultPath = ClassLoader.getSystemResource("perfects/pack_height5.txt").getPath();
        List<TestData> testCases;
        try (Stream<String> lines = Files.lines(Paths.get(resultPath))) {
            testCases = lines
                    .map(line -> line.split("//")[0])
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(line -> line.split("="))
                    .map(split -> {
                        Stream<Piece> blocks = BlockInterpreter.parse(split[0]);
                        LongPieces pieces = new LongPieces(blocks);
                        int count = Integer.parseInt(split[1]);
                        return new TestData(pieces, count);
                    })
                    .collect(Collectors.toList());
        }

        compareCount(width, height, testCases);
    }

    // 高さ6: パリティベースとの探索結果を比較する (同一ミノは2つまで)
    @Test
    void testAllSRSValidPacksHeight6() throws Exception {
        int width = 2;
        int height = 6;

        String resultPath = ClassLoader.getSystemResource("perfects/pack_height6.txt").getPath();
        List<String> lines = Files.readAllLines(Paths.get(resultPath));
        Collections.shuffle(lines);
        List<TestData> testCases = lines.subList(0, 30).stream()
                .map(line -> line.split("//")[0])
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> line.split("="))
                .map(split -> {
                    Stream<Piece> blocks = BlockInterpreter.parse(split[0]);
                    LongPieces pieces = new LongPieces(blocks);
                    int count = Integer.parseInt(split[1]);
                    return new TestData(pieces, count);
                })
                .collect(Collectors.toList());

        compareCount(width, height, testCases);
    }

    private void compareCount(int width, int height, List<TestData> testDataList) throws InterruptedException, ExecutionException {
        SizedBit sizedBit = new SizedBit(width, height);
        SeparableMinos separableMinos = createSeparableMinos(sizedBit);
        BasicSolutionsCalculator calculator = new BasicSolutionsCalculator(separableMinos, sizedBit);
        Map<ColumnField, RecursiveMinoFields> calculate = calculator.calculate();
        Supplier<MinoRotation> minoRotationSupplier = SRSMinoRotationFactory::createDefault;

        for (TestData data : testDataList) {
            // 準備
            List<Piece> usingPieces = data.getPieces();
            int popCount = usingPieces.size();
            Field initField = createSquareEmptyField(height, popCount);

            // packで探索
            Set<PieceCounter> pieceCounters = Collections.singleton(new PieceCounter(usingPieces));
            SolutionFilter solutionFilter = createUsingBlockAndValidKeyMementoFilter(minoRotationSupplier, initField, sizedBit, pieceCounters);
            BasicSolutions basicSolutions = new MappedBasicSolutions(calculate, solutionFilter);
            long packCounter = calculateSRSValidCount(sizedBit, basicSolutions, initField, solutionFilter);

            assertThat(packCounter).isEqualTo(data.getCount());
        }
    }

    private static SeparableMinos createSeparableMinos(SizedBit sizedBit) {
        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        return SeparableMinos.createSeparableMinos(minoFactory, minoShifter, sizedBit);
    }

    private long calculateSRSValidCount(SizedBit sizedBit, BasicSolutions basicSolutions, Field initField, SolutionFilter solutionFilter) throws InterruptedException, ExecutionException {
        // フィールドの変換
        int width = sizedBit.getWidth();
        int height = sizedBit.getHeight();
        List<InOutPairField> inOutPairFields = InOutPairField.createInOutPairFields(width, height, initField);

        // 探索準備
        TaskResultHelper taskResultHelper = createTaskResultHelper(sizedBit);
        PerfectPackSearcher searcher = new PerfectPackSearcher(inOutPairFields, basicSolutions, sizedBit, solutionFilter, taskResultHelper);

        // 探索
        return searcher.count();
    }

    private TaskResultHelper createTaskResultHelper(SizedBit sizedBit) {
        if (sizedBit.getWidth() == 4 && sizedBit.getWidth() == 3)
            return new Field4x10MinoPackingHelper();
        return new BasicMinoPackingHelper();
    }

    // パフェするまでに有効なブロック数を列挙する
    private SolutionFilter createUsingBlockAndValidKeyMementoFilter(Supplier<MinoRotation> minoRotationSupplier, Field initField, SizedBit sizedBit, Set<PieceCounter> counters) {
        HashSet<Long> validBlockCounters = new HashSet<>();

        for (PieceCounter counter : counters) {
            List<Piece> usingPieces = counter.getBlocks();
            for (int size = 1; size <= usingPieces.size(); size++) {
                CombinationIterable<Piece> combinationIterable = new CombinationIterable<>(usingPieces, size);
                for (List<Piece> pieces : combinationIterable) {
                    PieceCounter newCounter = new PieceCounter(pieces);
                    validBlockCounters.add(newCounter.getCounter());
                }
            }
        }

        ILockedReachableThreadLocal reachableThreadLocal = new ILockedReachableThreadLocal(minoRotationSupplier, sizedBit.getHeight(), false);
        return new UsingBlockAndValidKeySolutionFilter(initField, validBlockCounters, reachableThreadLocal, sizedBit);
    }

    private Field createSquareEmptyField(int emptyHeight, int popCount) {
        int needBlock = 4 * popCount;
        int emptyWidth = (needBlock - 1) / emptyHeight + 1;
        Field field = FieldFactory.createField(emptyHeight);
        for (int x = emptyWidth; x < FIELD_WIDTH; x++)
            for (int y = 0; y < emptyHeight; y++)
                field.setBlock(x, y);

        for (int y = 0; y < emptyHeight * emptyWidth - needBlock; y++)
            field.setBlock(0, y);
        return field;
    }
}