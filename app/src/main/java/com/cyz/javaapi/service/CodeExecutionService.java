package com.cyz.javaapi.service;

import com.cyz.javaapi.model.CodeResult;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.*;

@Service
public class CodeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutionService.class);
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/javaapi-test/";
    private static final long TIMEOUT_SECONDS = 10;
    private static final String TEST_CLASS_NAME = "TempTest";
    private static final String TEST_PACKAGE = "com.cyz.javaapi.test";

    // 黑名单：禁止使用的类前缀
    private static final String[] BLACKLIST_PREFIXES = {
            "java.io.File", "java.io.FileOutputStream", "java.io.FileInputStream",
            "java.io.FileWriter", "java.io.FileReader", "java.io.RandomAccessFile",
            "java.net.", "java.lang.Runtime", "java.lang.ProcessBuilder",
            "java.lang.reflect.", "javax.tools.", "java.sql.",
    };

    public CodeResult execute(String testName, String code) {
        long startTime = System.currentTimeMillis();
        CodeResult result = new CodeResult();

        try {
            // 1. 安全检查
            String securityError = checkSecurity(code);
            if (securityError != null) {
                result.setSuccess(false);
                result.setPassed(false);
                result.setError(securityError);
                result.setExecutionTime(System.currentTimeMillis() - startTime);
                return result;
            }

            // 2. 包装代码为完整测试类
            String fullClass = wrapTestClass(code);

            // 3. 写入临时文件
            Path tempDir = Paths.get(TEMP_DIR);
            Files.createDirectories(tempDir);

            Path pkgDir = tempDir.resolve(TEST_PACKAGE.replace('.', '/'));
            Files.createDirectories(pkgDir);

            Path sourceFile = pkgDir.resolve(TEST_CLASS_NAME + ".java");
            Files.write(sourceFile, fullClass.getBytes(StandardCharsets.UTF_8));

            // 4. 编译
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                result.setSuccess(false);
                result.setError("未找到 Java 编译器（请确认使用 JDK 而非 JRE）");
                result.setExecutionTime(System.currentTimeMillis() - startTime);
                return result;
            }

            String classpath = buildCompileClasspath();

            List<String> compileOptions = Arrays.asList(
                    "-cp", classpath,
                    "-encoding", "UTF-8",
                    "-d", tempDir.toAbsolutePath().toString(),
                    sourceFile.toAbsolutePath().toString()
            );

            ByteArrayOutputStream compileErrStream = new ByteArrayOutputStream();
            int compileResult = compiler.run(null, null, compileErrStream,
                    compileOptions.toArray(new String[0]));

            if (compileResult != 0) {
                String errorMsg = compileErrStream.toString(StandardCharsets.UTF_8.name());
                errorMsg = errorMsg.replace(sourceFile.toAbsolutePath().toString(), "TempTest.java");
                result.setSuccess(false);
                result.setError("编译错误:\n" + errorMsg);
                result.setExecutionTime(System.currentTimeMillis() - startTime);
                cleanup(tempDir);
                return result;
            }

            // 5. 加载并执行
            try {
                try (URLClassLoader classLoader = new URLClassLoader(
                        new URL[]{tempDir.toAbsolutePath().toFile().toURI().toURL()},
                        getClass().getClassLoader())) {

                    Class<?> testClass = classLoader.loadClass(TEST_PACKAGE + "." + TEST_CLASS_NAME);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    PrintStream printStream = new PrintStream(outputStream, true, "UTF-8");
                    PrintStream originalOut = System.out;

                    try {
                        System.setOut(printStream);

                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Future<Result> future = executor.submit(() -> JUnitCore.runClasses(testClass));

                        Result junitResult;
                        try {
                            junitResult = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        } catch (TimeoutException e) {
                            future.cancel(true);
                            result.setSuccess(false);
                            result.setError("执行超时（超过 " + TIMEOUT_SECONDS + " 秒）");
                            result.setExecutionTime(System.currentTimeMillis() - startTime);
                            return result;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            result.setSuccess(false);
                            result.setError("执行被中断");
                            result.setExecutionTime(System.currentTimeMillis() - startTime);
                            return result;
                        } catch (ExecutionException e) {
                            result.setSuccess(true);
                            result.setPassed(false);
                            Throwable cause = e.getCause();
                            String errorMsg = (cause != null) ? cause.getMessage() : e.getMessage();
                            result.setError("运行时异常: " + errorMsg);
                            result.setExecutionTime(System.currentTimeMillis() - startTime);
                            return result;
                        } finally {
                            executor.shutdownNow();
                        }

                        String output = outputStream.toString("UTF-8").trim();

                        if (junitResult.wasSuccessful()) {
                            result.setSuccess(true);
                            result.setPassed(true);
                            result.setOutput(output);
                        } else {
                            StringBuilder errorBuilder = new StringBuilder();
                            for (Failure failure : junitResult.getFailures()) {
                                errorBuilder.append(failure.getMessage()).append("\n");
                                if (failure.getException() != null) {
                                    String trace = getRelevantTrace(failure.getException());
                                    errorBuilder.append(trace);
                                }
                            }
                            result.setSuccess(true);
                            result.setPassed(false);
                            result.setError(errorBuilder.toString().trim());
                            result.setOutput(output);
                        }
                    } finally {
                        System.setOut(originalOut);
                        printStream.close();
                    }
                }

                result.setExecutionTime(System.currentTimeMillis() - startTime);
            } finally {
                cleanup(tempDir);
            }

        } catch (Exception e) {
            log.error("执行测试异常", e);
            result.setSuccess(false);
            result.setError("执行异常: " + e.getMessage());
            result.setExecutionTime(System.currentTimeMillis() - startTime);
        }

        return result;
    }

    private String checkSecurity(String code) {
        for (String prefix : BLACKLIST_PREFIXES) {
            if (code.contains(prefix)) {
                return "代码包含不允许使用的类: " + prefix;
            }
        }

        if (code.contains("System.exit(") || code.contains("System.setSecurityManager(")) {
            return "代码包含禁止的系统调用";
        }

        return null;
    }

    private String wrapTestClass(String code) {
        return "package " + TEST_PACKAGE + ";\n\n" +
                "import static org.junit.Assert.*;\n" +
                "import org.junit.Test;\n" +
                "import java.util.concurrent.*;\n" +
                "import java.util.function.*;\n" +
                "import java.util.stream.*;\n" +
                "import java.util.*;\n" +
                "import java.util.concurrent.locks.*;\n" +
                "import java.math.*;\n" +
                "import java.time.*;\n" +
                "import java.time.format.*;\n" +
                "import java.time.temporal.*;\n\n" +
                "public class " + TEST_CLASS_NAME + " {\n" +
                "    @Test\n" +
                "    public void testMethod() throws Exception {\n" +
                "        " + code.replace("\n", "\n        ") + "\n" +
                "    }\n" +
                "}";
    }

    /**
     * 构建编译 classpath：从系统属性 classpath 和运行时 classloader 中发现的 jar 路径。
     * 当从 Spring Boot fat jar 运行时，嵌套 jar 在 BOOT-INF/lib/ 下，需要提取到临时文件。
     */
    private String buildCompileClasspath() {
        Set<String> paths = new LinkedHashSet<>();

        // 1. 系统属性 classpath
        String sysCp = System.getProperty("java.class.path");
        if (sysCp != null) {
            for (String p : sysCp.split(File.pathSeparator)) {
                if (!p.trim().isEmpty()) paths.add(p.trim());
            }
        }

        // 2. 从 JUnit 的 Assert 类找到 JUnit jar
        addJarPath(org.junit.Assert.class, paths);
        // 3. 从 Hamcrest 找到 hamcrest jar
        try {
            addJarPath(Class.forName("org.hamcrest.Matcher"), paths);
        } catch (ClassNotFoundException ignored) {}

        // 4. 如果 JUnit jar 未找到（可能嵌套在 fat jar 中），从 fat jar 中提取
        boolean junitFound = false;
        for (String p : paths) {
            if (p.toLowerCase().contains("junit") || p.toLowerCase().contains("hamcrest")) {
                junitFound = true;
                break;
            }
        }
        if (!junitFound) {
            String extracted = extractNestedJars();
            if (extracted != null) paths.add(extracted);
        }

        return String.join(File.pathSeparator, paths);
    }

    /**
     * 从 Spring Boot fat jar 的 BOOT-INF/lib/ 中提取 junit + hamcrest jar 到临时文件。
     * 返回提取后的 classpath 片段（junit.jar:hamcrest.jar）。
     */
    private String extractNestedJars() {
        try {
            String mainJar = System.getProperty("java.class.path");
            if (mainJar == null || !mainJar.endsWith(".jar")) return null;

            java.util.zip.ZipFile zip = new java.util.zip.ZipFile(mainJar);
            Path tmpDir = Paths.get(TEMP_DIR);
            Files.createDirectories(tmpDir);

            StringBuilder sb = new StringBuilder();
            String[] prefixes = {"junit", "hamcrest"};
            for (String prefix : prefixes) {
                java.util.zip.ZipEntry entry = null;
                java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    java.util.zip.ZipEntry e = entries.nextElement();
                    String name = e.getName();
                    if (name.startsWith("BOOT-INF/lib/" + prefix) && name.endsWith(".jar")) {
                        entry = e;
                        break;
                    }
                }
                if (entry != null) {
                    String fileName = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
                    Path outFile = tmpDir.resolve(fileName);
                    if (!Files.exists(outFile)) {
                        try (InputStream is = zip.getInputStream(entry)) {
                            Files.copy(is, outFile);
                        }
                    }
                    log.info("提取嵌套 jar: {} -> {}", entry.getName(), outFile);
                    if (sb.length() > 0) sb.append(File.pathSeparator);
                    sb.append(outFile.toAbsolutePath().toString());
                }
            }
            zip.close();
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) {
            log.warn("提取嵌套 jar 失败", e);
            return null;
        }
    }

    private void addJarPath(Class<?> clazz, Set<String> paths) {
        try {
            ProtectionDomain pd = clazz.getProtectionDomain();
            if (pd != null) {
                CodeSource cs = pd.getCodeSource();
                if (cs != null && cs.getLocation() != null) {
                    String jarPath = cs.getLocation().getFile();
                    // 过滤掉 jar 内嵌路径（含 !）和不存在文件，仅接受磁盘上的真实 jar
                    if (jarPath != null && !jarPath.isEmpty()
                            && !jarPath.contains("!") && new File(jarPath).isFile()) {
                        paths.add(jarPath);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Cannot resolve jar path for {}", clazz.getName(), e);
        }
    }

    private String getRelevantTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] trace = t.getStackTrace();
        boolean found = false;
        for (StackTraceElement element : trace) {
            if (element.getClassName().contains(TEST_CLASS_NAME)) {
                found = true;
            }
            if (found) {
                sb.append("  at ").append(element.toString()).append("\n");
                if (element.getLineNumber() > 0
                        && element.getClassName().contains(TEST_CLASS_NAME)) {
                    break;
                }
            }
        }
        return sb.toString();
    }

    private void cleanup(Path tempDir) {
        try {
            File tempDirFile = tempDir.toFile();
            if (tempDirFile.exists()) {
                deleteDirectory(tempDirFile);
            }
        } catch (Exception e) {
            log.warn("清理临时文件失败", e);
        }
    }

    private void deleteDirectory(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteDirectory(f);
            }
        }
        file.delete();
    }
}
