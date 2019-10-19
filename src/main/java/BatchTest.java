import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class BatchTest {

    static void tryDecompress() {
        String compressFolderPath = "/mnt/share/dataset/";
        File compressFolder = new File(compressFolderPath);
        File[] files = compressFolder.listFiles();
        File logFile = new File("/mnt/share/log.txt");

        for (File compressFile : files) {
            boolean success = DecompressUtils.decompressZip(compressFile.getAbsolutePath(), "/mnt/share/tmp/");
            if (!success) {
                try {
                    FileUtils.write(logFile, compressFile.getAbsolutePath() + System.lineSeparator(), StandardCharsets.UTF_8, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static void batchTest() {
        String zipPath = "/mnt/share/dataset";
        String baseWorkSpace = "/mnt/share/workspace";
        File zipFolder = new File(zipPath);
        File[] compressFiles = zipFolder.listFiles();
        for (File compressFile : compressFiles) {
            String name = compressFile.getName().substring(0, compressFile.getName().indexOf("."));
            String curWorkSpacePath = baseWorkSpace + File.separator + name;
            File curWorkSpace = new File(curWorkSpacePath);
            if (!curWorkSpace.exists()) {
                curWorkSpace.mkdir();
            }
            String cmd = "docker run --rm -v " + curWorkSpacePath + "/:/workspace cscanner:latest start -F /workspace/" + compressFile.getName();
            try {
                Process process = Runtime.getRuntime().exec(cmd);
                ProcessUtils.processMessageToNull(process.getErrorStream());
                ProcessUtils.processMessageToNull(process.getInputStream());
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static void deleteUseless() {
        File log = new File("/mnt/share/log.txt");
        try {
            List<String> fileList = FileUtils.readLines(log, StandardCharsets.UTF_8);
            for (String s : fileList) {
                new File(s).delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
//        tryDecompress();
        deleteUseless();
    }
}
