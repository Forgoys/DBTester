package backend.dataset;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试参数抽象类，需要定义测试参数子类继承该类
 */
public abstract class TestArguments extends AbstractDataSet{

    /**
     * TPCC测试参数名
     */
    public static final ArrayList<String> TPCC_ARGS_NAMES = new ArrayList<>();
    static {
        TPCC_ARGS_NAMES.add("数据规模");
    }

    /**
     * TPCH测试参数名
     */
    public static final ArrayList<String> TPCH_ARGS_NAMES = new ArrayList<>();
    static {
        TPCH_ARGS_NAMES.add("数据规模");
    }

    /**
     * 可靠性测试参数名
     */

    /**
     * 此处设置其余测试参数名
     */

}
