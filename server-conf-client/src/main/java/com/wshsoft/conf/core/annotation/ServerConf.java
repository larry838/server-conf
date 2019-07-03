package com.wshsoft.conf.core.annotation;

import java.lang.annotation.*;

/**
 * server conf annotaion  (only support filed)
 *
 * @author Carry_xie 2018-02-04 00:34:30
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ServerConf {

    /**
     * conf key
     *
     * @return
     */
    String value();

    /**
     * conf default value
     *
     * @return
     */
    String defaultValue() default "";

    /**
     *  whether you need a callback refresh, when the value changes.
     *
     * @return
     */
    boolean callback() default true;
}