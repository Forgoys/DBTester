package org.example.dbtester;

import java.util.HashMap;
import java.util.Map;

public class TestProject {
    private String name;
    private Map<String, String> configParameters;
    private Map<String, String> resultData;

    public TestProject(String name) {
        this.name = name;
        this.configParameters = new HashMap<>();
        this.resultData = new HashMap<>();
    }

    // 获取项目名称
    public String getName() {
        return name;
    }

    // 设置项目名称
    public void setName(String name) {
        this.name = name;
    }

    // 获取配置参数
    public Map<String, String> getConfigParameters() {
        return configParameters;
    }

    // 设置配置参数
    public void setConfigParameters(Map<String, String> configParameters) {
        this.configParameters = configParameters;
    }

    // 添加单个配置参数
    public void addConfigParameter(String key, String value) {
        this.configParameters.put(key, value);
    }

    // 获取结果数据
    public Map<String, String> getResultData() {
        return resultData;
    }

    // 设置结果数据
    public void setResultData(Map<String, String> resultData) {
        this.resultData = resultData;
    }

    // 添加单个结果数据
    public void addResultData(String key, String value) {
        this.resultData.put(key, value);
    }
}

