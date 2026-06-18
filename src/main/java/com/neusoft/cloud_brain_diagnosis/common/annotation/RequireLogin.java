package com.neusoft.cloud_brain_diagnosis.common.annotation;

import com.neusoft.cloud_brain_diagnosis.common.enums.RoleEnum;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireLogin {
    RoleEnum[] value() default {};
}