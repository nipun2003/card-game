package com.nipunapps.cardgame.dto.request;

import com.nipunapps.cardgame.entity.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class UserCreateRequestDto {

    @Email
    @NotBlank
    @NotNull
    private String email;

    @Length(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @NotBlank
    @Length(min = 8, max = 25, message = "Password must be between 8 and 25 characters")
    private String password;
    private String profilePictureUrl = "";

    public UserEntity toEntity() {
        return UserEntity.builder()
                .email(this.email)
                .name(this.name)
                .profilePictureUrl(this.profilePictureUrl)
                .build();
    }

}
