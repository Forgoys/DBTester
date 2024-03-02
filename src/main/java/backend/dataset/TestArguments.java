package backend.dataset;

import java.util.ArrayList;
import java.util.List;


class ArgumentProperty {
    /**
     * 参数名称
     */
    public String argNames;

    /**
     * 候选项的值
     */
    public String[] candidateValues = null;

    /**
     * 判断该参数是否有候选项
     */
    public boolean hasCandidate() {
        if(candidateValues == null) {
            return false;
        }
        return candidateValues.length != 0;
    }

    public ArgumentProperty(String argNames) {
        this.argNames = argNames;
    }

    public ArgumentProperty(String argNames, String[] candidateValues) {
        this.argNames = argNames;
        this.candidateValues = candidateValues;
    }
}

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
    public List<String> values;

}
