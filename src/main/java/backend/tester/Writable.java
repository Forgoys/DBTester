package backend.tester;

import backend.dataset.TestAllResult;
import backend.dataset.TestResult;

import java.util.List;

public interface Writable {
    /**
     * 能够将测试过程中的时序数据以及测试结果写入文件中
     */
    public void writeToFile(String resultPath);

    /**
     *
     * @param resultPath 要读的文件夹位置
     * @return 测试结果对，测试结果和时序型结果
     */
    public TestAllResult readFromFile(String resultPath);
}
