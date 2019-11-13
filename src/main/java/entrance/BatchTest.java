package entrance;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    static void batchTest(String zipPath, String baseWorkSpace) {
//        String zipPath = "/mnt/share/dataset/";
//        String zipPath = "/mnt/share/testcase/c++";
        File zipFolder = new File(zipPath);
        File[] compressFiles = zipFolder.listFiles();
        List<String> compressFilePathList = new ArrayList<>();
        for (File f : compressFiles) {
            compressFilePathList.add(f.getAbsolutePath());
        }

        ExecutorService executorService = Executors.newCachedThreadPool();
        int threadSize = 4;
        int gap = compressFilePathList.size() / threadSize;
        for (int i = 0; i < threadSize; i++) {
            int start = i * gap;
            int end = (i + 1) * gap;

            if (i == threadSize - 1) {
                end = compressFilePathList.size();
            }

            final List<String> singleList = compressFilePathList.subList(start, end);

            Runnable run = () -> {
                try {
                    singleTest(singleList, baseWorkSpace);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            executorService.execute(run);
        }

    }

    static void singleTest(List<String> compressFilePaths, String baseWorkSpace) {

        for (String compressFilePath : compressFilePaths) {
            File compressFile = new File(compressFilePath);
            String name = compressFile.getName().substring(0, compressFile.getName().indexOf("."));
            String curWorkSpacePath = baseWorkSpace + File.separator + name;
            File curWorkSpace = new File(curWorkSpacePath);
            if (!curWorkSpace.exists()) {
                curWorkSpace.mkdir();
            }
            try {
                FileUtils.copyFileToDirectory(compressFile, curWorkSpace);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String cmd = "docker run --rm -v " + curWorkSpacePath + "/:/workspace mt_csc:latest start -D -F /workspace/" + compressFile.getName();
            String outFilePath = curWorkSpacePath + File.separator + "out";
            String errorFilePath = curWorkSpacePath + File.separator + "error";
            try {
                Process process = Runtime.getRuntime().exec(cmd);
                ProcessExecutor processExecutor = new ProcessExecutor(process, outFilePath, errorFilePath);
                processExecutor.execute();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
//        tryDecompress();
//        deleteUseless();
        String zipPath = "/mnt/share/dataset/C_CPP";
        String workPath = "/mnt/share/workspaceJuliet";
        batchTest(zipPath, workPath);
    }
}
