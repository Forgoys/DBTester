package backend.tester;

import backend.dataset.TestResult;
import backend.dataset.TestTimeData;

import java.io.IOException;

public interface Testable {

    /**
     * 测试工具或相关测试环境的部署、数据集的生成、数据导入数据库
     */
    public void testEnvPrepare();

    /**
     * 在该方法中开始测试。
     * 开始测试前应检查测试状态 {@link TestItem#status}：测试是否就绪、运行中的测试不能重复运行。
     */
    public void startTest() throws IOException, InterruptedException;

    /**
     * 当调用此方法时，此时采样得到的时序数据，并且将数据写入{@link TestItem#timeDataList}中。
     * 应检查测试状态 {@link TestItem#status}：只有运行中的测试可以返回当前时序数据
     */
    public void generateTimeData();

    /**
     * 该方法返回格式化的结果
     */
    public TestResult getTestResults();
}
