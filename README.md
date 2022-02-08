experimental-design-lib
-----------------------------------
# <p align=center>Experimental Design Library</p>

<p align="center"><i>Library for parsing of different experimental design formats and mapping to openbis properties </i></p>

<div align="center">



[![Build Maven Package](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/build_package.yml/badge.svg)](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/build_package.yml)
[![Run Maven Tests](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/run_tests.yml/badge.svg)](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/run_tests.yml)
[![CodeQL](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/qbicsoftware/experimental-design-lib/actions/workflows/codeql-analysis.yml)
[![release](https://img.shields.io/github/v/release/qbicsoftware/experimental-design-lib?include_prereleases)](https://github.com/qbicsoftware/experimental-design-lib/releases)

[![license](https://img.shields.io/github/license/qbicsoftware/experimental-design-lib)](https://github.com/qbicsoftware/experimental-design-lib/blob/main/LICENSE)
![language](https://img.shields.io/badge/language-java-blue.svg)

</div>

## How to compile

Compile the project and build an executable java archive:

```
mvn clean package
```

The JAR file will be created in the ``/target`` folder

## Add this library as a dependency

This library is not hosted on maven central. To use it, you have to include our artifact repository to your pom.

```xml
<repositories>
    <repository>
        <releases>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
            <checksumPolicy>fail</checksumPolicy>
        </releases>
        <snapshots>
            <enabled>false</enabled>
        </snapshots>
        <id>nexus-releases</id>
        <name>QBiC Releases</name>
        <url>https://qbic-repo.qbic.uni-tuebingen.de/repository/maven-releases</url>
    </repository>
</repositories>
```

Then include this library as an artifact.
```xml
<dependency>
    <groupId>life.qbic</groupId>
    <artifactId>experimental-design-lib</artifactId>
    <version>0.21.0-SNAPSHOT</version>
</dependency>
```

