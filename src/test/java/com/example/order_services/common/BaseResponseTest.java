package com.example.order_services.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseResponseTest {

    @Test
    void successResponseUsesSuccessCodeAndKeepsData() {
        BaseResponse<String> response = BaseResponse.success("saved");

        assertThat(response.getCode()).isEqualTo(EnumCode.SUCCESS.getCode());
        assertThat(response.getData()).isEqualTo("saved");
        assertThat(response.getMetadata()).isEmpty();
    }
}
