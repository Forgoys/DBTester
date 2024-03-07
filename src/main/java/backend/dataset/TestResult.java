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
     * 压力测试结果名
     */
    public static final String[] PRESSURE_TEST_RES_NAMES = new String[]{"平均线程发送请求数", "平均失败数"};

    /**
     * influxdb-comparision工具时序数据库读写测试结果
     */
    public static final String[] INFLUXCOMP_WRTIE_RES_NAMES = new String[]{"写入条目数","写入时间","客户端数","写入数据点速度","写入数据值速度","磁盘速度"};
    public static final String[] INFLUXCOMP_READ_RES_NAMES = new String[]{"最小查询时间","平均查询时间","最大查询时间"};
    public static final String[] INFLUXCOMP_PRESS_RES_NAMES1 = new String[]{"成功次数","失败次数"};
    // FIO读写速度测试结果名
    public static final String[] FIO_RW_TEST = new String[]{"读取IOPS", "读取带宽(KiB/s)", "读取时延(usec)","写入IOPS", "写入带宽(KiB/s)", "写入时延(usec)"};

    // IOZONE读写速度测试结果名
    public static final String[] IOZONE_RW_TEST = new String[]{"write", "rewrite", "read", "reread", "read (backward)", "write (random)"};

    // FIO并发度测试结果名
    public static final String[] FIO_PARALLEL_TEST = new String[]{"读取IOPS", "读取带宽(KiB/s)", "读取时延(usec)","写入IOPS", "写入带宽(KiB/s)", "写入时延(usec)"};

    // FIO小文件测试结果名
    public static final String[] FIO_MINIFILE_TEST = new String[]{"读取IOPS", "读取带宽(KiB/s)", "读取时延(usec)","写入IOPS", "写入带宽(KiB/s)", "写入时延(usec)"};

    /**
     * 数据名称
     */
    public String[] names;

    /**
     * 各数据值，以String格式保存
     */
    public String[] values;

}
