# XOAI

[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=gdcc_xoai&metric=alert_status)](https://sonarcloud.io/dashboard?id=gdcc_xoai)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=gdcc_xoai&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=gdcc_xoai)
[![SonarCloud Bugs](https://sonarcloud.io/api/project_badges/measure?project=gdcc_xoai&metric=bugs)](https://sonarcloud.io/component_measures/metric/reliability_rating/list?id=gdcc_xoai)
[![SonarCloud Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=gdcc_xoai&metric=vulnerabilities)](https://sonarcloud.io/component_measures/metric/security_rating/list?id=gdcc_xoai)

What is XOAI?

XOAI is the most powerful and flexible OAI-PMH Java Toolkit (initially developed by [Lyncode](https://github.com/lyncode),
updated by [DSpace](https://github.com/DSpace)). XOAI contains common Java classes allowing to easily implement
[OAI-PMH](https://en.wikipedia.org/wiki/Open_Archives_Initiative_Protocol_for_Metadata_Harvesting) data and service providers.

## Usage

**Moving** (again): as XOAI is [no longer actively maintained by DSpace since 2019](https://github.com/DSpace/xoai/issues/72#issuecomment-557292929),
this fork by the [*Global Dataverse Community Consortium*](https://dataversecommunity.global) provides an updated
version for the needs of and usage with the open source repository [Dataverse Software](https://dataverse.org).

This library is available from Maven Central, simply rely on the main POM:

```
<dependency>
	<groupId>io.gdcc</groupId>
	<artifactId>xoai</artifactId>
	<version>5.0.0</version>
</dependency>
```

Some minimal usage documentation has been scraped from the DSpace Wiki, mostly
explaining the concepts of this library, and put into [docs/README.md](docs/README.md).
It also contains some minimal explanation of this forks special changes.
Feel free to extend the documentation, pull requests welcome.

## Release notes

### v5.0.0
This is a breaking changes release with a lot of new features, influenced by the usage of XOAI within Dataverse and other places.

#### 💔 BREAKING CHANGES
- Compatible with Java 11+ only
- Uses java.time API instead of java.util.Date
- Data Provider:
	- Changes required to your `ItemRepository`, `Item` and `ItemIdentifier` implementations
	- Changes required to your `SetRepository` implementation
	- Changes required to your usage of `DataProvider` (much simplified!)
- Service Provider: Changes required to your code using an `OAIClient`, as default implementation changed

#### 🌟 FEATURES
- Use the new `CopyElement` or `Metadata.copyFromStream()` to skip metadata XML processing, so pregenerated or cached
data can be served from your `ItemRepository` implementation
- Use native JDK HTTP client for OAI requests in service provider,
extended with client builder and option to create unsafe SSL connections for testing
- Add total number of results (inspired by GBIF #8)
- Larger rewrite of how data provider works:
	- Enable caching requests by exposing the resumption token to the application and making the pagination of
	results more explicit and comprehensible using a new type `ResultsPage`
	- Extended, simplified and more verbose parameter validation for requests
	- `until` timestamps are tweaked to enable more inclusive requests (avoid spilling milk with database timestamps etc)
	- Extensible reuse of `RawRequest` and `Request` classes to create non-servlet based endpoints with in-tree
	verification methods now possible via `RequestBuilder`!
	- Simplified filtering model for XOAI: easier to setup, default conditions provided

#### 🏹 BUG FIXES
- Sets now are properly compared, re-enabling `SetRepositoryHelper` to identify available sets
- Many new try-with-resources to mitigate memory leak risks
- The StAX XML components have been configured to avoid loading external entities, mitigating potential security risks
- `from` and `until` timestamps are now correctly verified in data provider #25
- And more...

## License

See [LICENSE](LICENSE) or [DSpace BSD License](https://raw.github.com/DSpace/DSpace/master/LICENSE)
