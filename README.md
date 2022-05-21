# XOAI

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

## License

See [LICENSE](LICENSE) or [DSpace BSD License](https://raw.github.com/DSpace/DSpace/master/LICENSE)
