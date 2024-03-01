package backend.tester;

import backend.dataset.TestResult;
import backend.dataset.TestTimeData;

public interface Testable {
    public void dataPrepare();

    public void startTest();

    public TestTimeData getTimeData();

    public TestResult getTestResults();
}
