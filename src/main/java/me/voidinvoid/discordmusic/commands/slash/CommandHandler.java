package me.voidinvoid.discordmusic.commands.slash;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Guardian - 16/12/2020
 * This code was developed by VoidInVoid / Exfusion
 * © 2020
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandHandler {

    String[] value() default {};
}
