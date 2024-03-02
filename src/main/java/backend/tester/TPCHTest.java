package backend.tester;

import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.dataset.TestTimeData;
import org.example.dbtester.DBConnection;
import org.example.dbtester.SSHConnection;

public class TPCHTest extends TestItem{

    public TPCHTest(String testName, SSHConnection sshStmt, DBConnection DBStmt) {
        super(testName, sshStmt, DBStmt);
    }

    public TPCHTest(String testName, SSHConnection sshStmt, DBConnection DBStmt, TestArguments testArgs) {
        super(testName, sshStmt, DBStmt, testArgs);
    }

    @Override
    public void dataPrepare() {

    }

    @Override
    public void startTest() {

    }

    @Override
    public TestTimeData getCurTimeData() {
        return null;
    }


    @Override
    public TestResult getTestResults() {
        return null;
    }

    @Override
    public void writeToFile() {

    }

    @Override
    public void readFromFile() {

    }
}
