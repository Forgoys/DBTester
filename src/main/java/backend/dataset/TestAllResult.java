package backend.dataset;

import java.util.List;

public class TestAllResult {
    public TestResult testResult;
    public List<List<Double>> timeDataResult;

    public TestAllResult() {}

    public TestAllResult(TestResult testResult) {
        this.testResult = testResult;
        this.timeDataResult = null;
    }
    public TestAllResult(List<List<Double>> timeDataResult) {
        this.testResult = null;
        this.timeDataResult = timeDataResult;
    }

    public TestAllResult(TestResult testResult, List<List<Double>> timeDataResult) {
        this.testResult = testResult;
        this.timeDataResult = timeDataResult;
    }
}
