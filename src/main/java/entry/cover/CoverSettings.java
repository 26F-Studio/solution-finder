package entry.cover;

import entry.DropType;
import exceptions.FinderParseException;

import java.util.Collections;
import java.util.List;

public class CoverSettings {
    private static final String DEFAULT_LOG_FILE_PATH = "output/last_output.txt";
    private static final String DEFAULT_OUTPUT_BASE_FILE_PATH = "output/seq.csv";

    private String logFilePath = DEFAULT_LOG_FILE_PATH;
    private String outputBaseFilePath = DEFAULT_OUTPUT_BASE_FILE_PATH;
    private List<String> patterns = Collections.emptyList();
    private List<CoverParameter> parameters;
    private DropType dropType = DropType.Softdrop;
    private boolean isUsingHold = true;

    // ********* Getter ************
    boolean isUsingHold() {
        return isUsingHold;
    }

    List<CoverParameter> getParameters() {
        return parameters;
    }

    List<String> getPatterns() {
        return patterns;
    }

    String getLogFilePath() {
        return logFilePath;
    }

    String getOutputBaseFilePath() {
        return outputBaseFilePath;
    }

    DropType getDropType() {
        return dropType;
    }

    // ********* Setter ************
    void setUsingHold(Boolean isUsingHold) {
        this.isUsingHold = isUsingHold;
    }

    void setLogFilePath(String path) {
        this.logFilePath = path;
    }

    void setOutputBaseFilePath(String path) {
        this.outputBaseFilePath = path;
    }

    void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    boolean isOutputToConsole() {
        return true;
    }

    public void setParameters(List<CoverParameter> parameters) {
        this.parameters = parameters;
    }

    void setDropType(String type) throws FinderParseException {
        switch (type.trim().toLowerCase()) {
            case "soft":
            case "softdrop":
                this.dropType = DropType.Softdrop;
                return;
            case "hard":
            case "harddrop":
                this.dropType = DropType.Harddrop;
                return;
            default:
                throw new FinderParseException("Unsupported droptype: type=" + type);
        }
    }
}