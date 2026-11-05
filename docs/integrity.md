# Dependency integrity and reproducible gates

Development 0.1.0 requires Gradle 8.10.2, JDK 21, Kotlin JVM 2.0.21 and JUnit Jupiter 5.11.4. The explicit JUnit Platform launcher 1.11.4 is the matching test runner component. There is no runtime dependency beyond Kotlin stdlib and the JDK.

## Policy

- `gradle.lockfile` and `gradle/verification-metadata.xml` are required source inputs, never ignored outputs.
- Locks must come from actual Gradle resolution. `LockMode.STRICT` applies to all project configurations.
- Verification is forced to STRICT in settings. Hashes must be actual upstream artifact SHA-256 values, including plugin artifacts, compiler artifacts, POM files, Gradle module metadata and BOMs that resolution uses.
- Do not create placeholder locks, example checksums, wildcard trust, trusted-artifact exclusions or an ignore-failure path.
- Missing integrity files fail before project dependency resolution in ordinary builds.
- Ordinary CI neither writes locks nor regenerates verification metadata. Integrity preparation is an explicit local maintenance operation and is rejected when `CI` is set.

Generating hashes is not proof of publisher identity: a first resolution can itself be compromised. Review repository origins, coordinates and independently available upstream checksums before accepting records. Preserve metadata verification in the generated XML (`verify-metadata` true); do not suppress POM/BOM verification to make resolution pass.

## First preparation or deliberate dependency maintenance

Use a clean local working directory with the required Gradle/JDK already installed. These commands access dependency repositories but do not publish or deploy anything:

```sh
mkdir -p build/gradle-home build/java-home build/tmp
export GRADLE_USER_HOME="$PWD/build/gradle-home"
export JAVA_TOOL_OPTIONS="-Duser.home=$PWD/build/java-home -Djava.io.tmpdir=$PWD/build/tmp -Dfile.encoding=UTF-8"
gradle -PprepareIntegrity=true --write-locks --write-verification-metadata sha256 resolveDependencies test demoAssert
```

The flag permits generation only when both write options are present; it does not turn verification off. Existing mismatches require investigation, not a lenient/off retry. Resolve the discrepancy against upstream before deliberately updating records.

Review and retain the actual root lockfile, generated verification XML and any additional lockfile Gradle emits. Check that the Kotlin plugin/compiler and JUnit transitive graph, including metadata, are represented. Never infer or type hash records from version numbers. Generated integrity files are source, unlike generated binaries or caches.

## Cold-cache verification

Use a new, unused Gradle home under `build/`; do not reuse the preparation cache as evidence of cold resolution:

```sh
mkdir -p build/cold-gradle-home
GRADLE_USER_HOME="$PWD/build/cold-gradle-home" gradle --dependency-verification strict resolveDependencies testClasses
GRADLE_USER_HOME="$PWD/build/cold-gradle-home" gradle --offline test
GRADLE_USER_HOME="$PWD/build/cold-gradle-home" gradle --offline --quiet demoAssert
```

The first command may download artifacts but may not alter integrity records. The latter commands are separate offline gates using that newly populated home. Choose another unused directory name for another cold run. No command here removes files.

## Resource placement

Run from the project root with the documented environment. Gradle user home, Java user home, temporary files, project cache, compiler state and task outputs then live under ignored `build/`.

`settings.gradle.kts` sets `gradle.startParameter.projectCacheDir = file("build/project-cache")` and configures `kotlin.project.persistent.dir` to `build/kotlin-state` (pre-creating `build/kotlin-state/sessions`) on a copied project-properties map. You can also pass `--project-cache-dir build/project-cache` explicitly on the command line:

```sh
gradle --project-cache-dir build/project-cache --offline test
```

Neither location contains required sources or integrity metadata. Never place required sources or metadata under a build/cache/vendor directory.

One worker, an in-process compiler and a 320 MiB Gradle heap reduce concurrent JVM demand. The Gradle metaspace cap is 320 MiB and code cache 64 MiB; test/demo heaps are 192 MiB. These are JVM configuration limits, not an exact RSS guarantee. There is no Gradle wrapper download or automatic toolchain download.

## Português

Locks e hashes são fontes obrigatórias, derivados de resolução real. A geração é uma operação local explícita de manutenção, nunca parte do CI normal. Revise também POMs, BOMs e metadados dos plugins; não use curingas de confiança nem desative verificação. Após a revisão, use um novo cache em `build/` para resolver novamente com verificação estrita e execute testes e demonstração offline em comandos separados. A geração inicial de hashes não comprova sozinha a identidade do fornecedor.
