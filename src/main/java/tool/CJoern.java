package tool;

import entrance.ProcessExecutor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author cary.shi on 2019/11/15
 */
public class CJoern {
    void batchGenerateJoern(String workSpacePath) {
        File[] workSpaces = new File(workSpacePath).listFiles();
        assert workSpaces != null;
        List<File> workSpaceFiles = new ArrayList<>(Arrays.asList(workSpaces));
        generateJoern(workSpaceFiles);
//        ExecutorService executorService = Executors.newCachedThreadPool();
//        int threadSize = 4;
//        int gap = workSpaceFiles.size() / threadSize;
//
//        for (int i = 0; i < threadSize; i++) {
//            int start = i * gap;
//            int end = (i + 1) * gap;
//            if (i == threadSize - 1) {
//                end = workSpaceFiles.size();
//            }
//
//            final List<File> singleList = workSpaceFiles.subList(start, end);
//
//            Runnable run = () -> {
//                try {
//                    generateJoern(singleList);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            };
//            executorService.execute(run);
//        }

    }


    boolean generateJoern(List<File> workSpaces) {
//        File[] workSpaces = new File(workSpacePath).listFiles();
        for (File workSpace : workSpaces) {
            System.out.println(workSpace.getName());
            String cweName = workSpace.getName();
            String decompressPath = new File(workSpace.getAbsolutePath() + File.separator + "decompress").getAbsolutePath();
            String codePath = decompressPath + File.separator + cweName;
            String outPath = decompressPath + File.separator + cweName + ".bin.zip";
            if (new File(outPath).exists()) {
                continue;
            }
            String outLogPath = decompressPath + File.separator + "out";
            String errorLogPath = decompressPath + File.separator + "error";

            String command = "/mnt/share/code/joern-cli/joern-parse " + codePath + " --out " + outPath;
            try {
                Process process = Runtime.getRuntime().exec(command);
                ProcessExecutor processExecutor = new ProcessExecutor(process, outLogPath, errorLogPath);
                processExecutor.execute();

            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    boolean generateFunction(String workSpacePath) {
        File[] workSpaces = new File(workSpacePath).listFiles();
        for (File workSpace : workSpaces) {
            String name = workSpace.getName();
            System.out.println(name);

            String decompressPath = workSpace.getAbsolutePath() + File.separator + "decompress";

            String cpgPath = decompressPath + File.separator + name + ".bin.zip";
            String funcPath = decompressPath + File.separator + "funcs";
            String outPath = decompressPath + File.separator + "out";
            String errorPath = decompressPath + File.separator + "error";

            String cmd = "/mnt/share/code/joern-cli/joern --script /mnt/share/code/joern-cli/scripts/list-funcs/list-funcs.scala --params cpgFile=" +
                    cpgPath + ",outFile=" + funcPath;

            try {
                Process process = Runtime.getRuntime().exec(cmd);
                ProcessExecutor processExecutor = new ProcessExecutor(process, outPath, errorPath);
                processExecutor.execute();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        String path = "/mnt/share/workspaceJuliet";
        CJoern cJoern = new CJoern();
//        cJoern.batchGenerateJoern(path);
        cJoern.generateFunction(path);
    }
}
