package tool;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author cary.shi on 2019/11/15
 */
public class DataCleaner {

    boolean deleteUnnecessaryFile(File folder) {
        File[] files = folder.listFiles();
        for (File file : files) {
            if ((file.isFile() && !file.getName().startsWith("CWE")) || file.getName().endsWith(".bat")) {
                file.delete();
            }
        }
        return true;
    }

    boolean cleanData(String path) {
        File[] folders = new File(path).listFiles();
        for (File folder : folders) {
            deleteUnnecessaryFile(folder);
            File[] subFolders = folder.listFiles();
            for (File subFolder : subFolders) {
                if (subFolder.isDirectory()) {
                    deleteUnnecessaryFile(subFolder);
                }
            }
        }
        return true;
    }

    boolean organizeData(String path) {
        File[] folders = new File(path).listFiles();
        for (File folder : folders) {
            File[] subFolders = folder.listFiles();
            for (File subFolder : subFolders) {
                if (subFolder.isDirectory()) {
                    File[] sources = subFolder.listFiles();
                    for (File source : sources) {
                        try {
                            FileUtils.copyFileToDirectory(source, subFolder.getParentFile(), false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        FileUtils.deleteDirectory(subFolder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return true;
    }

    public static void main(String[] args) {
        DataCleaner dataCleaner = new DataCleaner();

        String path = "G:\\share\\dataset\\C_CPP";
        dataCleaner.cleanData(path);
        dataCleaner.organizeData(path);
    }
}
