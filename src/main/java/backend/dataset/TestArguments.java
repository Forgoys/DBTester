package backend.dataset;

import java.util.ArrayList;
import java.util.List;

public class TestArguments {
    public List<DataPair> testArgs;

    public static final TestArguments TPCC_ARGUMENTS = new TestArguments();

    {
        TPCC_ARGUMENTS.testArgs = new ArrayList<>();
        TPCC_ARGUMENTS.testArgs.add(new DataPair("测试规模"));
    }

    public static final TestArguments TPCH_ARGUMENTS = TPCC_ARGUMENTS;





    public TestArguments(){}

    public TestArguments(List<DataPair> testArgs) {
        this.testArgs = testArgs;
    }
}
