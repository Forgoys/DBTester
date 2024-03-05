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
    public static final ArgumentProperty[] TPCC_ARG_PROPERTIES = new ArgumentProperty[]{
            // 测试规模，有候选项
            new ArgumentProperty("数据规模", new String[]{"20", "50", "100"}),
            new ArgumentProperty("并发连接数", new String[]{"8","16","32","64","128","256"}),
            new ArgumentProperty("加载进程数", new String[]{"1","2","4","8","16","32","64"}),
            new ArgumentProperty("运行时长(min)", new String[]{"1","5","10","20","30","40",
                    "50","60","70","80","90","100","110","120"})
    };


    /**
     * TPCH测试参数属性
     */
    public static final ArgumentProperty[] TPCH_ARG_PROPERTIES = new ArgumentProperty[]{
            // 测试规模，有候选项
            new ArgumentProperty("数据规模", new String[]{"1", "2", "4", "8"}),
            // 数据文件路径，没有候选项，需要输入
            new ArgumentProperty("测试路径")
    };

    /**
     * 此处设置其余测试参数属性。每一个参数属性由参数名和可能存在的参数候选值组成。
     */

    /*
     * FIO读写速度测试参数属性
     * */
    public static final ArgumentProperty[] FIO_ARG_PROPERTIES = new ArgumentProperty[]{
            // 测试目录，用户输入
            new ArgumentProperty("测试目录"),
            // 读写方式，有候选项
            new ArgumentProperty("读写方式", new String[]{"随机读", "随机写", "顺序读", "顺序写", "%70顺序读,%70顺序写", "%70随机读,%30随机写"}),
            // 文件块大小，有候选项
            new ArgumentProperty("文件块大小", new String[]{"4k", "8k", "16k", "32k", "64k"}),
            // 文件大小，有候选项
            new ArgumentProperty("文件大小", new String[]{"1G", "4G", "8G"}),
    };

    /*
    * IOZone读写速度测试参数属性
    * */
    public static final ArgumentProperty[] IOZONE_ARG_PROPERTIES = new ArgumentProperty[]{
            // 测试目录，用户输入
            new ArgumentProperty("测试目录"),
            // 文件块大小，有候选项
            new ArgumentProperty("文件块大小", new String[]{"4k", "8k", "16k", "32k", "64k"}),
            // 文件大小，有候选项
            new ArgumentProperty("文件大小", new String[]{"1G", "4G", "8G"}),
    };

    // fio并发度测试参数属性
    public static final ArgumentProperty[] FIO_PARALLEL_ARG_PROPERTIES = new ArgumentProperty[]{
            // 测试目录，用户输入
            new ArgumentProperty("测试目录"),
            // 并发度设置，有候选项
            new ArgumentProperty("并发线程数", new String[]{"16", "64", "128", "256"}),
    };

    // fio小文件测试参数属性
    public static final ArgumentProperty[] FIO_MINIFILE_ARG_PROPERTIES = new ArgumentProperty[]{
            // 测试目录，用户输入
            new ArgumentProperty("测试目录"),
    };

    /**
     * 各数据值，以String格式保存
     */
    public ArrayList<String> values;

    /**
     * @param testProject 测试项目中文名
     * @return 返回测试项目中文名对应TestArguments里面的静态参数数组名
     */
    public static ArgumentProperty[] getArgPropertiesForTest(String testProject) {
        switch (testProject) {
            case "TPC-C":
                return TPCC_ARG_PROPERTIES;
            case "TPC-H":
                return TPCH_ARG_PROPERTIES;
            // 补全这里补全这里补全这里补全这里补全这里补全这里补全这里
            // 补全这里补全这里补全这里补全这里补全这里补全这里补全这里
            // 补全这里补全这里补全这里补全这里补全这里补全这里补全这里
            case "读写速度测试":
                return FIO_ARG_PROPERTIES;
            case "IOZONE读写速度测试":
                return IOZONE_ARG_PROPERTIES;
            case "并发度测试":
                return FIO_PARALLEL_ARG_PROPERTIES;
            case "小文件测试":
                return FIO_MINIFILE_ARG_PROPERTIES;
            default:
                return new ArgumentProperty[]{}; // 返回空数组表示没有找到匹配的测试项目
        }
    }
}
