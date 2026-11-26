package com.spa.spabookingservice.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogActivity {
    String module();
    String action();
    String description() default "";
}