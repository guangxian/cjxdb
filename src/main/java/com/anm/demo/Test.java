package com.anm.demo;

import com.anm.core.Page;
import com.anm.core.QueryWrapper;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Test {
    public static void main(String[] args) {
        try {
            UserRepository userRepository = new UserRepository();

            // 插入用户
            User user = new User();
            user.setUsername("li");
            user.setPassword("123456");
            user.setPhoneNumber("13800000");
            User insertedUser = userRepository.insert(user);
            System.out.println("Inserted User ID: " + insertedUser.getId());

            // 查询用户
            Optional<User> queriedUser = userRepository.selectById(insertedUser.getId());
            queriedUser.ifPresent(u -> System.out.println("Queried User: " + u.getUsername()));

            // 使用 selectOne 动态查询单个用户
            QueryWrapper<User> wrapperForOne = new QueryWrapper<>();
            wrapperForOne.eq("username", "li");
            Optional<User> singleUser = userRepository.selectOne(wrapperForOne);
            singleUser.ifPresent(u -> System.out.println("Single User: " + u.getUsername()));

            // 更新用户
            queriedUser.ifPresent(u -> {
                u.setPassword("654321");
                try {
                    userRepository.update(u);
                } catch (SQLException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            });

            // 动态查询
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq("age", 16)
                    .in("city", Arrays.asList("北海市", "上海市", "北京市"))
                    .like("name", "li")
                    .leftJoin("dept", "name", "研发部")
                    .leftJoin("dept.company", "name", "A公司")
                    .leftJoin("role", "id", 1L)
                    .orderBy("id", "desc");
            List<User> users = userRepository.selectList(wrapper);
            users.forEach(u -> System.out.println("User: " + u.getUsername()));

            // 分页查询
            Page<User> userPage = userRepository.selectPage(1, 10, wrapper);
            userPage.getRecords().forEach(u -> System.out.println("Paged User: " + u.getUsername()));
            System.out.println("Total Users: " + userPage.getTotal());

            // 计数
            long count = userRepository.count(wrapper);
            System.out.println("User Count: " + count);

            // 检查用户是否存在
            boolean exists = userRepository.exists(wrapperForOne);
            System.out.println("User exists: " + exists);

            // 删除用户
            userRepository.deleteById(insertedUser.getId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
