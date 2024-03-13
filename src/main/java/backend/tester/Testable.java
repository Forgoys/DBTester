package backend.tester;

import backend.dataset.TestResult;

import java.io.IOException;
import java.util.List;


public interface Testable {

    /**
     * 测试工具或相关测试环境的部署、数据集的生成、数据导入数据库
     */
    void testEnvPrepare() throws Exception;

    /**
     * 在该方法中开始测试。
     * 开始测试前应检查测试状态 {@link TestItem#status}：测试是否就绪、运行中的测试不能重复运行。
     */
    void startTest() throws IOException, InterruptedException;

    /**
     * 当调用此方法时，将采样到的CPU利用率、内存占用率、磁盘读速度、磁盘写速度（数据库）或者IOPS、读带宽、写带宽、读延迟、写延迟（文件系统的可靠性测试）
     * 返回的是一个n行m列的二维数组，n是指标个数，m是该指标的所有结果
     */
    List<List<Double>> getTimeData();

    /**
     * 该方法返回格式化的结果
     */
    TestResult getTestResults();
}
