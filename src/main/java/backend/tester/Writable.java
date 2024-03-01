package backend.tester;

public interface Writable {
    /**
     * 能够将测试过程中的时序数据以及测试结果写入文件中
     */
    public void writeToFile();

    /**
     * 能够从文件中读取测试过程中的时序数据以及测试结果
     */
    public void readFromFile();
}
