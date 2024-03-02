package backend.dataset;

import java.util.ArrayList;
import java.util.List;

public abstract class TestResult extends AbstractDataSet{
    /**
     * TPCC测试结果名
     */
    public static final ArrayList<String> TPCC_RES_NAMES = new ArrayList<>();
    static {
        TPCC_RES_NAMES.add("QPH还是什么来着");
    }

    /**
     * TPCH测试结果名
     */
    public static final ArrayList<String> TPCH_RES_NAMES = new ArrayList<>();
    static {
        TPCH_RES_NAMES.add("每小时执行次数？");
    }


}
