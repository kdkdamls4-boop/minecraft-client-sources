package tokyo.ghoul.api.module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleRegister {
    String name();
    String description() default "";
    Category category();
    int bind() default -999;
    boolean hidden() default false;
}
