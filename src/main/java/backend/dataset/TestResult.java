package backend.dataset;

import java.util.ArrayList;
import java.util.List;

public class TestResult{

    //*************************************常量值************************************************
    /**
     * TPCC测试结果名
     */
    public static final String[] TPCC_RES_NAMES = new String[]{"QpH"};

    /**
     * TPCH测试结果名
     */
    public static final String[] TPCH_RES_NAMES = new String[]{"每小时执行次数"};

    /**
     * 此处补充其余测试结果名
     */


    /**
     * 各数据值，以String格式保存
     */
    public List<String> values;

}
