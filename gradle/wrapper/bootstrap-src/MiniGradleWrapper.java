import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class MiniGradleWrapper {
    public static void main(String[] args) throws Exception {
        Path project = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path propsPath = project.resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(propsPath)) { props.load(in); }
        String distributionUrl = props.getProperty("distributionUrl").replace("\\:", ":");
        URI uri = URI.create(distributionUrl);
        String zipName = Paths.get(uri.getPath()).getFileName().toString();
        String base = zipName.endsWith(".zip") ? zipName.substring(0, zipName.length() - 4) : zipName;
        Path cache = Paths.get(System.getProperty("user.home"), ".gradle", "wrapper", "dists", "mini-bootstrap", base);
        Path zip = cache.resolve(zipName);
        Files.createDirectories(cache);

        Path gradleHome = findGradleHome(cache);
        if (gradleHome == null) {
            if (!Files.exists(zip)) {
                System.out.println("Downloading " + distributionUrl);
                download(uri.toURL(), zip);
            }
            unzip(zip, cache);
            gradleHome = findGradleHome(cache);
        }
        if (gradleHome == null) throw new IllegalStateException("Could not locate Gradle after extracting " + zip);

        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        Path executable = gradleHome.resolve("bin").resolve(windows ? "gradle.bat" : "gradle");
        if (!windows) executable.toFile().setExecutable(true);

        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(command).directory(project.toFile()).inheritIO();
        int exit = pb.start().waitFor();
        System.exit(exit);
    }

    private static void download(URL url, Path target) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + ".part");
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(60_000);
        try (InputStream in = new BufferedInputStream(connection.getInputStream());
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(temp))) {
            in.transferTo(out);
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static void unzip(Path zip, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path out = targetDir.resolve(entry.getName()).normalize();
                if (!out.startsWith(targetDir)) throw new IOException("Unsafe zip entry: " + entry.getName());
                if (entry.isDirectory()) Files.createDirectories(out);
                else {
                    Files.createDirectories(out.getParent());
                    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(out))) {
                        zis.transferTo(os);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private static Path findGradleHome(Path cache) throws IOException {
        if (!Files.exists(cache)) return null;
        try (var stream = Files.list(cache)) {
            return stream.filter(Files::isDirectory)
                    .filter(p -> Files.isDirectory(p.resolve("bin")) &&
                            (Files.exists(p.resolve("bin/gradle")) || Files.exists(p.resolve("bin/gradle.bat"))))
                    .findFirst().orElse(null);
        }
    }
}
