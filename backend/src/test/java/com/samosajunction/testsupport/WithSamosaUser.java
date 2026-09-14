package com.samosajunction.testsupport;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithSamosaUserSecurityContextFactory.class)
public @interface WithSamosaUser {

    String id() default "11111111-1111-4111-8111-111111111111";

    String email() default "ada@samosa.test";

    String role() default "CUSTOMER";
}
