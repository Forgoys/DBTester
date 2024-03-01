package backend.dataset;

import java.util.ArrayList;
import java.util.Collections;

public abstract class TestTimeData extends AbstractDataSet{


    /**
     * 包含日期时间、CPU使用率、内存使用率和测试速度四项时序数据名称
     */
    public static final ArrayList<String> NORMAL_TIMEDATA_NAMES = new ArrayList<>();
    static {
        NORMAL_TIMEDATA_NAMES.add("日期时间");
        NORMAL_TIMEDATA_NAMES.add("CPU使用率");
        NORMAL_TIMEDATA_NAMES.add("内存使用率");
        NORMAL_TIMEDATA_NAMES.add("测试速度");
    }

    /**
     * 添加其余测试过程中的时序数据名称
     */


}
