package lan.chaos.tools;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraalJSExample {

    public static void main(String[] args) {
        // 示例1: 使用GraalVM Polyglot API (推荐方式)
        System.out.println("=== 使用GraalVM Polyglot API ===");
        polyglotExample();

        // 示例2: 使用ScriptEngine (兼容JSR-223)
        System.out.println("\n=== 使用ScriptEngine API ===");
        scriptEngineExample();

        // 示例3: Java与JavaScript交互
        System.out.println("\n=== Java与JavaScript交互 ===");
        javaJavascriptInteraction();

        // 示例4: 性能测试
        System.out.println("\n=== 性能测试: 计算斐波那契数列 ===");
        performanceTest();
    }

    /**
     * 示例1: 使用GraalVM Polyglot API
     * 这是GraalVM推荐的使用方式
     */
    private static void polyglotExample() {
        try (Context context = Context.create()) {
            // 执行简单的JavaScript代码
            Value result = context.eval("js", "1 + 2");
            System.out.println("1 + 2 = " + result.asInt());

            // 执行多行JavaScript代码
            Value func = context.eval("js",
                    "(function(a, b) {\n" +
                            "  return a * b;\n" +
                            "})"
            );

            // 调用JavaScript函数
            Value multiplyResult = func.execute(5, 3);
            System.out.println("5 * 3 = " + multiplyResult.asInt());

            // 访问JavaScript对象
            context.eval("js",
                    "var person = {\n" +
                            "  name: '张三',\n" +
                            "  age: 30,\n" +
                            "  greet: function() {\n" +
                            "    return '你好，我是' + this.name;\n" +
                            "  }\n" +
                            "}"
            );

            Value person = context.getBindings("js").getMember("person");
            System.out.println("姓名: " + person.getMember("name").asString());
            System.out.println("年龄: " + person.getMember("age").asInt());
            System.out.println("问候: " + person.getMember("greet").execute().asString());
        }
    }

    /**
     * 示例2: 使用ScriptEngine API
     * 兼容传统的JSR-223标准
     */
    private static void scriptEngineExample() {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("graal.js");

            if (engine == null) {
                System.out.println("未找到Graal.js引擎");
                return;
            }

            // 基本运算
            engine.eval("var x = 10; var y = 20;");
            Object result = engine.eval("x + y");
            System.out.println("x + y = " + result);

            // 使用Java对象
            List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
            engine.put("numbers", numbers);
            engine.eval(
                    "var sum = 0;\n" +
                            "for (var i = 0; i < numbers.length; i++) {\n" +
                            "  sum += numbers.get(i);\n" +
                            "}\n" +
                            "sum;"
            );

            // 调用JavaScript函数
            engine.eval(
                    "function factorial(n) {\n" +
                            "  if (n <= 1) return 1;\n" +
                            "  return n * factorial(n - 1);\n" +
                            "}"
            );

            Object factorialResult = engine.eval("factorial(5)");
            System.out.println("5! = " + factorialResult);

        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    /**
     * 示例3: Java与JavaScript的深度交互
     */
    private static void javaJavascriptInteraction() {
        try (Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowHostClassLookup(className -> true)
                .build()) {

            // 1. 从Java传递数据到JavaScript
            Map<String, Object> userData = new HashMap<>();
            userData.put("name", "李四");
            userData.put("score", 95.5);
            userData.put("subjects", new String[]{"数学", "语文", "英语"});

            context.getBindings("js").putMember("userData", userData);

            String processScript =
                    "// 处理Java传递的数据\n" +
                            "var user = userData;\n" +
                            "var message = '学生: ' + user.name + '\\n';\n" +
                            "message += '平均分: ' + user.score.toFixed(1) + '\\n';\n" +
                            "message += '科目: ' + user.subjects.join(', ') + '\\n';\n" +
                            "message += '评级: ' + (user.score >= 90 ? '优秀' : '良好');\n" +
                            "message;";

            Value processed = context.eval("js", processScript);
            System.out.println(processed.asString());

            // 2. 在JavaScript中调用Java方法
            context.getBindings("js").putMember("javaSystem", System.class);

            String callJavaScript =
                    "// 调用Java的System.currentTimeMillis()\n" +
                            "var timestamp = javaSystem.currentTimeMillis();\n" +
                            "var date = new Date(timestamp);\n" +
                            "'当前时间: ' + date.toLocaleString();";

            Value timeResult = context.eval("js", callJavaScript);
            System.out.println(timeResult.asString());

            // 3. 使用Proxy对象
            context.getBindings("js").putMember("calculator", (ProxyExecutable) arguments -> {
                if (arguments.length == 2 && arguments[0].isNumber() && arguments[1].isNumber()) {
                    double a = arguments[0].asDouble();
                    double b = arguments[1].asDouble();
                    String op = arguments.length > 2 ? arguments[2].asString() : "add";

                    switch (op) {
                        case "add":
                            return a + b;
                        case "subtract":
                            return a - b;
                        case "multiply":
                            return a * b;
                        case "divide":
                            return a / b;
                        default:
                            return a + b;
                    }
                }
                return 0;
            });

            Value proxyResult = context.eval("js",
                    "calculator(10, 5, 'multiply')"
            );
            System.out.println("通过Proxy计算 10 * 5 = " + proxyResult.asInt());

        } catch (PolyglotException e) {
            e.printStackTrace();
        }
    }

    /**
     * 示例4: 性能测试
     */
    private static void performanceTest() {
        long startTime, endTime;

        try (Context context = Context.create()) {
            // 定义斐波那契函数
            String fibonacciCode =
                    "function fibonacci(n) {\n" +
                            "  if (n <= 1) return n;\n" +
                            "  return fibonacci(n - 1) + fibonacci(n - 2);\n" +
                            "}";

            context.eval("js", fibonacciCode);
            Value fibonacciFunc = context.getBindings("js").getMember("fibonacci");

            // 测试JavaScript性能
            startTime = System.nanoTime();
            Value jsResult = fibonacciFunc.execute(30);
            endTime = System.nanoTime();
            long jsTime = (endTime - startTime) / 1_000_000;

            System.out.println("JavaScript计算fibonacci(30) = " + jsResult.asInt());
            System.out.println("JavaScript执行时间: " + jsTime + "ms");

            // 比较Java实现
            startTime = System.nanoTime();
            int javaResult = fibonacciJava(30);
            endTime = System.nanoTime();
            long javaTime = (endTime - startTime) / 1_000_000;

            System.out.println("Java计算fibonacci(30) = " + javaResult);
            System.out.println("Java执行时间: " + javaTime + "ms");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Java实现的斐波那契函数，用于性能比较
    private static int fibonacciJava(int n) {
        if (n <= 1) return n;
        return fibonacciJava(n - 1) + fibonacciJava(n - 2);
    }

    /**
     * 示例5: 异步JavaScript执行
     * 这个示例需要额外的方法调用
     */
    public static void executeAsyncJavaScript() {
        try (Context context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .allowExperimentalOptions(true)
                .option("js.esm-eval-returns-exports", "true")
                .build()) {

            // 使用Promise和异步函数
            String asyncCode =
                    "async function fetchData() {\n" +
                            "  // 模拟异步操作\n" +
                            "  return new Promise((resolve) => {\n" +
                            "    setTimeout(() => {\n" +
                            "      resolve({ data: '异步数据加载完成', timestamp: Date.now() });\n" +
                            "    }, 1000);\n" +
                            "  });\n" +
                            "}\n" +
                            "\n" +
                            "// 调用异步函数\n" +
                            "fetchData().then(result => {\n" +
                            "  console.log('异步结果:', result);\n" +
                            "  return result;\n" +
                            "});";

            System.out.println("开始执行异步JavaScript...");
            Value promise = context.eval("js", asyncCode);

            // 等待Promise完成
            promise.invokeMember("then", (ProxyExecutable) args -> {
                System.out.println("Promise完成: " + args[0]);
                return null;
            });

            // 等待一段时间让异步操作完成
            Thread.sleep(1500);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}