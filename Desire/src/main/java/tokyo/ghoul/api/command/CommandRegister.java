package tokyo.ghoul.api.command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandRegister {
    String name();
    String[] aliases() default {};
}