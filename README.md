# Experimental Design Library

[![Build Maven Package](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/build_package.yml/badge.svg)](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/build_package.yml)
[![Run Maven Tests](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/run_tests.yml/badge.svg)](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/run_tests.yml)
[![CodeQL](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/codeql-analysis.yml)
[![release](https://img.shields.io/github/v/release/qbicsoftware/experimental-design-lib?include_prereleases)](https://github.com/qbicsoftware/experimental-design-lib/releases)

[![license](https://img.shields.io/github/license/qbicsoftware/experimental-design-lib)](https://github.com/qbicsoftware/experimental-design-lib/blob/main/LICENSE)
![language](https://img.shields.io/badge/language-java-blue.svg)

Parsers, writers for experimental design formats.

## How to Run

Create a runnable version of this code with maven and java 8:

```
mvn clean package
```

The JAR file will be created in the ``/target`` folder, for example:

```
|-target
|---experimental-design-lib-<version-number>.jar
|---...
```

## How to Use

With Maven you can include the recent library version as dependency with:

```XML
<dependency>
  <groupId>life.qbic</groupId>
  <artifactId>experimental-design-lib</artifactId>
  <version>version-number</version>
</dependency>
```

## License

This work is licensed under the [MIT license](https://mit-license.org/).
