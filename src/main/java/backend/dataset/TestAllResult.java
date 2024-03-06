package backend.dataset;

import java.util.List;

public class TestAllResult {
    public TestArguments testArguments;
    public List<List<String>> timeDataResult;

    public TestAllResult() {}

    public TestAllResult(TestArguments testArguments) {
        this.testArguments = testArguments;
        this.timeDataResult = null;
    }
    public TestAllResult(List<List<String>> timeDataResult) {
        this.testArguments = null;
        this.timeDataResult = timeDataResult;
    }

    public TestAllResult(TestArguments testArguments, List<List<String>> timeDataResult) {
        this.testArguments = testArguments;
        this.timeDataResult = timeDataResult;
    }
}
