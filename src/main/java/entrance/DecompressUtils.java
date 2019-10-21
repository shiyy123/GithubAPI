package entrance;

import java.io.*;
import java.util.zip.GZIPInputStream;


import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.ZipFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Conduct decompress
 *
 * @author cary.shi on 2019/10/11
 */
public class DecompressUtils {

    public static String compressedDir = null;


    /**
     * Decompress zip in disk
     *
     * @return decompress success or not
     */
    public static boolean decompressZip(String compressFilePath, String targetPath) {
        boolean res = true;
        try {
            ZipFile zipFile = new ZipFile(compressFilePath);
            zipFile.extractAll(targetPath);
        } catch (ZipException e) {
            res = false;
        }
        return res;
    }



}
