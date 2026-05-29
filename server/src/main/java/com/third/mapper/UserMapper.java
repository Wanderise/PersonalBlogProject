package com.third.mapper;

import com.third.pojo.dto.UserDTO;
import com.third.pojo.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {
    @Insert("insert into blog.user(name, gmt_create, gmt_modified, password, image, level) " +
            "VALUES (#{name}, #{gmtCreate}, #{gmtModified}, #{password}, #{image}, #{level})")
    void registerUser(User user);

    @Select("select id, name, image, level, gmt_create, password from blog.user where name = #{name}")
    User login(UserDTO user);

    @Select("select id, name, gmt_create, gmt_modified, password, image, level from blog.user where name = #{userName}")
    User getUserInfoByName(String userName);

    @Update("update blog.user SET name = #{name}, gmt_modified = #{now} where name = #{userName}")
    void updateUserName(String userName, String name, LocalDateTime now);

    @Select("select id, name, gmt_create, gmt_modified, password, image, level from blog.user where id = #{userId}")
    User getUserInfoById(int userId);

    @Update("update blog.user SET image = #{image} where name = #{name}")
    void updateUserAvatar(User user);
}
