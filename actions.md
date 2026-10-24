# GitHub Actions for This Project (Free Edition)

This project is a Java-based Spring Boot gRPC sample with two Maven projects:

- BookService
- LibraryService

GitHub Actions can be used effectively on the free plan for this repository, especially if the repository is public. The best approach is to keep workflows simple, fast, and focused on build verification.

## 1. Why GitHub Actions is useful for this project

This project benefits from GitHub Actions because it can automatically validate that both services still build and test successfully after every code change.

Typical use cases:

- run Maven builds for BookService and LibraryService
- compile the gRPC proto files
- run unit tests
- fail the build if a service breaks
- create release artifacts when a tag is pushed
- validate pull requests before merge

## 2. Free-tier considerations

On GitHub Free, the best practices are:

- keep workflows short and efficient
- use caching for Maven dependencies
- avoid running expensive integration tests on every push
- use public repos where possible to avoid extra cost concerns
- use a single workflow that builds both modules with a matrix

GitHub Free also has limits on Actions minutes. To stay within limits:

- keep jobs under a few minutes
- avoid long-running tests or repeated builds
- use `mvn -q -DskipTests` only for packaging or release steps when needed
- run full validation only on pull requests or nightly builds

## 3. Recommended workflow strategy

A good setup for this project is:

- CI on push and pull request
- build both BookService and LibraryService
- cache Maven dependencies
- run `mvn test`
- compile gRPC code automatically through the protobuf plugin
- optionally build a jar artifact for the release branch

## 4. Example workflow: build both services

Create a file named `.github/workflows/ci.yml` with the following content:

```yaml
name: CI Build

on:
  push:
    branches:
      - main
      - master
  pull_request:
    branches:
      - main
      - master

jobs:
  build:
    runs-on: ubuntu-latest

    strategy:
      matrix:
        service:
          - BookService
          - LibraryService

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven

      - name: Build and test ${{ matrix.service }}
        working-directory: ${{ matrix.service }}
        run: mvn test
```

### What this does

- checks out the repository
- installs Java 17
- caches Maven dependencies to speed up builds
- runs the Maven test lifecycle in each service folder
- verifies the proto generation and gRPC compile step for both services

## 5. Why this is a good fit for this repo

This repo has two independent Java Maven projects. A matrix build is a natural fit because each service can be built separately but still run in the same workflow.

Benefits:

- you get early feedback on failed builds
- you can detect if BookService gRPC code breaks the contract
- you can ensure LibraryService keeps compiling against the generated stubs
- the workflow is simple and suitable for free tier limits

## 6. Optional: add a nightly build

If you want to be more efficient, run a full build only at night instead of every push. This is useful when integration tests are heavier.

```yaml
name: Nightly Validation

on:
  schedule:
    - cron: '0 2 * * *'

jobs:
  nightly-build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven

      - name: Build BookService
        working-directory: BookService
        run: mvn test

      - name: Build LibraryService
        working-directory: LibraryService
        run: mvn test
```

This reduces load on the free plan while still keeping validation cover.

## 7. Optional: build JAR artifacts

If you want to package the applications into artifacts, add a packaging step.

```yaml
name: Package Artifacts

on:
  push:
    tags:
      - 'v*'

jobs:
  package:
    runs-on: ubuntu-latest

    strategy:
      matrix:
        service:
          - BookService
          - LibraryService

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven

      - name: Package ${{ matrix.service }}
        working-directory: ${{ matrix.service }}
        run: mvn clean package -DskipTests

      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: ${{ matrix.service }}-artifact
          path: ${{ matrix.service }}/target/*.jar
```

This is useful when you want to share binaries or release builds from GitHub.

## 8. Optional: add code quality checks

GitHub Actions can also run static checks if you later add them. Examples:

- SpotBugs
- Checkstyle
- PMD
- SonarCloud (if you use a free alternative or public repo analysis)

For a small project, simple `mvn test` plus compile validation is usually enough.

## 9. Recommended minimum setup for this repo

If you want the smallest and most practical setup for free usage, use this:

- one workflow file
- Java 17
- Maven cache
- matrix build for both modules
- run it on every push and pull request

This gives strong value without using a large amount of Actions minutes.

## 10. Example: combined workflow summary

Recommended flow:

1. developer pushes code
2. GitHub Actions triggers workflow
3. JDK 17 is installed
4. Maven dependencies are cached
5. BookService Maven tests run
6. LibraryService Maven tests run
7. build fails if either service is broken
8. PR can be blocked until green

## 11. Final suggestion

For this project, the most suitable GitHub Actions setup under the free tier is:

- public repository if possible
- Java 17
- Maven dependency cache
- single matrix build for both BookService and LibraryService
- CI on push and pull request
- optional nightly full validation
- optional packaged release artifacts for tags

This keeps the implementation easy, reliable, and cost-conscious.

## 12. Sample file to create

The workflow file should be placed here:

```text
.github/workflows/ci.yml
```

This is the most practical first step for the project.
