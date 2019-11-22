package entrance;

import com.opencsv.CSVWriter;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author cary.shi on 2019/11/21
 */
public class GenerateWord2vec {

    List<Double> calcMean(List<String> list, HashMap<String, List<Double>> map, int vecSize) {
        List<Double> res = new ArrayList<>();
        for (int i = 0; i < vecSize; i++) {
            res.add(0.0);
        }
        int size = list.size();
        int cnt = 0;
        for (int i = 0; i < size; i++) {
            if (list.get(i).trim().length() == 0) {
                continue;
            }
            cnt++;

            List<Double> cur = map.get(list.get(i));
            for (int j = 0; j < vecSize; j++) {
                res.set(j, res.get(j) + cur.get(j));
            }
        }
        for (int i = 0; i < vecSize; i++) {
            res.set(i, res.get(i) / cnt);
        }
        return res;
    }

    boolean generateWord2vecForFunction(String corpusPath, String wordOutPath, String funcOutPath) {
        boolean result = true;
        File wordOutFile = new File(wordOutPath);
        HashMap<String, List<Double>> map = new HashMap<>();
        int vecSize = 0;
        try {
            List<String> wordOutList = FileUtils.readLines(wordOutFile, StandardCharsets.UTF_8);
            for (int i = 1; i < wordOutList.size(); i++) {
                String[] data = wordOutList.get(i).split(" ");
                List<Double> vecList = new ArrayList<>();
                vecSize = data.length - 1;
                for (int j = 1; j < data.length; j++) {
                    vecList.add(Double.parseDouble(data[j]));
                }
                map.put(data[0], vecList);
            }
        } catch (IOException e) {
            result = false;
        }

        File corpusFile = new File(corpusPath);
        File funcOutFile = new File(funcOutPath);

        try {
            CSVWriter csvWriter = new CSVWriter(new BufferedWriter(new FileWriter(funcOutFile)));

            List<String> corpusList = FileUtils.readLines(corpusFile, StandardCharsets.UTF_8);
            for (int i = 0; i < corpusList.size(); i++) {
                String[] tmp = corpusList.get(i).split("    ");
                List<String> line = new ArrayList<>();
                Collections.addAll(line, tmp);

                List<Double> vec = calcMean(line, map, vecSize);
                String[] data = new String[vecSize + 1];
                for (int j = 0; j < vec.size(); j++) {
                    data[j] = String.format("%.5f", vec.get(j));
                }
                if (i <= 91) {
                    data[data.length - 1] = "1";
                    csvWriter.writeNext(data);
//                    FileUtils.write(funcOutFile, data.toString(), StandardCharsets.UTF_8, true);
//                    FileUtils.write(funcOutFile, System.getProperty("line.separator"), StandardCharsets.UTF_8, true);
                } else {
                    data[data.length - 1] = "0";
                    csvWriter.writeNext(data);
//                    FileUtils.write(funcOutFile, data.toString(), StandardCharsets.UTF_8, true);
//                    FileUtils.write(funcOutFile, System.getProperty("line.separator"), StandardCharsets.UTF_8, true);
                }
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void main(String[] args) {
        GenerateWord2vec generateWord2vec = new GenerateWord2vec();
        generateWord2vec.generateWord2vecForFunction("G:\\share\\training_data\\corpus.src",
                "G:\\share\\training_data\\word2vec.out", "G:\\share\\training_data\\func.csv");
    }

}
