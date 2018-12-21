import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

public class Tools {

    /**
     * process the output info of process
     * @param inputStream
     * @param isDebuggable
     */
    public static void processMessage(final InputStream inputStream, final boolean isDebuggable) {
        new Thread(() -> {
            Reader reader = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(reader);
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    if (isDebuggable) {
                        System.out.println(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * get remaining request time
     * @return
     */
    public static int getRemainRequestTime() {
        int remaining = 0;
        String cmd = "curl -H \"Authorization: token " + StaticResource.token + "\" " + "https://api.github.com/rate_limit";

        try {
            Process process = Runtime.getRuntime().exec(getCmd(cmd));
            StringBuilder res = getProcessOutput(process.getInputStream(), true);
            process.waitFor();

            JSONObject rateLimit = new JSONObject(res.toString());
            remaining = rateLimit.getJSONObject("resources").getJSONObject("search").getInt("remaining");
        } catch (IOException | InterruptedException | JSONException e) {
            e.printStackTrace();
        }
        return remaining;
    }

    public static String[] getCmd(String cmd) {
        return new String[]{"/bin/sh", "-c", cmd};
    }

    /**
     * Get output from p(Process).getInputStream()
     * @param inputStream input
     * @param debuggable print in console or not
     * @return output
     */
    public static StringBuilder getProcessOutput(InputStream inputStream, boolean debuggable) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder res = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                res.append(line);
                if (debuggable) {
                    System.out.println(line);
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * prevent exceed the request time and sleep 60 seconds
     */
    public static void protectRateLimit() {
        while (getRemainRequestTime() < 3) {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * read from process output and write in specific file
     * @param inputStream
     * @param dataStorePath
     * @param split
     */
    public static void readAndWrite(InputStream inputStream, String dataStorePath, String split) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            BufferedWriter writer = new BufferedWriter(new FileWriter(dataStorePath, true));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
            writer.write(split);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
