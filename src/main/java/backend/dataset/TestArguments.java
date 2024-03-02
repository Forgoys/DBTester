package backend.dataset;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试参数抽象类，需要定义测试参数子类继承该类
 */
public class TestArguments {

    //*************************************常量值************************************************
    /**
     * TPCC测试参数属性
     */
    public static final ArgumentProperty[] TPCC_ARG_PROPERTIES = new ArgumentProperty[] {
            // 测试规模，有候选项
            new ArgumentProperty("数据规模", new String[]{"5","10","20"}),
            // 数据文件路径，没有候选项，需要输入
            new ArgumentProperty("测试路径")
    };


    /**
     * TPCH测试参数属性
     */
    public static final ArgumentProperty[] TPCH_ARG_PROPERTIES = new ArgumentProperty[] {
            // 测试规模，有候选项
            new ArgumentProperty("数据规模", new String[]{"1","2","4","8"}),
            // 数据文件路径，没有候选项，需要输入
            new ArgumentProperty("测试路径")
    };

    /**
     * 此处设置其余测试参数属性。每一个参数属性由参数名和可能存在的参数候选值组成。
     */




    /**
     * 各数据值，以String格式保存
     */
    public String[] values;

}
