import com.opencsv.CSVReader;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * @author cary.shi on 2019/11/3
 */
public class UpdateDB {
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost/vul_enumeration?characterEncoding=utf8&useSSL=false";

    //  Database credentials -- 数据库名和密码自己修改
    private static final String USER = "root";
    private static final String PASS = "123456";

    int convertRiskLevel2Int(String riskLevel) {
        switch (riskLevel) {
            case "Information":
                return 0;
            case "Warning":
                return 1;
            case "Serious":
                return 3;
            case "Critical":
                return 4;
        }
        return -1;
    }

    List<vulnerability> getVulnerabilityList(File xmlFile) {
        List<vulnerability> vulnerabilityList = new ArrayList<>();
        SAXReader saxReader = new SAXReader();
        int vul_id = 1;
        try {
            Document document = saxReader.read(xmlFile);
            Element root = document.getRootElement();
            List<Element> elements = root.elements("section");
            for (Element element : elements) {
                String name = element.attributeValue("name");
                if (name.equals("Checks")) {
                    List<Element> ids = element.elements("id");
                    for (Element id : ids) {
                        String vul_type = id.attributeValue("name");
                        List<Element> subIdList = id.elements("subid");
                        for (Element sub : subIdList) {
                            vulnerability v = new vulnerability();
                            v.vul_id = vul_id++;
                            v.risk_level = convertRiskLevel2Int(sub.attributeValue("severity"));
                            v.vul_name = sub.attributeValue("name");
                            v.vul_type = vul_type;
                            v.overview = sub.attributeValue("rule_name") + "." + sub.attributeValue("desc");
                            v.source = "c++";
                            vulnerabilityList.add(v);
                        }
                    }
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return vulnerabilityList;
    }

    void insertVulnerability() {
        File xmlFile = new File("G:\\share\\VulScanner\\bin\\cfg\\cfg.xml");
        List<vulnerability> vulnerabilityList = getVulnerabilityList(xmlFile);

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            //STEP 2: Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");

            //STEP 3: Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            //STEP 4: Execute a query
            System.out.println("Creating statement...");
            for (vulnerability v : vulnerabilityList) {
                String sql = "INSERT INTO vulnerability(vul_id, risk_level, vul_name, vul_type, overview, source) VALUES(?,?,?,?,?,?)";
                stmt = conn.prepareStatement(sql);

                stmt.setInt(1, v.vul_id);
                stmt.setInt(2, v.risk_level);
                stmt.setString(3, v.vul_name);
                stmt.setString(4, v.vul_type);
                stmt.setString(5, v.overview);
                stmt.setString(6, v.source);

                stmt.executeUpdate();
                stmt.close();
            }

            //STEP 6: Clean-up environment
            conn.close();
        } catch (Exception se) {
            //Handle errors for JDBC
            se.printStackTrace();
        }//Handle errors for Class.forName
        finally {
            //finally block used to close resources
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException ignored) {
            }// nothing we can do
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }//end finally try
        }//end try
    }

    void insertReference() {
        String csvPath = "G:\\share\\vulnerability.csv";
        File csvFile = new File(csvPath);
        HashSet<Integer> set = new HashSet<>();
        try {
            CSVReader csvReader = new CSVReader(new FileReader(csvFile), ';');
            String[] next;
            csvReader.readNext();
            while ((next = csvReader.readNext()) != null) {
                String id = next[6];
                set.add(Integer.parseInt(id));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Integer> list = new ArrayList<>(set);
        Collections.sort(list);

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            for (int ref_id : list) {
                String sql = "INSERT INTO reference(ref_id, ref_name, ref_url) VALUES(?,?,?)";
                stmt = conn.prepareStatement(sql);

                stmt.setInt(1, ref_id);
                stmt.setString(2, "CWE-" + ref_id);
                stmt.setString(3, "https://cwe.mitre.org/data/definitions/" + ref_id + ".html");

                stmt.executeUpdate();
                stmt.close();
            }

            conn.close();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException ignored) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    void insertSolution() {
        HashMap<Integer, Integer> map = new HashMap<>();
        List<Integer> vulIdList = new ArrayList<>();
        HashMap<Integer, Integer> cwe2sol = new HashMap<>();
        try {
            CSVReader csvReader = new CSVReader(new FileReader(new File("G:\\share\\vulnerability.csv")), ';');
            csvReader.readNext();
            String[] next;
            while ((next = csvReader.readNext()) != null) {
                map.put(Integer.parseInt(next[0]), Integer.parseInt(next[next.length - 1]));
                vulIdList.add(Integer.parseInt(next[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        File[] sols = new File("G:\\vul\\solution").listFiles();
//        File[] examples = new File("G:\\vul\\example").listFiles();

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            assert sols != null;
            int id = 1;
            for (File solFile : sols) {
                String name = solFile.getName();
                File exampleFile = new File("G:\\vul\\example" + File.separator + name);

                List<String> solList = FileUtils.readLines(solFile, StandardCharsets.UTF_8);
                List<String> exampleList = FileUtils.readLines(exampleFile, StandardCharsets.UTF_8);
                StringBuilder solBuilder = new StringBuilder();
                for (String s : solList) {
                    solBuilder.append(s);
                    solBuilder.append("\\n");
                }
//                System.out.println(solBuilder.toString());
                StringBuilder exampleBuilder = new StringBuilder();
                for (String s : exampleList) {
                    exampleBuilder.append(s);
                    exampleBuilder.append("\\n");
                }
                cwe2sol.put(Integer.parseInt(name.substring(0, name.indexOf('.'))), id);
//                System.out.println(exampleBuilder.toString());

                String sql = "INSERT INTO solution(sol_id, example, solution) VALUES (?,?,?)";
                stmt = conn.prepareStatement(sql);

                stmt.setInt(1, id);
                stmt.setString(2, exampleBuilder.toString());
                stmt.setString(3, solBuilder.toString());

                stmt.executeUpdate();
                stmt.close();

                id++;
            }

            for (int vulId : vulIdList) {
                String sql2 = "INSERT INTO vul_sol(vul_id, sol_id) VALUES (?,?)";
                PreparedStatement preparedStatement = conn.prepareStatement(sql2);

                preparedStatement.setInt(1, vulId);
                preparedStatement.setInt(2, cwe2sol.get(map.get(vulId)));

                preparedStatement.executeUpdate();
                preparedStatement.close();
            }

        } catch (ClassNotFoundException | SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException ignored) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    void insertVulRef() {
        File vulFile = new File("G:\\share\\vulnerability.csv");
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            CSVReader csvReader = new CSVReader(new FileReader(vulFile), ';');
            csvReader.readNext();
            String[] next;
            while ((next = csvReader.readNext()) != null) {
                int vul_id = Integer.parseInt(next[0]);
                int ref_id = Integer.parseInt(next[next.length - 1]);

                String sql = "INSERT INTO vul_ref(vul_id,ref_id) VALUES (?,?)";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, vul_id);
                stmt.setInt(2, ref_id);

                stmt.executeUpdate();
                stmt.close();
            }

        } catch (ClassNotFoundException | SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException ignored) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        UpdateDB updateDB = new UpdateDB();
//        updateDB.insertReference();
        updateDB.insertSolution();
    }
}

class vulnerability {
    int vul_id;
    int risk_level;
    String vul_name;
    String vul_type;
    String overview;
    String source;
}

class solution {
    int sol_id;
    String example;
    String solution;
}

class reference {
    int ref_id;
    String ref_name;
    String ref_url;
}
