package com.anm.demo;

import java.util.Optional;

public class Test {
    public static void main(String[] args) {
        try {
            UserRepository userRepository = new UserRepository();
            // 查询用户
            Optional<User> queriedUser = userRepository.selectById(2L);
            queriedUser.ifPresent(u -> System.out.println("Queried User: " + u.getUsername()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
