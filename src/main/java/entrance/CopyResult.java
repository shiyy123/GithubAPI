package entrance;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author cary.shi on 2019/11/14
 */
public class CopyResult {
    boolean copyResult(String srcPath, String dstPath) {
        File[] srcFolders = new File(srcPath).listFiles();
        for (File srcFolder : srcFolders) {
            String name = srcFolder.getName();
            File dstFolder = new File(dstPath + File.separator + name);
            if (!dstFolder.exists()) {
                dstFolder.mkdir();
            }

            try {
                FileUtils.copyFileToDirectory(new File(srcFolder.getAbsolutePath() + File.separator + "cpp_bugs.xml"), dstFolder);
                FileUtils.copyFileToDirectory(new File(srcFolder.getAbsolutePath() + File.separator + "debug"), dstFolder);
                FileUtils.copyFileToDirectory(new File(srcFolder.getAbsolutePath() + File.separator + "log"), dstFolder);
                FileUtils.copyFileToDirectory(new File(srcFolder.getAbsolutePath() + File.separator + "out"), dstFolder);
                FileUtils.copyFileToDirectory(new File(srcFolder.getAbsolutePath() + File.separator + "PID"), dstFolder);
                FileUtils.copyFileToDirectory(new File(srcFolder.getAbsolutePath() + File.separator + "time"), dstFolder);
                File error = new File(srcFolder.getAbsolutePath() + File.separator + "error");
                if (error.exists()) {
                    FileUtils.copyFileToDirectory(error, dstFolder);
                }
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        CopyResult copyResult=new CopyResult();
        copyResult.copyResult("G:\\share\\workspaceJuliet", "H:\\workspaceJuliet");
    }

}
