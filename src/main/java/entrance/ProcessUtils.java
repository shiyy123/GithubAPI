package entrance;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Common utils
 *
 * @author cary.shi on 2019/10/11
 */
public class ProcessUtils {
    /**
     * Process the stream generated in process to /dev/null
     */
    public static void processMessageToNull(final InputStream inputStream) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Reader reader = new InputStreamReader(inputStream);
                BufferedReader br = new BufferedReader(reader);
                // write to /dev/null
                NullOutputStream nullOutputStream = new NullOutputStream();
                try {
                    String line;
                    while ((line = br.readLine()) != null) {
                        nullOutputStream.write(line.getBytes(StandardCharsets.UTF_8));
                    }
                    nullOutputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    System.err.println("print processMessage Error");
                }
            }
        }).start();
    }

    /**
     * Process the stream generated in process to String
     */
    public static List<String> processMessageToStringMulti(InputStream inputStream) {
        List<String> lines = new ArrayList<>();
        new Thread(() -> {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line);
                    System.out.println(line);
//                stringBuilder.append(line);
//                stringBuilder.append(System.getProperty("line.separator"));
                }
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        return lines;
    }

    public static List<String> processMessageToStringSingle(InputStream inputStream) {
        List<String> lines = new ArrayList<>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
                System.out.println(line);
//                stringBuilder.append(line);
//                stringBuilder.append(System.getProperty("line.separator"));
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private static String getPID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }

    public static int getAvailableProcessNumber() {
        int pNum = Runtime.getRuntime().availableProcessors();
        return Math.max(pNum, 1);
    }
}
