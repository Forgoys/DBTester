package backend.dataset;

import java.util.ArrayList;
import java.util.List;

public class TestResult {

    //*************************************常量值************************************************
    /**
     * TPCC测试结果名
     */
    public static final String[] TPCC_RES_NAMES = new String[]{"tpmC"};

    /**
     * TPCH测试结果名
     */
    public static final String[] TPCH_RES_NAMES = new String[]{"每小时执行次数"};

    /**
     * influxdb-comparision工具时序数据库读写测试结果
     */
    public static final String[] INFLUXCOMP_WRTIE_RES_NAMES = new String[]{"时序写入性能结果"};
    public static final String[] INFLUXCOMP_READ_RES_NAMES = new String[]{"时序查询性能结果"};

    // FIO读写速度测试结果名
    public static final String[] FIO_RW_TEST = new String[]{"readIOPS", "readBandwidth", "readLatency","writeIOPS", "writeBandwidth", "writeLatency"};

    // IOZONE读写速度测试结果名
    public static final String[] IOZONE_RW_TEST = new String[]{"write", "rewrite", "read", "reread", "read (backward)", "write (random)"};

    // FIO并发度测试结果名
    public static final String[] FIO_PARALLEL_TEST = new String[]{"readIOPS", "readBandwidth", "readLatency","writeIOPS", "writeBandwidth", "writeLatency"};

    // FIO小文件测试结果名
    public static final String[] FIO_MINIFILE_TEST = new String[]{"readIOPS", "readBandwidth", "readLatency","writeIOPS", "writeBandwidth", "writeLatency"};

    /**
     * 数据名称
     */
    public String[] names;

    /**
     * 各数据值，以String格式保存
     */
    public String[] values;

}
