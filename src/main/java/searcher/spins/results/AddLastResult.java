package searcher.spins.results;

import common.datastore.PieceCounter;
import core.field.Field;
import core.neighbor.SimpleOriginalPiece;

import java.util.stream.Stream;

public class AddLastResult extends Result {
    public static AddLastResult create(Result prev, SimpleOriginalPiece operation) {
        PieceCounter reminderPieceCounter = prev.getRemainderPieceCounter().removeAndReturnNew(PieceCounter.getSinglePieceCounter(operation.getPiece()));

        // すでに使われているブロックを計算
        Field usingField = prev.freezeUsingField();
        usingField.merge(operation.createMinoField(usingField.getMaxFieldHeight()));

        return new AddLastResult(prev, operation, reminderPieceCounter, usingField);
    }

    private final Result prev;
    private final SimpleOriginalPiece operation;
    private final PieceCounter reminderPieceCounter;
    private final Field usingField;

    private AddLastResult(Result prev, SimpleOriginalPiece operation, PieceCounter reminderPieceCounter, Field usingField) {
        this.prev = prev;
        this.operation = operation;
        this.reminderPieceCounter = reminderPieceCounter;
        this.usingField = usingField;
    }

    @Override
    public Field getInitField() {
        return prev.getInitField();
    }

    @Override
    public Field getUsingField() {
        return usingField;
    }

    @Override
    public Field getAllMergedField() {
        Field field = freezeInitField();
        field.merge(getUsingField());
        return field;
    }

    @Override
    public PieceCounter getRemainderPieceCounter() {
        return reminderPieceCounter;
    }

    @Override
    public Stream<SimpleOriginalPiece> operationStream() {
        return Stream.concat(prev.operationStream(), Stream.of(operation));
    }

    @Override
    public int getNumOfUsingPiece() {
        return prev.getNumOfUsingPiece() + 1;
    }

    public SimpleOriginalPiece getCurrentOperation() {
        return operation;
    }
}
