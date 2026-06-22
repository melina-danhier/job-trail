package com.melina.jobtrail.mapper;

import com.melina.jobtrail.dto.UserDto;
import com.melina.jobtrail.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
