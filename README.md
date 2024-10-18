# XOAI

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=gdcc_xoai&metric=alert_status)](https://sonarcloud.io/summary/overall?id=gdcc_xoai)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=gdcc_xoai&metric=coverage)](https://sonarcloud.io/summary/overall?id=gdcc_xoai)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=gdcc_xoai&metric=bugs)](https://sonarcloud.io/summary/overall?id=gdcc_xoai)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=gdcc_xoai&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=gdcc_xoai)
[![TCK](https://github.com/gdcc/xoai/actions/workflows/tck.yml/badge.svg?branch=branch-5.0)](https://github.com/gdcc/xoai/actions/workflows/tck.yml)

What is XOAI?

XOAI is the most powerful and flexible OAI-PMH Java Toolkit (initially developed by [Lyncode](https://github.com/lyncode),
updated by [DSpace](https://github.com/DSpace)). XOAI contains common Java classes allowing to easily implement
[OAI-PMH](https://en.wikipedia.org/wiki/Open_Archives_Initiative_Protocol_for_Metadata_Harvesting) data and service providers.

Compliance with the OAI-PMH standard is checked using an included Technology Compatibility Kit, relying on
https://github.com/zimeon/oaipmh-validator. Compliance checks are operated on every pull request and push to main branch.

## Usage

**Moving** (again): as XOAI is [no longer actively maintained by DSpace since 2019](https://github.com/DSpace/xoai/issues/72#issuecomment-557292929),
this fork by the [*Global Dataverse Community Consortium*](https://dataversecommunity.global) provides an updated
version for the needs of and usage with the open source repository [Dataverse Software](https://dataverse.org).

This library is available from Maven Central, simply rely on them in your builds:

When building a data provider, you'll add [xoai-data-provider](https://mvnrepository.com/artifact/io.gdcc/xoai-data-provider/latest):
```
<dependency>
	<groupId>io.gdcc</groupId>
	<artifactId>xoai-data-provider</artifactId>
	<version>${xoai.version}</version>
</dependency>
```

When building a service provider, you'll add [xoai-service-provider](https://mvnrepository.com/artifact/io.gdcc/xoai-service-provider/latest):
```
<dependency>
	<groupId>io.gdcc</groupId>
	<artifactId>xoai-service-provider</artifactId>
	<version>${xoai.version}</version>
</dependency>
```


Some minimal usage documentation has been scraped from the DSpace Wiki, mostly
explaining the concepts of this library, and put into [docs/README.md](docs/README.md).
It also contains some minimal explanation of this forks special changes.
Feel free to extend the documentation, pull requests welcome.

## Development

This project uses [Spotless Maven Plugin](https://github.com/diffplug/spotless),
[google-format-java](https://github.com/google/google-java-format) and
[pre-commit](https://pre-commit.com/) to ensure a standardized and well-formatted codebase.

After cloning the repo, please make sure to install `pre-commit`, which will take care of everything for you:
```shell
pip install pre-commit
pre-commit install
```

If you want to run spotless directly, use Maven:
```shell
mvn spotless:apply
# - or to just check for consistency use -
mvn spotless:check
```

## Release notes

### v5.2.2

#### 🌟 FEATURES
- (none)

#### 💔 BREAKING CHANGES
- (none)

#### 🏹 BUG FIXES
- Catch invalid Base64 encodings for resumption tokens (#272) - a community contribution by @bumann-sbb 💫

### v5.2.1

#### 🌟 FEATURES
- (none)

#### 💔 BREAKING CHANGES
- (none)

#### 🏹 BUG FIXES
- Do not add empty namespace to XML elements (#240) - a community contribution by @bumann-sbb 💫
- Code coverage button links to a 404 (#214)

### v5.2.0

#### 🌟 FEATURES
- (none)

#### 💔 BREAKING CHANGES
- TCK now uses Spring 6 and Spring Boot 3

#### 🏹 BUG FIXES
- Do not break UTF-8 multibyte characters in data provider when using `CopyElement` to copy and paste metadata (#188)

### v5.1.0

#### 🌟 FEATURES
- Switching to Java 17 for compilation and testing, but keeping compatibility with Java 11 for JARs
- Switching to Jakarta EE 10 dependencies (For most scenarios, this is not a breaking change.)
- More updated dependencies, Maven plugins, etc

#### 💔 BREAKING CHANGES
- (none)

#### 🏹 BUG FIXES
- (none)

### v5.0.0
This is a breaking changes release with a lot of new features, influenced by the usage of XOAI within Dataverse and other places.

#### 💔 BREAKING CHANGES
- Compatible with Java 11+ only
- Uses java.time API instead of java.util.Date
- Data Provider:
	- Changes required to your `ItemRepository`, `Item` and `ItemIdentifier` implementations
	- Changes required to your `SetRepository` implementation
	- Changes required to your usage of `DataProvider` (much simplified!)
	- Renewed configuration mechanism for data provider requires adaption
- Service Provider: Changes required to your code using an `OAIClient`, as default implementation changed

#### 🌟 FEATURES
- Use the new `CopyElement` or `Metadata.copyFromStream()` to skip metadata XML processing, so pregenerated or cached
data can be served from your `ItemRepository` implementation
- Use native JDK HTTP client for OAI requests in service provider,
extended with client builder and option to create unsafe SSL connections for testing
- New JDK HTTP client allows to send custom headers, useful for authentication etc
- Add total number of results (inspired by GBIF #8)
- Larger rewrite of how data provider works:
	- Enable caching requests by exposing the resumption token to the application and making the pagination of
	results more explicit and comprehensible using a new type `ResultsPage`
	- Extended, simplified and more verbose parameter validation for requests
	- `until` timestamps are tweaked to enable more inclusive requests (avoid spilling milk with database timestamps etc)
	- Extensible reuse of `RawRequest` and `Request` classes to create non-servlet based endpoints with in-tree
	verification methods now possible via `RequestBuilder`!
	- Simplified filtering model for XOAI: easier to setup, default conditions provided
- Special XML handling for Dataverse JSON metadata to provide backward compatibility

#### 🏹 BUG FIXES
- Sets now are properly compared, re-enabling `SetRepositoryHelper` to identify available sets
- Many new try-with-resources to mitigate memory leak risks
- The StAX XML components have been configured to avoid loading external entities, mitigating potential security risks
- `from` and `until` timestamps are now correctly verified in data provider, see [#25](https://github.com/gdcc/xoai/issues/25)
	- Granularity "Lenient" introduced, as the OAI-PMH 2.0 spec allows request with both precisions when "Second"
	granularity is supported. Former implementation did not allow this - remember to configure this, default is stick
	with old behaviour!
	- Configurable behaviour how to deal with requests where `from` is not after `earliestDate`. Default is to allow
	such requests, as spec is non-prohibitive. Former implementation behaviour was to deny such requests!
- And more...

## License

See [LICENSE](LICENSE) or [DSpace BSD License](https://raw.github.com/DSpace/DSpace/master/LICENSE)
