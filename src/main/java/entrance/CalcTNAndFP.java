package entrance;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    HashSet<Integer> vulIdSet;

    /**
     * @param name      函数名
     * @param path      函数相对路径
     * @param startLine 函数起始行
     * @param endLine   函数终止行
     * @param isVul     是否是漏洞函数
     * @param vulIdSet  漏洞id的集合
     */
    Func(String name, String path, int startLine, int endLine, boolean isVul, HashSet<Integer> vulIdSet) {
        this.name = name;
        this.path = path;
        this.startLine = startLine;
        this.endLine = endLine;
        this.isVul = isVul;
        this.vulIdSet = vulIdSet;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Integer i : vulIdSet) {
            sb.append(i).append(" ");
        }
        return name + "\n" + path + "\n" + startLine + " " + endLine + "\n" + sb.toString() + "\n" + isVul + "\n";
    }
}

public class CalcTNAndFP {

    boolean isVulnerability(String s) {
        if (s.toLowerCase().contains("bad")) {
            return true;
        }
        return false;
    }

    HashSet<Integer> getVulIdSetFromName(String name) {
        HashSet<Integer> set = new HashSet<>();
        String[] ss = name.split("_");
        for (String s : ss) {
            if (!s.isEmpty() && s.startsWith("CWE")) {
                set.add(Integer.parseInt(s.substring(3)));
            }
        }
        return set;
    }

    /**
     * 以folder为索引，包含每个folder下的所有函数，
     *
     * @param workSpacePath
     * @return
     */
    HashMap<String, List<Func>> loadFunc(String workSpacePath) {
        HashMap<String, List<Func>> map = new HashMap<>();

        File[] folders = new File(workSpacePath).listFiles();
        assert folders != null;
        for (File folder : folders) {
            List<Func> funcList = new ArrayList<>();

            String cweName = folder.getName();
//            int vulId = Integer.parseInt(cweName.split("_")[0].substring(3));

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

                        HashSet<Integer> set = getVulIdSetFromName(name);
                        Func func = new Func(name, path, startLine, endLine, isVulnerability(name), set);
                        funcList.add(func);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            map.put(cweName, funcList);
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
    boolean calcJuliet(String workSpacePath) {
        boolean result = true;

        HashMap<String, List<Func>> map = loadFunc(workSpacePath);

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

    boolean calcCppCheck(String workSpacePath) {
        boolean result = true;

        String experimentPath = "G:\\share\\experiment\\data.txt";
        File experimentFile = new File(experimentPath);
        if (experimentFile.exists()) {
            experimentFile.delete();
        }

        HashMap<String, List<Func>> map = loadFunc(workSpacePath);

        File[] folders = new File(workSpacePath).listFiles();
        assert folders != null;
        try {
            for (File folder : folders) {
                String folderName = folder.getName();
//                if (!folderName.equals("CWE476_NULL_Pointer_Dereference")) {
//                    continue;
//                }

                // 漏洞函数代表的漏洞id
                HashSet<Integer> vulIdSet = getVulIdSetFromName(folderName);
                System.out.println(folderName);

//                int folderVid = Integer.parseInt(folderName.split("_")[0].substring(3));
                int trueVul = 0;
                int falseVul = 0;

                String decompressPath = folder.getAbsolutePath() + File.separator + "decompress";
                File cppCheck = new File(decompressPath + File.separator + "cppcheck.xml");

                SAXReader saxReader = new SAXReader();
                Document document = saxReader.read(cppCheck);
                Element root = document.getRootElement().element("errors");

                List<Element> elements = root.elements("error");

                List<Func> funcList = map.getOrDefault(folderName, new ArrayList<>());
                List<Boolean> used = new ArrayList<>();
                for (int i = 0; i < funcList.size(); i++) {
                    used.add(false);
                }

                for (Element element : elements) {
                    int scanVulId;
                    if (element.attributeValue("id").equals("arrayIndexOutOfBounds")) {
                        scanVulId = 129;
                    } else if (element.attributeValue("id").equals("bufferAccessOutOfBounds")) {
                        scanVulId = 805;
                    } else {
                        scanVulId = Integer.parseInt(element.attributeValue("cwe"));
                    }

                    List<Element> elementList = element.elements("location");

                    for (Element element1 : elementList) {
                        String[] tmp = element1.attributeValue("file").split("/");
                        String scanPath = tmp[tmp.length - 2] + "/" + tmp[tmp.length - 1];
                        int scanLineNum = Integer.parseInt(element1.attributeValue("line"));

                        boolean found = false;
                        Func scanVulFunc = null;
                        for (int i = 0; i < funcList.size(); i++) {
                            Func func = funcList.get(i);
                            if (scanPath.equals(func.path) && (scanLineNum >= func.startLine && scanLineNum <= func.endLine)) {
                                scanVulFunc = func;
                                if (vulIdSet.contains(scanVulId)) {
                                    used.set(i, true);
                                    found = true;
                                    trueVul++;
                                    break;
                                }
                            }
                        }

                        assert scanVulFunc != null;
                        // 误报 1
                        if (!found) {
                            FileUtils.write(experimentFile, scanPath + "\t" + scanVulFunc.name + "\t" + scanVulFunc.startLine + "\t" + scanVulFunc.endLine + "\t" + 1, StandardCharsets.UTF_8, true);
                            FileUtils.write(experimentFile, "\n", StandardCharsets.UTF_8, true);
                            falseVul++;
                        } else {//正报 0
                            FileUtils.write(experimentFile, scanPath + "\t" + scanVulFunc.name + "\t" + scanVulFunc.startLine + "\t" + scanVulFunc.endLine + "\t" + 0, StandardCharsets.UTF_8, true);
                            FileUtils.write(experimentFile, "\n", StandardCharsets.UTF_8, true);
                        }
                    }
                }

                // 漏报
                for (int i = 0; i < funcList.size(); i++) {
                    if (!used.get(i)) {
                        Func func = funcList.get(i);
                        if (func.isVul) {
                            FileUtils.write(experimentFile, func.path + "\t" + func.name + "\t" + func.startLine + "\t" + func.endLine + "\t" + 2, StandardCharsets.UTF_8, true);
                            FileUtils.write(experimentFile, "\n", StandardCharsets.UTF_8, true);
                        }
                    }
                }

                int totalVul = 0;
                for (Func func : map.get(folderName)) {
                    if (func.isVul) {
                        totalVul++;
                    }
                }
                System.out.println(totalVul);
                System.out.println(trueVul);
                System.out.println(falseVul);
            }
        } catch (DocumentException e) {
            result = false;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    int countUnique(String path) {
        HashSet<String> set = new HashSet<>();
        try {
            List<String> stringList = FileUtils.readLines(new File(path), StandardCharsets.UTF_8);
            set.addAll(stringList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(set.size());
        return set.size();
    }

    public static void main(String[] args) {
        CalcTNAndFP calcTNAndFP = new CalcTNAndFP();
        String workSpacePath = "G:\\share\\workspaceJuliet";

        calcTNAndFP.calcCppCheck(workSpacePath);
//        calcTNAndFP.countUnique("G:\\share\\experiment\\data_CWE476.txt");
    }
}
