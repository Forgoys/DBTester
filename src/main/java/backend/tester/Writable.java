package backend.tester;

import backend.dataset.TestAllResult;
import backend.dataset.TestResult;

import java.util.List;

public interface Writable {

    /**
     * 获取当前测试的测试结果文件夹名，定义为关键参数+年-月-日-时-分-秒，关键参数的选择自行决定，能让用户分得清就行
     * @return 测试结果文件夹名
     */
    public String getResultDicName();
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

    // 新增一个专门用于读取时序型结果的方法
    public List<List<Double>> readFromFile1(String resultPath);
}
