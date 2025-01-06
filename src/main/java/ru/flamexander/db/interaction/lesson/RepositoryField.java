package ru.flamexander.db.interaction.lesson;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
public @interface RepositoryField {
    String name() default "";
}
