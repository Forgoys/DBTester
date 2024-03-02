package backend.dataset;

public class ArgumentProperty {
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
