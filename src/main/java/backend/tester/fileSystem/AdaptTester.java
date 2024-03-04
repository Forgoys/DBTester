package backend.tester.fileSystem;

import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.tester.TestItem;
import frontend.connection.DBConnection;
import frontend.connection.SSHConnection;

// 文件系统适配性测试 通过挂载 暂时不写
public class AdaptTester extends TestItem {

    public AdaptTester(String testName, SSHConnection sshStmt, DBConnection DBStmt) {
        super(testName, sshStmt, DBStmt);
    }

    public AdaptTester(String testName, SSHConnection sshStmt, DBConnection DBStmt, TestArguments testArgs) {
        super(testName, sshStmt, DBStmt, testArgs);
    }

    // 文件系统适配性测试环境部署
    @Override
    public void testEnvPrepare() {
        // 默认文件系统已经存在 提供文件系统接口 10.181.8.145:/gv1
    }

    // 开始适配性测试 进行挂载
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
