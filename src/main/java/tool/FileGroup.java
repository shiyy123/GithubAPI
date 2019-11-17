package tool;

import java.io.File;

/**
 * @author cary.shi on 2019/11/14
 */
public class FileGroup {
    boolean helpFileGroupByName(String workSpacePath) {
        File [] workSpaces=new File(workSpacePath).listFiles();

        return true;
    }

}
