package tool;

import entrance.ProcessExecutor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author cary.shi on 2019/11/17
 */
public class CppCheck {

    boolean check(String workSpacePath) {
        File[] folders = new File(workSpacePath).listFiles();
        assert folders != null;
        for (File folder : folders) {
            String name = folder.getName();
            System.out.println(name);
            String decompressPath = folder.getAbsolutePath() + File.separator + "decompress";

            String scanPath = decompressPath + File.separator + name;
            String resPath = decompressPath + File.separator + "cppcheck.xml";

            String outPath = decompressPath + File.separator + "out_cppcheck";
            String errorPath = decompressPath + File.separator + "error_cppcheck";

            String cmd = "cppcheck --xml " + scanPath + " --output-file=" + resPath;

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

    boolean selectData(String dataPath, HashSet<String> folderSet) {
        boolean result = true;
        File data = new File(dataPath);
        File subData = new File(data.getParent() + File.separator + "subData.txt");
        List<String> subDataList = new ArrayList<>();
        try {
            List<String> dataList = FileUtils.readLines(data, StandardCharsets.UTF_8);
            for (String s : dataList) {
                String[] ss = s.split("\t");
                String[] paths = ss[0].split("/");
                if (folderSet.contains(paths[0])) {
                    subDataList.add(s);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileUtils.writeLines(subData, "utf-8", subDataList, System.getProperty("line.separator"), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    HashSet<String> getSet() {
        HashSet<String> set = new HashSet<>();
        set.add("CWE121_Stack_Based_Buffer_Overflow");
        set.add("CWE122_Heap_Based_Buffer_Overflow");
        set.add("CWE124_Buffer_Underwrite");
        set.add("CWE126_Buffer_Overread");
        set.add("CWE127_Buffer_Underread");
        set.add("CWE401_Memory_Leak");
        set.add("CWE415_Double_Free");
        set.add("CWE416_Use_After_Free");
        set.add("CWE457_Use_of_Uninitialized_Variable");
        set.add("CWE476_NULL_Pointer_Dereference");
        set.add("CWE563_Unused_Variable");
        set.add("CWE590_Free_Memory_Not_on_Heap");
        set.add("CWE680_Integer_Overflow_to_Buffer_Overflow");
        set.add("CWE685_Function_Call_With_Incorrect_Number_of_Arguments");
        set.add("CWE690_NULL_Deref_From_Return");
        set.add("CWE758_Undefined_Behavior");
        set.add("CWE761_Free_Pointer_Not_at_Start_of_Buffer");
        set.add("CWE762_Mismatched_Memory_Management_Routines");
        set.add("CWE773_Missing_Reference_to_Active_File_Descriptor_or_Handle");
        set.add("CWE775_Missing_Release_of_File_Descriptor_or_Handle");
        set.add("CWE843_Type_Confusion");

        return set;
    }

    public static void main(String[] args) {
        CppCheck cppCheck = new CppCheck();

//        cppCheck.check("/mnt/share/workspaceJuliet");
        cppCheck.selectData("G:\\share\\experiment\\data.txt", cppCheck.getSet());
    }
}
