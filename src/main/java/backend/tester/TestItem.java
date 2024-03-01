package backend.tester;


import backend.dataset.TestArguments;
import backend.dataset.TestResult;
import backend.dataset.TestTimeData;
import org.example.dbtester.DBConnection;
import org.example.dbtester.SSHConnection;

import java.util.List;

/**
 * 测试项父类，各测试项需要继承该类
 */
public abstract class TestItem implements Testable, Writable{
    /**
     * 测试项目名称：TPC-C，TPC-H等
     */
    private String testName;

    public enum Status {
        /**
         * 准备阶段，例如参数未配置
          */
        UNPREPARED,
        /**
         * 就绪状态
         */
        READY,
        /**
         * 测试项正在运行
         */
        RUNNING,
        /**
         * 测试项已完成
         */
        FINISHED;
    }

    /**
     * 当前测试项的状态。你需要处理该状态的切换。
     */
    private Status status = Status.UNPREPARED;

    /**
     * ssh远程连接句柄，通过该句柄与远程服务器进行交互。
     * 如果某项测试需要用到该句柄，则需在子类中初始化该句柄。
     */
    private SSHConnection sshStmt;

    /**
     * 数据库连接句柄，通过该句柄与远程数据库进行交互。
     * 如果某项测试需要用到该句柄，则需在子类中初始化该句柄。
     */
    private DBConnection DBStmt;

    /**
     * 测试参数
     */
    private TestArguments testArgs;

    /**
     * 保存测试过程中的时序数据
     */
    private List<TestTimeData> timeDataList;

    /**
     * 返回测试过程中的时序数据
     * @return 返回一个存有时序数据的List
     */
    public List<TestTimeData> getTimeDataList() {
        return timeDataList;
    }

    /**
     * 保存测试结果
     */
    private TestResult testResult;

}
