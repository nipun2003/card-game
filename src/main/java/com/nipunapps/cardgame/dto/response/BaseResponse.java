package com.nipunapps.cardgame.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nipunapps.cardgame.dto.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {

    private boolean success;
    private String message;
    private String errorCode;
    private T data;


    public BaseResponse(ErrorCode errorCode) {
        this.success = false;
        this.message = errorCode.getMessage();
        this.errorCode = errorCode.getCode();
    }
}
