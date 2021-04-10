package de.dseelp.kommon.command;

import de.dseelp.kommon.command.arguments.*;

public class JavaBuilderConstants {
    public static <S> JavaCommandBuilder<S> literal(String name, String[] aliases, Class<S> type) {
        return new JavaCommandBuilder<>(name, aliases);
    }

    public static <S> JavaCommandBuilder<S> literal(String name, Class<S> type) {
        return literal(name, new String[]{}, type);
    }

    public static <S> JavaCommandBuilder<S> argument(Argument<?> argument, Class<S> type) {
        return new JavaCommandBuilder<>(argument);
    }


    public static BooleanArgument bool(String name, Boolean optional) {
        return new BooleanArgument(name, optional);
    }

    public static BooleanArgument bool(String name) {
        return bool(name, false);
    }
    public static BooleanArgument bool(String name, String trueString, String falseString) {
        return new BooleanArgument(name, false, trueString, falseString);
    }

    public static BooleanArgument optionalbool(String name) {
        return bool(name, true);
    }
    public static BooleanArgument optionalbool(String name, String trueString, String falseString) {
        return new BooleanArgument(name, true, trueString, falseString);
    }


    public static IntArgument integerArgument(String name, Boolean optional) {
        return new IntArgument(name, optional);
    }

    public static IntArgument integerArgument(String name) {
        return integerArgument(name, false);
    }

    public static IntArgument optionalIntegerArgument(String name) {
        return integerArgument(name, true);
    }


    public static UUIDArgument uniqueIdArgument(String name, Boolean optional) {
        return new UUIDArgument(name, optional);
    }

    public static UUIDArgument uniqueIdArgument(String name) {
        return uniqueIdArgument(name, false);
    }

    public static UUIDArgument optionalUniqueIdArgument(String name) {
        return uniqueIdArgument(name, true);
    }


    public static StringArgument stringArgument(String name, Boolean optional) {
        return new StringArgument(name, optional);
    }

    public static StringArgument stringArgument(String name) {
        return stringArgument(name, false);
    }

    public static StringArgument optionalStringArgument(String name) {
        return stringArgument(name, true);
    }
}
