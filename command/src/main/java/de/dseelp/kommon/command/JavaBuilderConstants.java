package de.dseelp.kommon.command;

import de.dseelp.kommon.command.arguments.*;

public class JavaBuilderConstants {
    public static <T> JavaCommandBuilder<T> nodeBuilder(String name, String[] aliases, Class<T> type) {
        return new JavaCommandBuilder<T>(name, aliases);
    }

    public static <T> JavaCommandBuilder<T> nodeBuilder(String name, Class<T> type) {
        return nodeBuilder(name, new String[]{}, type);
    }

    public static <T> JavaCommandBuilder<T> argumentBuilder(Argument<?> argument, Class<T> type) {
        return new JavaCommandBuilder<T>(argument);
    }


    public static BooleanArgument booleanArgument(String name, Boolean optional) {
        return new BooleanArgument(name, optional);
    }

    public static BooleanArgument booleanArgument(String name) {
        return booleanArgument(name, false);
    }

    public static BooleanArgument optionalBooleanArgument(String name) {
        return booleanArgument(name, true);
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
