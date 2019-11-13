import java.io.IOException;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("bash /home/cary/exec.sh");
            ProcessExecutor processExecutor = new ProcessExecutor(process);
            processExecutor.execute();
            List<String> outList = processExecutor.getOutputList();
            List<String> errorList = processExecutor.getErrorList();
//            outList.forEach(System.out::println);
//            errorList.forEach(System.out::println);
            System.out.println(outList.size());
            System.out.println(errorList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
