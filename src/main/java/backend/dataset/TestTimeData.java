package backend.dataset;

import java.util.ArrayList;
import java.util.List;

public class TestTimeData {
    public List<DataPair> timeData;

    public static final TestTimeData NORMAL_TIMEDATA = new TestTimeData();

    {
        NORMAL_TIMEDATA.timeData = new ArrayList<>();
        NORMAL_TIMEDATA.timeData.add(new DataPair("CPU使用率"));
        NORMAL_TIMEDATA.timeData.add(new DataPair("内存使用率"));
        NORMAL_TIMEDATA.timeData.add(new DataPair("测试速度"));
    }

    public TestTimeData() {}

    public TestTimeData(List<DataPair> timeData) {
        this.timeData = timeData;
    }
}
