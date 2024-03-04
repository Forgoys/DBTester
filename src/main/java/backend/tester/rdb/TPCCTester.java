package backend.tester.rdb;

import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.tester.TestItem;
import frontend.connection.DBConnection;
import frontend.connection.SSHConnection;

public class TPCCTester extends TestItem {

    public static final String dataSetPath = "./TPCC_data";

    public TPCCTester(String testName, SSHConnection sshStmt, DBConnection DBStmt) {
        super(testName, sshStmt, DBStmt);
    }

    public TPCCTester(String testName, SSHConnection sshStmt, DBConnection DBStmt, TestArguments testArgs) {
        super(testName, sshStmt, DBStmt, testArgs);
    }

    @Override
    public void dataPrepare() {
//        if(this.test)
//        if(dataSetPath)
    }

    @Override
    public void startTest() {

    }

    @Override
    public void generateTimeData() {

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
