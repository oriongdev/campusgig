package com.campusgig.user;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Checks the layering rules of this service by reading the import statements in its source files.
 *
 * <p>The layers are the four packages presentation, service, domain and infrastructure.
 * The domain layer is the centre: it may not depend on any other layer. The web layer
 * (presentation) and the technical layer (infrastructure) may not depend on each other.
 */
class LayeredArchitectureTest {

    private static final String BASE_PACKAGE = "com.campusgig.user";

    @Test
    void theDomainLayerDependsOnNoOtherLayer() throws Exception {
        assertNoImports("domain", "presentation", "service", "infrastructure");
    }

    @Test
    void theServiceLayerDoesNotDependOnThePresentationLayer() throws Exception {
        assertNoImports("service", "presentation");
    }

    @Test
    void theInfrastructureLayerDoesNotDependOnThePresentationLayer() throws Exception {
        assertNoImports("infrastructure", "presentation");
    }

    @Test
    void thePresentationLayerDoesNotDependOnTheInfrastructureLayer() throws Exception {
        assertNoImports("presentation", "infrastructure");
    }

    private static void assertNoImports(String layer, String... forbiddenLayers) throws IOException, URISyntaxException {
        Path layerDirectory = sourceRoot().resolve(BASE_PACKAGE.replace('.', '/')).resolve(layer);
        List<Path> sourceFiles;
        try (Stream<Path> files = Files.walk(layerDirectory)) {
            sourceFiles = files.filter(file -> file.toString().endsWith(".java")).toList();
        }
        assertFalse(sourceFiles.isEmpty(), "No source files found in " + layerDirectory);

        List<String> violations = new ArrayList<>();
        for (Path file : sourceFiles) {
            for (String line : Files.readAllLines(file)) {
                for (String forbidden : forbiddenLayers) {
                    String target = BASE_PACKAGE + "." + forbidden + ".";
                    if (line.startsWith("import " + target) || line.startsWith("import static " + target)) {
                        violations.add(layerDirectory.relativize(file) + " has: " + line.trim());
                    }
                }
            }
        }
        assertEquals(List.of(), violations, "The " + layer + " layer must not depend on: " + String.join(", ", forbiddenLayers));
    }

    /** Finds src/main/java from where the compiled tests are (target/test-classes), whatever the working directory. */
    private static Path sourceRoot() throws URISyntaxException {
        Path testClasses = Path.of(LayeredArchitectureTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return testClasses.getParent().getParent().resolve("src").resolve("main").resolve("java");
    }
}
