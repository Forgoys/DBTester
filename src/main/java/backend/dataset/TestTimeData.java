package backend.dataset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestTimeData {

    //*************************************常量值************************************************
    /**
     * 包含日期时间、CPU使用率、内存使用率和测试速度四项时序数据名称
     */
    public static final String[] NORMAL_TIMEDATA_NAMES = new String[]{"CPU使用率", "内存使用率", "磁盘读速度", "磁盘写速度"};

    /**
     * 添加其余测试过程中的时序数据名称，保持一致
     * NORMAL_TIMEDATA_NAMES
     */

    // 文件系统可靠性测试的时序数据
    public static final String[] FS_RELIABLE_TIMEDATA_NAMES = new String[]{"IOPS", "Bandwidth", "Latency"};


    public String[] names;

    /**
     * 各数据值，以String格式保存
     */
//    public String[] values;
    public List<List<Double>> values;
}

