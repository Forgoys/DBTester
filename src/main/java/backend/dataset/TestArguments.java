package backend.dataset;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试参数抽象类，需要定义测试参数子类继承该类
 */
public class TestArguments {


    /**
     * 各数据值，以String格式保存
     */
    public ArrayList<String> values;

    public TestArguments() {
        values = new ArrayList<>();
    }

    //*************************************常量值************************************************
    /**
     * TPCC测试参数属性
     */
    public static final ArgumentProperty[] TPCC_ARG_PROPERTIES = new ArgumentProperty[]{
            // 测试规模，有候选项
            new ArgumentProperty("数据规模", new String[]{"20", "50", "100"}),
            new ArgumentProperty("并发连接数", new String[]{"8", "16", "32", "64", "128", "256"}),
            new ArgumentProperty("加载进程数", new String[]{"1", "2", "4", "8", "16", "32", "64"}),
            new ArgumentProperty("运行时长(min)", new String[]{"1", "5", "10", "20", "30", "40",
                    "50", "60", "70", "80", "90", "100", "110", "120"})
    };


    /**
     * TPCH测试参数属性
     */
    public static final ArgumentProperty[] TPCH_ARG_PROPERTIES = new ArgumentProperty[]{
            // 测试规模，有候选项
            new ArgumentProperty("数据规模", new String[]{"5", "10", "20"}),
    };

    /**
     * influxdb-comparision工具时序数据库测试的参数属性
     */
    public static final ArgumentProperty[] INFLUXCOMP_WRITE_ARG_PROPERTIES = new ArgumentProperty[] {
            new ArgumentProperty("写入场景", new String[]{"100台*30天","4000台*3天","2万台*3小时","10万台*3小时","100万台*3分钟*"}),
            new ArgumentProperty("客户端数", new String[]{"16","100"})
    };
    public static final ArgumentProperty[] INFLUXCOMP_READ_ARG_PROPERTIES = new ArgumentProperty[] {
            new ArgumentProperty("查询场景", new String[]{"100台*30天","4000台*3天","2万台*3小时","10万台*3小时","100万台*3分钟*"}),
            new ArgumentProperty("查询类型", new String[]{"8host-1hr","1host-1hr","1host-12hr"}),
            // new ArgumentProperty("查询语句数量",new String[]{"1万","5万","10万})
    };

    /*
     * FIO读写速度测试参数属性
     * */
    public static final ArgumentProperty[] FIO_ARG_PROPERTIES = new ArgumentProperty[]{
            // 测试目录，用户输入
            new ArgumentProperty("测试目录"),
            // 文件块大小，有候选项
            new ArgumentProperty("文件块大小", new String[]{"4k", "8k", "16k", "32k", "64k"}),
            // 文件大小，有候选项
            new ArgumentProperty("文件大小", new String[]{"8K", "1G", "4G", "8G"}),
            // 读写方式，有候选项
            new ArgumentProperty("读写方式", new String[]{"随机读", "随机写", "顺序读", "顺序写", "%70顺序读,%30顺序写", "%70随机读,%30随机写"}),
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
            new ArgumentProperty("文件大小", new String[]{"8k", "1G", "4G", "8G"}),
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
     * @param testProject 测试项目中文名
     * @return 返回测试项目中文名对应TestArguments里面的静态参数数组名
     */
    public static ArgumentProperty[] getArgPropertiesForTest(String testObject, String testProject) {
        switch (testProject) {
            case "TPC-C":
                return TPCC_ARG_PROPERTIES;
            case "TPC-H":
                return TPCH_ARG_PROPERTIES;
            case "写入性能":
                return INFLUXCOMP_WRITE_ARG_PROPERTIES;
            case "查询性能":
                return INFLUXCOMP_READ_ARG_PROPERTIES;
            case "IOZONE读写速度测试":
                return IOZONE_ARG_PROPERTIES;
            case "读写速度测试":
                return FIO_ARG_PROPERTIES;
            case "小文件测试":
                return FIO_MINIFILE_ARG_PROPERTIES;
            case "并发度测试":
                return FIO_PARALLEL_ARG_PROPERTIES;
            default:
                return new ArgumentProperty[]{}; // 返回空数组表示没有找到匹配的测试项目
        }
    }
}
