package backend.dataset;

import java.util.List;

public abstract class AbstractDataSet {
    /**
     * 数据集中各数据名称
     */
    public static List<String> dataNames;

    /**
     * 各数据值，以String格式保存
     */
    public List<String> valueList;

    /**
     * 如果尚未初始化 @param dataNames，则该构造器会调用子类的{@link #setDataNames()}
     */
    public AbstractDataSet() {
        if(dataNames == null || dataNames.isEmpty()) {
            setDataNames();
        }
    }

    /**
     * 子类中实现该方法，用于初始化{@link #dataNames}中各数据名称.
     * 你需要指定已有的数据名称List或者新建一个List并添加元素
     */
    abstract void setDataNames();

}
