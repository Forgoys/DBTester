package backend.dataset;

import java.io.File;
import java.util.Arrays;

public class DirectoryManager {

    /**
     * 创建结果目录
     */
    public static void createDirectories() {
        String[] rootFolders = {"PolarDB", "神通数据库", "OpenGauss", "TDengine", "InfluxDB", "Lindorm", "GlusterFS", "OceanFS"};
        String[] subFoldersForFirstThree = {"TPC-C", "TPC-H", "可靠性", "适配性"};
        String[] subFoldersForNextThree = {"写入性能", "查询性能", "可靠性", "适配性"};
        String[] subFoldersForLastTwo = {"读写速度测试", "并发度测试", "小文件测试", "可靠性测试"};

        String basePath = System.getProperty("user.dir") + File.separator + "results";
        if (new File(basePath).exists()) {  // 已经存在，无需创建
            return;
        }
        new File(basePath).mkdirs();

        for (String folder : rootFolders) {
            String path = basePath + File.separator + folder;
            new File(path).mkdirs();

            String[] subFolders;
            if ("PolarDB".equals(folder) || "神通数据库".equals(folder) || "OpenGauss".equals(folder)) {
                subFolders = subFoldersForFirstThree;
            } else if ("TDengine".equals(folder) || "InfluxDB".equals(folder) || "Lindorm".equals(folder)) {
                subFolders = subFoldersForNextThree;
            } else {
                subFolders = subFoldersForLastTwo;
            }

            for (String subFolder : subFolders) {
                new File(path + File.separator + subFolder).mkdirs();
            }
        }
    }

    /**
     * 从相对路径分析测试对象和测试项目，例如results/InfluxDB/可靠性/Result123，则输出InfluxDB和可靠性
     * @param relativePath 相对路径，以results开始
     * @return String数组，第0个是测试对象名，第1格式测试项目名
     */
    public static String[] analyzePath(String relativePath) {
        String[] parts = relativePath.split(File.separator.replace("\\", "\\\\")); // 对于Windows路径分隔符的特殊处理
        System.out.println(Arrays.toString(parts));

        // 从results开始，共有四层文件夹
        if (parts.length != 4) {
            System.out.println("Invalid path.");
            return null;
        }
        return new String[]{parts[1], parts[2]};
    }

    /**
     *
     * @param testObject 测试对象名（要严格对应）
     * @param testProject 测试项目名（要严格对应）
     * @param testResultFolderName 测试结果文件名
     * @return 该测试结果文件所处的绝对路径
     */
    public static String buildAbsolutePath(String testObject, String testProject, String testResultFolderName) {
        String absolutePath = System.getProperty("user.dir") + File.separator + "results" + File.separator + testObject + File.separator + testProject + File.separator + testResultFolderName;
        return absolutePath;
    }

    /**
     * 创建目录
     * @param absolutePath 所要创建目录的绝对路径，可以调用 DirectoryManager.buildAbsolutePath方法生成
     */
    public static void fakeMakeDir(String absolutePath) {
        if (absolutePath == null || absolutePath.trim().isEmpty()) {
            throw new IllegalArgumentException("提供的路径为空或无效。");
        }

        try {
            File dir = new File(absolutePath);

            // 检查目录是否已经存在
            if (dir.exists()) {
                System.out.println("目录已存在: " + absolutePath);
                return; // 如果目录已存在，则认为操作成功，不抛出异常
            }

            // 尝试创建目录
            if (!dir.mkdirs()) {
                // 如果创建失败，则抛出异常
                throw new Exception("创建目录失败。请检查路径是否正确，并确保有足够的权限: " + absolutePath);
            } else {
                System.out.println("目录成功创建: " + absolutePath);
            }
        } catch (Exception e) {
            // 捕获到安全异常时，抛出更具体的异常
            throw new SecurityException("在创建目录时发生安全异常: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Step 1: Create directories
        System.out.println("Creating directories...");
        DirectoryManager.createDirectories();
        System.out.println("Directories created successfully.\n");

        // Step 2: Analyze a sample path
        String samplePath = "results" + File.separator + "PolarDB" + File.separator + "TPC-C";
        System.out.println("Analyzing path: " + samplePath);
        DirectoryManager.analyzePath(samplePath);

        // Step 3: Build and analyze a relative path
        String testObject = "InfluxDB";
        String testProject = "可靠性";
        String testResultFolderName = "Result123";
        String relativePath = DirectoryManager.buildAbsolutePath(testObject, testProject, testResultFolderName);
        System.out.println("\nBuilt relative path: " + relativePath);

        // Optional: Verify the built path by analyzing it
        System.out.println("Verifying the built path by analyzing:");
        DirectoryManager.analyzePath(relativePath);
    }
}
