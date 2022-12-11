package org.ds2os.vsl.core.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This Annotation contains the id (from the initial configuration file) and description of the
 * annotated configuration method.
 *
 * @author liebald
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigDescription {
    /**
     * The id of the configuration option as it must be specified in the configuration file (e.g.
     * kor.archive.enabled).
     *
     * @return The id of the annotated configuration method
     */
    String id();

    /**
     * The description of the annotated configuration method (What does the configuration method
     * do).
     *
     * @return Description of the annotated configuration method.
     */
    String description();

    /**
     * Restrictions on this field.
     *
     * @return String describing possible restrictions for the value of the configuration option.
     */
    String restrictions() default "";

    /**
     * Default value.
     *
     * @return Default value of the configuration option.
     */
    String defaultValue();
}
