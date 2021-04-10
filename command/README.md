# Examples

## Kotlin

```kotlin
    command<String>("foo") {
        execute { println("Foo") }
        literal("bar") {
            execute { println("Bar") }
            argument(StringArgument("test")) {
                execute {
                    println(get<String>("test"))
                }
                literal("string") {
                    execute {
                        println("String called with arg: " + get<String>("test"))
                    }
                }
            }
        }
    }
```

## Java

```java
nodeBuilder("foo", String.class)
            .then(
                    literal("bar", String.class)
                            .execute(context -> System.out.println("Bar"))
                            .then(
                                    argumentBuilder(stringArgument("test"), String.class)
                                            .execute(context -> System.out.println(context.get("test", String.class)))
                                            .node(
                                                    literal("string", String.class)
                                                            .execute(context -> System.out.println("String called with arg: " + context.get("test", String.class)))
                                            )
                            )
            )
            .execute(context -> System.out.println("Foo"))
            .build();
```

| Command                 | Output                                |
| ------------------------|:-------------------------------------:|
| foo                     | Foo                                   |
| foo bar                 | Bar                                   |
| foo bar <String>        | {String}                              |
| foo bar <String> String | String called with arg: {String}      |

