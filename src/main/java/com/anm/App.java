package com.anm;

import java.sql.*;

/**
 * Hello world!
 *
 */
public class App 
{
    // 数据库连接字符串（请根据你的配置进行修改）
    private static final String JDBC_URL = "jdbc:mysql://gz-cynosdbmysql-grp-mh2itlkp.sql.tencentcdb.com/aaaaa?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
    // 数据库用户名
    private static final String JDBC_USER = "root";
    // 数据库密码
    private static final String JDBC_PASSWORD = "123456mysql";

    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        String url = "jdbc:mysql://gz-cynosdbmysql-grp-mh2itlkp.sql.tencentcdb.com:29335/aaaaa?nullCatalogMeansCurrent=true&useUnicode=true&serverTimezone=GMT%2b8&characterEncoding=utf-8&useSSL=true";
        String username = "root";
        String password = "123456mysql";
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            // 创建PreparedStatement对象
            String sql = "SELECT * FROM tb_user WHERE id = ?"; // 更改表名为正确的名称
            PreparedStatement pstmt = conn.prepareStatement(sql);
            int id = 1; // 假设我们需要查询ID为1的用户
            pstmt.setInt(1, id); // 设置第一个参数的值
            // 执行查询
            ResultSet rs = pstmt.executeQuery();
            // 处理结果集
            while (rs.next()) {
                System.out.println("id: " + rs.getInt("id"));
                System.out.println("username: " + rs.getString("username"));
                // 处理其他字段...
            }
            // 关闭资源
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
