package backend.dataset;

import java.util.ArrayList;
import java.util.List;

public class TestResult {

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

    // FIO读写速度测试结果名
    public static final String[] FIO_RW_TEST = new String[]{"IOPS", "Bandwidth", "Latency"};

    // IOZONE读写速度测试结果名
    public static final String[] IOZONE_RW_TEST = new String[]{"write", "rewrite", "read", "reread", "read (backward)", "write (random)"};

    // FIO并发度测试结果名
    public static final String[] FIO_PARALLEL_TEST = new String[]{"IOPS", "Bandwidth", "Latency"};


    /**
     * 数据名称
     */
    public String[] names;

    /**
     * 各数据值，以String格式保存
     */
    public String[] values;

}
