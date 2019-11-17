package entrance;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author cary.shi on 2019/11/13
 */

class Func {
    String name;
    String path;
    int startLine;
    int endLine;
    boolean isVul;
    int vulId;

    Func(String name, String path, int startLine, int endLine, boolean isVul, int vulId) {
        this.name = name;
        this.path = path;
        this.startLine = startLine;
        this.endLine = endLine;
        this.isVul = isVul;
        this.vulId = vulId;
    }

    @Override
    public String toString() {
        return name + "\n" + path + "\n" + startLine + " " + endLine + "\n" + vulId + "\n" + isVul + "\n";
    }
}

public class CalcTNAndFP {

    boolean isVulnerability(String s) {
        if (s.toLowerCase().contains("bad")) {
            return true;
        }
        return false;
    }

    HashMap<Integer, List<Func>> loadFunc(String workSpacePath) {
        HashMap<Integer, List<Func>> map = new HashMap<>();

        File[] folders = new File(workSpacePath).listFiles();
        assert folders != null;
        for (File folder : folders) {
            List<Func> funcList = new ArrayList<>();

            String cweName = folder.getName();
            int vulId = Integer.parseInt(cweName.split("_")[0].substring(3));

            String decompressPath = folder.getAbsolutePath() + File.separator + "decompress";
            String funcPath = decompressPath + File.separator + "funcs";

            File funcFile = new File(funcPath);
            try {
                String content = FileUtils.readFileToString(funcFile, StandardCharsets.UTF_8);
                String[] fileFuncList = content.split("------");
                for (String s : fileFuncList) {
                    String fileFunc = s.trim();
                    if (fileFunc.isEmpty()) {
                        continue;
                    }
                    String[] lines = fileFunc.split("\n");
                    String[] tmp = lines[0].split("/");
                    String path = tmp[tmp.length - 2] + "/" + tmp[tmp.length - 1];
                    for (int i = 1; i < lines.length; i += 3) {
                        String name = lines[i];
                        int startLine = Integer.parseInt(lines[i + 1].substring(lines[i + 1].indexOf('(') + 1, lines[i + 1].indexOf(')')));
                        int endLine = Integer.parseInt(lines[i + 2].substring(lines[i + 2].indexOf('(') + 1, lines[i + 2].indexOf(')')));

                        Func func = new Func(name, path, startLine, endLine, isVulnerability(name), vulId);
                        funcList.add(func);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            map.put(vulId, funcList);
        }
        return map;
    }

    void checkEffectiveness(String workSpacePath) {
        File[] workSpaces = new File(workSpacePath).listFiles();
        assert workSpaces != null;
        for (File workSpace : workSpaces) {
            File outFile = new File(workSpace.getAbsolutePath() + File.separator + "out");
            if (!outFile.exists()) {
                System.out.println(workSpace.getAbsolutePath());
            }
        }
    }

    // TP,FP,TN,FN
    boolean calc(String workSpacePath) {
        boolean result = true;

        HashMap<Integer, List<Func>> map = loadFunc(workSpacePath);

//        map.forEach((k, v) -> {
//            System.out.println(k);
//            System.out.println(v);
//        });

        File[] folders = new File(workSpacePath).listFiles();
        assert folders != null;
        for (File folder : folders) {
            int trueVul = 0;
            int falseVul = 0;

            String outPath = folder.getAbsolutePath() + File.separator + "out";
            File outFile = new File(outPath);
            try {
                JSONObject vulContent = new JSONObject(FileUtils.readFileToString(outFile, StandardCharsets.UTF_8));
                JSONArray vulJsonArray = vulContent.getJSONArray("data");
                for (int i = 0; i < vulJsonArray.length(); i++) {
                    JSONObject vulJsonObject = vulJsonArray.getJSONObject(i);
                    String tmp1 = vulJsonObject.getJSONArray("reference").getJSONObject(0).getString("refName");
                    int vulId = Integer.parseInt(tmp1.substring(tmp1.indexOf('-') + 1));
                    String tmp2 = vulJsonObject.getString("location");

                    int lineNum = Integer.parseInt(tmp2.substring(tmp2.lastIndexOf(':') + 1));
                    String path = tmp2.substring(22, tmp2.lastIndexOf(':'));

                    List<Func> funcList = map.getOrDefault(vulId, new ArrayList<>());
                    boolean found = false;
                    for (Func func : funcList) {
                        if (path.equals(func.path) && (lineNum >= func.startLine && lineNum <= func.endLine)) {
                            found = true;
                            trueVul++;
                        }
                    }
                    if (!found) {
                        falseVul++;
                    }
                }

            } catch (IOException e) {
                result = false;
            }
            System.out.println(folder.getName());
            int vid = Integer.parseInt(folder.getName().split("_")[0].substring(3));
            System.out.println(map.get(vid).size());
            System.out.println(trueVul);
            System.out.println(falseVul);
        }

        return result;
    }

    public static void main(String[] args) {
        CalcTNAndFP calcTNAndFP = new CalcTNAndFP();
        String workSpacePath = "G:\\share\\workspaceJuliet";

        calcTNAndFP.calc(workSpacePath);
    }
}
