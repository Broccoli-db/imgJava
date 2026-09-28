import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tool-neutral protected-root guard.
 *
 * Usage:
 *   java .ai/guards/ProtectProjectBoundaries.java --path PATH [--cwd DIR]
 *   java .ai/guards/ProtectProjectBoundaries.java --command COMMAND
 *
 * Exit 0 allows, exit 2 denies, exit 1 means invalid invocation. The result
 * is printed to stdout as {"decision":"allow|deny","reason":"..."}.
 *
 * Two independent checks apply to --path:
 *
 *   1. PROTECTED_READONLY_ROOTS (platform path separator separated) — a path
 *      inside any listed root is denied. Unset or empty means no such roots.
 *   2. The project root — a path OUTSIDE it is denied, because everything
 *      outside the project is read-only (see .ai/CORE.md). The root comes from
 *      the PROJECT_ROOT environment variable, falling back to the process
 *      working directory so the check works with no configuration when the
 *      guard is run from the project root. Setting
 *      ALLOW_WRITES_OUTSIDE_PROJECT=true skips this check; that is a manual
 *      override and must only be used with explicit user authorization.
 *
 * --command deliberately does NOT enforce the project boundary. A command
 * string cannot be classified as read or write, so denying every command that
 * mentions an outside absolute path would also deny legitimate reads
 * (`cat /elsewhere/file`, grepping a reference project). --command therefore
 * only flags commands that explicitly reference a root listed in
 * PROTECTED_READONLY_ROOTS.
 *
 * This heuristic guard is NOT a sandbox. It only inspects the paths and
 * commands it is explicitly handed; environment indirection, symlinks,
 * writes performed by scripts it never sees, and tools that do not call it
 * are outside its reach. Use OS permissions, read-only mounts or a sandbox
 * for real isolation.
 *
 * Self-test: java scripts/TestGuard.java
 */
public final class ProtectProjectBoundaries {

    private static final boolean WINDOWS = File.separatorChar == '\\';

    /** Explicit project root; when unset the working directory is used. */
    private static final String PROJECT_ROOT_ENV = "PROJECT_ROOT";

    /**
     * Manual override for the project-root check. Only set this to {@code true}
     * when the user has explicitly authorized editing files outside the project.
     */
    private static final String ALLOW_OUTSIDE_PROJECT_ENV = "ALLOW_WRITES_OUTSIDE_PROJECT";

    /**
     * Characters that may legally follow a root path inside a command line.
     * Mirrors the character set used by the reference Node.js guard.
     */
    private static final String COMMAND_BOUNDARY = "[\\s\"'`;|&<>(),]";

    private ProtectProjectBoundaries() {
    }

    public static void main(String[] args) {
        // The result is a machine-readable interface, so pin stdout to UTF-8.
        // Otherwise a non-ASCII path echoed into the reason field would be
        // encoded with the platform console code page and misread by callers.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        Parsed parsed = parseArguments(args);
        if (parsed == null) {
            System.err.println("Usage: --path PATH [--cwd DIR] | --command COMMAND");
            System.exit(1);
            return;
        }

        List<Path> roots = configuredRoots(System.getenv("PROTECTED_READONLY_ROOTS"));
        Decision decision = parsed.path() != null
                ? evaluatePath(parsed.path(), parsed.cwd(), roots, configuredProjectRoot())
                : evaluateCommand(parsed.command(), roots);

        System.out.println(
                "{\"decision\":\"" + decision.decision() + "\",\"reason\":\"" + escapeJson(decision.reason()) + "\"}");
        System.out.flush();
        System.exit("deny".equals(decision.decision()) ? 2 : 0);
    }

    /** Result of a guard decision. */
    public record Decision(String decision, String reason) {
    }

    /** Parsed command line; {@code path} and {@code command} are mutually exclusive. */
    public record Parsed(String path, String command, String cwd) {
    }

    /**
     * Splits the raw environment value into absolute, normalized roots.
     * Blank segments are ignored; a missing or empty value means "no roots".
     */
    public static List<Path> configuredRoots(String raw) {
        List<Path> roots = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return roots;
        }
        for (String item : raw.split(Pattern.quote(File.pathSeparator), -1)) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                roots.add(Paths.get(trimmed).toAbsolutePath().normalize());
            }
        }
        return roots;
    }

    /**
     * The project root: {@code PROJECT_ROOT} when set, otherwise the process
     * working directory. Never null, so the boundary check is always active.
     */
    public static Path configuredProjectRoot() {
        String raw = System.getenv(PROJECT_ROOT_ENV);
        if (raw != null && !raw.isBlank()) {
            return Paths.get(raw.trim()).toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    }

    /** True when the caller has manually authorized writing outside the project. */
    public static boolean writesOutsideProjectAllowed() {
        return "true".equalsIgnoreCase(System.getenv(ALLOW_OUTSIDE_PROJECT_ENV));
    }

    /**
     * Canonical form used for comparisons: absolute, lexically normalized, and
     * on Windows additionally case-folded with '/' converted to '\'. Case is
     * folded explicitly rather than relying on platform path comparison rules.
     *
     * Symlinks are intentionally not resolved, matching the reference guard.
     */
    public static Path canonical(Path value) {
        Path absolute = value.toAbsolutePath().normalize();
        if (!WINDOWS) {
            return absolute;
        }
        return Paths.get(absolute.toString().replace('/', '\\').toLowerCase(Locale.ROOT));
    }

    /** Resolves a possibly relative target against {@code cwd}. */
    public static Path resolveTarget(String value, String cwd) {
        Path path = Paths.get(value);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        String base = (cwd == null || cwd.isBlank()) ? System.getProperty("user.dir", ".") : cwd;
        return Paths.get(base).resolve(path).normalize();
    }

    /** Returns true when {@code target} is {@code root} itself or inside it. */
    public static boolean isWithin(Path target, Path root) {
        Path canonicalTarget = canonical(target);
        Path canonicalRoot = canonical(root);
        Path relative;
        try {
            relative = canonicalRoot.relativize(canonicalTarget);
        } catch (IllegalArgumentException ex) {
            // Different roots or drives (for example C: against D: on Windows)
            // cannot be related, so the target is not inside the protected root.
            return false;
        }
        if (relative.toString().isEmpty()) {
            return true;
        }
        if (relative.isAbsolute()) {
            return false;
        }
        return !relative.startsWith("..");
    }

    /**
     * Denies when the path falls inside a protected read-only root, or outside
     * the project root. The protected-root check runs first because it is the
     * more specific statement about that location.
     */
    public static Decision evaluatePath(String value, String cwd, List<Path> roots, Path projectRoot) {
        Path target;
        try {
            target = resolveTarget(value, cwd);
        } catch (InvalidPathException ex) {
            // An unparseable path cannot match a protected root; report that
            // honestly instead of guessing or crashing.
            return new Decision("allow", "Path could not be parsed and cannot match any protected root");
        }

        for (Path root : roots) {
            if (isWithin(target, root)) {
                return new Decision("deny", "Path is inside a protected read-only root: " + value);
            }
        }

        if (projectRoot != null && !writesOutsideProjectAllowed() && !isWithin(target, projectRoot)) {
            return new Decision("deny",
                    "Path is outside the project root and everything outside the project is read-only: " + value);
        }

        return new Decision("allow", "Path is inside the project and outside every protected root");
    }

    /**
     * Denies when the command text explicitly references any protected root.
     * Does not apply the project-root boundary; see the class comment.
     */
    public static Decision evaluateCommand(String command, List<Path> roots) {
        for (Path root : roots) {
            if (commandReferencesRoot(command, root)) {
                return new Decision("deny", "Command explicitly references a protected read-only root");
            }
        }
        return new Decision("allow", "No protected absolute root was detected");
    }

    /**
     * True when the command text contains the root path as a whole path token,
     * not merely as a prefix of a longer name. On Windows the command text is
     * lower-cased and '/' is normalized to '\' before matching.
     */
    public static boolean commandReferencesRoot(String command, Path root) {
        String commandText = command;
        if (WINDOWS) {
            commandText = command.replace('/', '\\').toLowerCase(Locale.ROOT);
        }
        String rootText = canonical(root).toString();
        String pattern = "(^|[^\\w.~-])" + Pattern.quote(rootText)
                + "(?=" + Pattern.quote(File.separator) + "|$|" + COMMAND_BOUNDARY + ")";
        return Pattern.compile(pattern).matcher(commandText).find();
    }

    /**
     * Accepts exactly the flags --path, --command and --cwd, each with a
     * non-empty value, and requires exactly one of --path / --command.
     * Returns null for anything else.
     */
    public static Parsed parseArguments(String[] argv) {
        String path = null;
        String command = null;
        String cwd = System.getProperty("user.dir", ".");
        for (int index = 0; index < argv.length; index += 2) {
            if (index + 1 >= argv.length) {
                return null;
            }
            String key = argv[index];
            String value = argv[index + 1];
            if (value.isEmpty()) {
                return null;
            }
            switch (key) {
                case "--path" -> path = value;
                case "--command" -> command = value;
                case "--cwd" -> cwd = value;
                default -> {
                    return null;
                }
            }
        }
        if ((path != null) == (command != null)) {
            return null;
        }
        return new Parsed(path, command, cwd);
    }

    private static String escapeJson(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (character < 0x20) {
                        out.append(String.format("\\u%04x", (int) character));
                    } else {
                        out.append(character);
                    }
                }
            }
        }
        return out.toString();
    }
}
