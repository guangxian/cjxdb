package com.anm.demo;

import com.anm.core.ConditionType;
import com.anm.core.JoinType;
import com.anm.core.Page;
import com.anm.core.QueryWrapper;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 测试
 */
public class Test {
    public static void main(String[] args) {
        try {
            UserRepository userRepository = new UserRepository();

//            RoleRepository roleRepository = new RoleRepository();
//            Role role = roleRepository.selectById(1L).orElseThrow(() -> new RuntimeException("role不存在"));
//
//            // 插入用户
//            User user = new User();
//            user.setUsername("li");
//            user.setPassword("123456");
//            user.setPhoneNumber("13800000");
//            user.setName("li");
//            user.setAge(16);
//            user.setRole(role);
//            user.setCity("北海市");
//            User insertedUser = userRepository.insert(user);
//            System.out.println("Inserted User ID: " + insertedUser.getId());
//
//            // 查询用户
//            Optional<User> queriedUser = userRepository.selectById(insertedUser.getId());
//            queriedUser.ifPresent(u -> System.out.println("Queried User: " + u.getUsername()));
//
//            // 使用 selectOne 动态查询单个用户
//            QueryWrapper<User> wrapperForOne = new QueryWrapper<>();
//            wrapperForOne.eq("username", "li");
//            Optional<User> singleUser = userRepository.selectOne(wrapperForOne);
//            singleUser.ifPresent(u -> System.out.println("Single User: " + u.getUsername()));
//
//            // 更新用户
//            queriedUser.ifPresent(u -> {
//                u.setPassword("654321");
//                try {
//                    userRepository.update(u);
//                } catch (SQLException | IllegalAccessException e) {
//                    e.printStackTrace();
//                }
//            });

//                    .leftJoin("dept", "name", "研发部")
//                    .leftJoin("dept.company", "name", "A公司")

//            wrapper.eq("phoneNumber", "138000")
//                    .gt("age", 16)
//                    .in("city", Arrays.asList("北海市", "上海市", "北京市"))
//                    .like("name", "li")
//                    .leftJoin("role", "id", 1L)
//                    .orderBy("id", "desc");


//            wrapper.add(ConditionType.EQ, "phoneNumber", "13");
//            wrapper.add(ConditionType.LIKE, "role.id", 1L, JoinType.LEFT);
//            wrapper.eq("phoneNumber", "13")
//                    .leftJoin(new QueryWrapper<Role>().eq("id", 1L)).eq("role.id", 1L);
            // 动态查询
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.eq("phoneNumber", "13800000").eq("role.id", 1L);
            List<User> users = userRepository.selectList(wrapper);
            users.forEach(u -> System.out.println("User: " + u.getRole().getName()));

//            // 分页查询
//            Page<User> userPage = userRepository.selectPage(1, 10, wrapper);
//            userPage.getRecords().forEach(u -> System.out.println("Paged User: " + u.getUsername()));
//            System.out.println("Total Users: " + userPage.getTotal());

//            // 计数
//            long count = userRepository.count(wrapper);
//            System.out.println("User Count: " + count);
//
//            // 检查用户是否存在
//            boolean exists = userRepository.exists(wrapperForOne);
//            System.out.println("User exists: " + exists);
//
//            // 删除用户
//            userRepository.deleteById(insertedUser.getId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void s(String con) {

    }
}
