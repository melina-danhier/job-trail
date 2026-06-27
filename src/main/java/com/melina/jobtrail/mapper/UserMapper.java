package com.melina.jobtrail.mapper;

import com.melina.jobtrail.dto.UserResponse;
import com.melina.jobtrail.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
