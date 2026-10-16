package io.github.cherepavel.architecture;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Checks compiled dependency edges, including signatures and method bodies, using the JDK's jdeps. */
class OnionArchitectureTest {
    private static final String BASE = "io.github.cherepavel.";
    private static final String APPLICATION = BASE + "application.";

    @Test void dependenciesPointInwardsAndContractsAreFrameworkFree() {
        var output = new StringWriter();
        var errors = new StringWriter();
        var jdeps = ToolProvider.findFirst("jdeps").orElseThrow(() -> new AssertionError("Run tests with a full JDK"));
        int status = jdeps.run(new PrintWriter(output), new PrintWriter(errors),
                "--ignore-missing-deps", "-verbose:class", "-filter:none",
                Path.of("target", "classes").toAbsolutePath().toString());
        assertEquals(0, status, errors.toString());
        List<String> violations = new ArrayList<>();
        int checked = 0;
        for (String line : output.toString().lines().toList()) {
            String[] edge = line.trim().split("\\s+");
            if (edge.length < 3 || !edge[1].equals("->")) continue;
            String source = edge[0];
            String target = edge[2];
            if (source.startsWith(BASE + "infrastructure.") && target.startsWith(BASE)) {
                // The legacy HTTP adapter may use the preserved public wire contract, but never services or the facade.
                boolean allowed = target.startsWith(BASE + "infrastructure.")
                        || target.startsWith(APPLICATION + "port.")
                        || target.startsWith(APPLICATION + "dto.")
                        || target.startsWith(APPLICATION + "exception.")
                        || target.equals(BASE + "AnythingLlmApi")
                        || target.startsWith(BASE + "ChatRequest") || target.equals(BASE + "ChatResponse");
                if (!allowed) violations.add(source + " -> " + target);
            }
            if (!source.startsWith(APPLICATION)) continue;
            checked++;
            boolean allowed = target.startsWith("java.") || target.startsWith(APPLICATION);
            if (source.startsWith(APPLICATION + "dto.")) {
                allowed = target.startsWith("java.") || target.startsWith(APPLICATION + "dto.");
            } else if (source.startsWith(APPLICATION + "exception.")) {
                allowed = target.startsWith("java.") || target.startsWith(APPLICATION + "exception.");
            } else if (source.startsWith(APPLICATION + "api.") || source.startsWith(APPLICATION + "port.")) {
                allowed = target.startsWith("java.") || target.startsWith(APPLICATION + "dto.")
                        || target.startsWith(APPLICATION + "exception.");
            }
            if (!allowed) violations.add(source + " -> " + target);
        }
        assertTrue(checked > 0, "No application classes were checked");
        assertTrue(violations.isEmpty(), () -> "Forbidden dependency edges:\n" + String.join("\n", violations));
    }
}
