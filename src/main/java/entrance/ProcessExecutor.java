package entrance;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cary.shi on 2019/10/29
 */
public class ProcessExecutor {
    private Process process;
    //    private List<String> outputList;
    private String outFilePath;
    private String errorFilePath;

    public ProcessExecutor(Process process, String outFilePath, String errorFilePath) {
        this.process = process;
        this.outFilePath = outFilePath;
        this.errorFilePath = errorFilePath;
    }

//    public List<String> getOutputList() {
//        return this.outputList;
//    }

//    public List<String> getErrorList() {
//        return this.errorList;
//    }

    public int execute() {
        int res = 0;
        ProcessOutputThreadFile outputThread = new ProcessOutputThreadFile(this.process.getInputStream(), this.outFilePath);
        ProcessOutputThreadFile errorThread = new ProcessOutputThreadFile(this.process.getErrorStream(), this.errorFilePath);
        outputThread.start();
        errorThread.start();
        try {
            res = this.process.waitFor();
            outputThread.join();
            errorThread.join();
//            this.outputList = outputThread.getOutputList();
//            this.errorList = errorThread.getOutputList();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return res;
    }

}

class ProcessOutputThread extends Thread {
    private InputStream is;
    private List<String> outputList;

    ProcessOutputThread(InputStream is) {
        this.is = is;
        this.outputList = new ArrayList<>();
    }

    List<String> getOutputList() {
        return this.outputList;
    }

    @Override
    public void run() {
        InputStreamReader isr = null;
        BufferedReader br = null;
        isr = new InputStreamReader(this.is);
        br = new BufferedReader(isr);
        String output = null;
        try {
            while (null != (output = br.readLine())) {
//                DebugUtils.debug(output);
                this.outputList.add(output);
            }
        } catch (IOException e) {
//            System.err.println("Process run error");
        } finally {
            try {
                br.close();
                isr.close();
                if (null != this.is) {
                    this.is.close();
                }
            } catch (IOException e) {
//                System.err.println("Process close stream error");
            }
        }
    }
}

class ProcessOutputThreadFile extends Thread {
    private InputStream is;
    private File file;

    ProcessOutputThreadFile(InputStream is, String filePath) {
        this.is = is;
        this.file = new File(filePath);
    }

    @Override
    public void run() {
        InputStreamReader isr = null;
        BufferedReader br = null;
        isr = new InputStreamReader(this.is);
        br = new BufferedReader(isr);
        String output = null;
        try {
            while (null != (output = br.readLine())) {
                FileUtils.write(this.file, output, StandardCharsets.UTF_8, true);
                FileUtils.write(this.file, System.getProperty("line.separator"), StandardCharsets.UTF_8, true);
            }
        } catch (IOException e) {
//            System.err.println("Process run error");
        } finally {
            try {
                br.close();
                isr.close();
                if (null != this.is) {
                    this.is.close();
                }
            } catch (IOException e) {
//                System.err.println("Process close stream error");
            }
        }
    }
}