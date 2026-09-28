import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Self-test for .ai/guards/ProtectProjectBoundaries.java
 *
 * The guard is invoked as a real subprocess, so this test covers the same
 * contract an AI tool hook would call: exit codes, the stdout JSON result and
 * the environment variables (PROTECTED_READONLY_ROOTS, PROJECT_ROOT,
 * ALLOW_WRITES_OUTSIDE_PROJECT).
 *
 * Usage (from the project root):
 *   java scripts/TestGuard.java
 *
 * When running from elsewhere, pass the project root explicitly:
 *   java scripts/TestGuard.java D:\path\to\project
 */
public final class TestGuard {

    private static final boolean WINDOWS = File.separatorChar == '\\';
    private static final String JAVA = Paths
            .get(System.getProperty("java.home"), "bin", WINDOWS ? "java.exe" : "java")
            .toString();

    private static int passed;
    private static int failed;

    private TestGuard() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // Pin stdout to UTF-8 so the report reads the same in Git Bash, Windows
        // Terminal and CI instead of following the console code page.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        Path projectRoot = args.length > 0
                ? Paths.get(args[0]).toAbsolutePath().normalize()
                : Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path guard = projectRoot.resolve(".ai").resolve("guards").resolve("ProtectProjectBoundaries.java");

        if (!Files.isRegularFile(guard)) {
            System.err.println("找不到 guard: " + guard);
            System.err.println("请在项目根目录执行，或把项目根目录作为参数传入。");
            System.exit(1);
            return;
        }

        String protectedRoot = WINDOWS ? "D:\\Protected\\Backend" : "/tmp/protected/backend";
        String insideProtected = protectedRoot + File.separator + "x.java";
        String insideProject = projectRoot.resolve("src").resolve("x.java").toString();
        String outsideProject = outsideProjectPath(projectRoot);

        Map<String, String> projectScoped = Map.of("PROJECT_ROOT", projectRoot.toString());
        Map<String, String> withProtectedRoot = Map.of(
                "PROTECTED_READONLY_ROOTS", protectedRoot,
                "PROJECT_ROOT", projectRoot.toString());

        // --command：命令文本中的受保护根识别。项目边界不适用于 --command（见 guard 类注释）。
        if (WINDOWS) {
            expectCommand(guard, withProtectedRoot, "rm D:\\Protected\\Backend\\x", true, "canonical Windows path");
            expectCommand(guard, withProtectedRoot, "rm d:\\protected\\backend\\x", true, "case-insensitive Windows path");
            expectCommand(guard, withProtectedRoot, "rm D:/Protected/Backend/x", true, "forward slash Windows path");
            expectCommand(guard, withProtectedRoot, "rm D:\\Protected\\BackendBackup\\x", false, "similar prefix is allowed");
        } else {
            expectCommand(guard, withProtectedRoot, "rm /tmp/protected/backend/x", true, "canonical POSIX path");
            expectCommand(guard, withProtectedRoot, "rm /tmp/protected/backend-backup/x", false, "similar prefix is allowed");
        }
        expectCommand(guard, withProtectedRoot, "mvn -q test", false, "unrelated command is allowed");

        // --path：受保护只读根判定
        expectPath(guard, withProtectedRoot, projectRoot, insideProtected, true, "protected file path");
        expectReason(guard, withProtectedRoot, insideProtected, "protected read-only root",
                "protected-root denial reports its own reason");

        // --path：项目边界判定（外部文件只读）
        expectPath(guard, projectScoped, projectRoot, insideProject, false, "path inside the project root is allowed");
        expectPath(guard, projectScoped, projectRoot, outsideProject, true, "path outside the project root is denied");
        expectReason(guard, projectScoped, outsideProject, "outside the project root",
                "project-boundary denial reports its own reason");

        // 手动授权可越过项目边界
        expectPath(guard, Map.of(
                "PROJECT_ROOT", projectRoot.toString(),
                "ALLOW_WRITES_OUTSIDE_PROJECT", "true"), projectRoot, outsideProject, false,
                "explicit authorization allows writing outside the project");

        // PROJECT_ROOT 未设置时回退到进程工作目录（子进程的工作目录即项目根）
        expectPath(guard, Map.of("PROJECT_ROOT", ""), projectRoot, insideProject, false,
                "project root falls back to the working directory");

        // 参数非法
        expectInvalidInvocation(guard, protectedRoot);

        System.out.println();
        System.out.println("通过 " + passed + "，失败 " + failed + "。");
        System.exit(failed == 0 ? 0 : 1);
    }

    private record Result(int exitCode, String stdout) {
    }

    /** A sibling of the project root, guaranteed to be outside it. */
    private static String outsideProjectPath(Path projectRoot) {
        Path parent = projectRoot.getParent();
        Path base = parent != null ? parent : projectRoot;
        return base.resolve("some-other-project").resolve("x.java").toString();
    }

    private static Result runGuard(Path guard, Map<String, String> environment, String... arguments)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(JAVA);
        command.add(guard.toString());
        command.addAll(List.of(arguments));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        // 先清空本测试关心的变量，让每个用例的环境都是确定的，不受外层 shell 影响
        builder.environment().remove("PROTECTED_READONLY_ROOTS");
        builder.environment().remove("PROJECT_ROOT");
        builder.environment().remove("ALLOW_WRITES_OUTSIDE_PROJECT");
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            if (entry.getValue().isEmpty()) {
                builder.environment().remove(entry.getKey());
            } else {
                builder.environment().put(entry.getKey(), entry.getValue());
            }
        }

        Process process = builder.start();
        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        int exitCode = process.waitFor();
        return new Result(exitCode, stdout);
    }

    private static void expectCommand(Path guard, Map<String, String> environment, String commandText,
                                      boolean expectDeny, String name) throws IOException, InterruptedException {
        check(expectDeny, runGuard(guard, environment, "--command", commandText), name);
    }

    private static void expectPath(Path guard, Map<String, String> environment, Path cwd, String target,
                                   boolean expectDeny, String name) throws IOException, InterruptedException {
        check(expectDeny, runGuard(guard, environment, "--path", target, "--cwd", cwd.toString()), name);
    }

    /** 两种拒绝必须能区分开，否则调用方无法判断该找哪条规则。 */
    private static void expectReason(Path guard, Map<String, String> environment, String target,
                                     String expectedFragment, String name) throws IOException, InterruptedException {
        Result result = runGuard(guard, environment, "--path", target);
        boolean ok = result.exitCode() == 2 && result.stdout().contains(expectedFragment);
        report(ok, name, describe(result));
    }

    private static void expectInvalidInvocation(Path guard, String root) throws IOException, InterruptedException {
        Result result = runGuard(guard, Map.of(), "--path", root, "--command", "rm " + root);
        report(result.exitCode() == 1, "conflicting flags exit 1", describe(result));
    }

    private static void check(boolean expectDeny, Result result, String name) {
        boolean ok = expectDeny
                ? result.exitCode() == 2 && result.stdout().contains("\"decision\":\"deny\"")
                : result.exitCode() == 0 && result.stdout().contains("\"decision\":\"allow\"");
        report(ok, name, describe(result));
    }

    private static String describe(Result result) {
        return "exit=" + result.exitCode() + " stdout=" + result.stdout();
    }

    private static void report(boolean ok, String name, String detail) {
        if (ok) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name + "  " + detail);
        }
    }
}
