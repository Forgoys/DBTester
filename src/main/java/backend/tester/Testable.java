package backend.tester;

import backend.dataset.TestResult;
import backend.dataset.TestTimeData;

public interface Testable {
    /**
     * 数据准备步骤，如果没有该步骤则不用实现
     */
    public void dataPrepare();

    /**
     * 在该方法中开始测试。
     * 开始测试前应检查测试状态 {@link TestItem#status}：测试是否就绪、运行中的测试不能重复运行。
     */
    public void startTest();

    /**
     * 当调用此方法时，此时采样得到的时序数据，并且将数据写入{@link TestItem#timeDataList}中。
     * 应检查测试状态 {@link TestItem#status}：只有运行中的测试可以返回当前时序数据
     */
    public void generateTimeData();

    public TestResult getTestResults();
}
