package com.skripsi.koma.dto.user;

import java.time.LocalDate;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.skripsi.koma.model.user.UserModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailDTO {
    @JsonProperty(value = "role_id")
    private Long roleId;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "email")
    private String email;

    @JsonProperty(value = "phone_number")
    private String phoneNumber;

    @JsonProperty(value = "password", access = Access.WRITE_ONLY)
    private String password;

    @JsonProperty(value = "gender")
    private String gender;

    @JsonProperty(value = "job")
    private String job;

    @JsonProperty(value = "date_of_birth")
    private LocalDate dateOfBirth;

    @JsonProperty(value = "profile_picture")
    private String profilePicture;

    @JsonProperty(value = "accumulated_point")
    private Integer accumulatedPoint;

    @JsonProperty(value = "user_id")
    private Long userId;

  public static UserDetailDTO mapToDTO(UserModel user) {
    UserDetailDTO dto = new UserDetailDTO();
    BeanUtils.copyProperties(user, dto);
    dto.setRoleId(user.getRoleId() != null ? user.getRoleId().getId() : null);
    dto.setUserId(user.getId());
    return dto;
  }
}
