# XOAI Data Provider documentation

**Note: this documentation is scrapped from the DuraSpace wiki, currently at
https://wiki.lyrasis.org/display/DSDOC7x.** It has been adopted to serve as a
minimal usage documentation for this library, to explain the concepts and ideas
behind some classes in `xoai-data-provider`.

## Introduction

Open Archives Initiative Protocol for Metadata Harvesting is a low-barrier mechanism
for repository interoperability. Data Providers are repositories that expose
structured metadata via OAI-PMH. Service Providers then make OAI-PMH service
requests to harvest that metadata. OAI-PMH is a set of six verbs or services that
are invoked within HTTP.

**What is OAI 2.0?**

OAI 2.0 is a Java implementation of an OAI-PMH data provider interface (originally
developed by Lyncode and DSpace) that uses XOAI (this library), an Open Source
OAI-PMH Java Library, licensed under a BSD 3-clause.

**Why OAI 2.0?**

Projects like OpenAIRE have specific metadata requirements (to the published content
through the OAI-PMH interface). As the OAI-PMH protocol doesn't establish any frame
to these specifics, OAI 2.0 can, in a simple way, have more than one instance of an
OAI interface (feature provided by the XOAI core library) so one could define an
interface for each project. That is the main purpose, although, OAI 2.0 allows much
more than that.

### Concepts (XOAI Core Library)

To understand how XOAI works, one must understand the concept of Filter, Transformer
and Context.

- With a Filter it is possible to select information from the data source.
- A Transformer allows one to make some changes in the metadata before showing it in
  the OAI interface.
- XOAI also adds a new concept to the OAI-PMH basic specification,
  the concept of context. A context is identified in the URL:
  `http://www.example.com/oai/<context>`

Contexts could be seen as virtual distinct OAI interfaces, so with this one could
have things like:

    http://www.example.com/oai/request
    http://www.example.com/oai/driver
    http://www.example.com/oai/openaire

With this ingredients it is possible to build a robust solution that fulfills all
requirements of Driver, OpenAIRE and also other project-specific requirements.
As shown in Figure 1, with contexts one could select a subset of all available
items in the data source. So when entering the OpenAIRE context, all OAI-PMH request
will be restricted to that subset of items.

At this stage, contexts could be seen as sets (also defined in the basic OAI-PMH
protocol). The magic of XOAI happens when one need specific metadata format to be
shown in each context. Metadata requirements by Driver slightly differs from the
OpenAIRE ones. So for each context one must define its specific transformer.
So, contexts could be seen as an extension to the concept of sets.

To implement an OAI interface from the XOAI core library, one just need to implement
the datasource interfaces `ItemRepository` and `SetRepository`.

It is important to note, though, that you *don't have to* incorporate the concept of
contexts. You can still choose to use provide only one endpoint and simply
ignore the context (up to the point of providing an empty implementation, that is).

### Sets

OAI-PMH allows repositories to expose an hierarchy of sets in which records may be placed.
A record can be in zero or more sets.

The XOAI library supports exposing any kind of collections as sets from your application.
Each OAI set is discoverable by harvesters via the ListSets verb. You will need to specify
within your application how sets are named.

### Unique Identifier

Every item in a OAI-PMH data repository must have an unique identifier, which must conform
to the URI syntax. This might be handles, DOIs or other persistent identifiers. The application
incorporating this library needs to provide the identifiers, usually using the same ids
within their UI/API and the OAI interface.

### Access control

XOAI provides no authentication/authorisation methods, hooks or interfaces, although these
could be implemented using standard HTTP methods. It is assumed that all access will be
anonymous for the time being.

What metadata is public, relies entirely on the metadata returned to XOAI from the implementation
of your `ItemRepository`.

### Modification Date (OAI Date Stamp)

OAI-PMH harvesters need to know when a record has been created, changed or deleted.
Remember to carefully update these within your application and, when caching or pregenerating
metadata to serve to harvesters, update these timestamps.

### "About" Information

As part of each record given out to a harvester, there is an optional, repeatable "about"
section which can be filled out in any (XML-schema conformant) way. Common uses are for
provenance and rights information, and there are schemas in use by OAI communities for this.
The XOAI library allows its definition and it's up to your application to provide this
via the `ItemRepository` implementation(s)

### Deletions

The XOAI library supports all variants of deletion methods mentioned in the OAI-PMH
specification. http://www.openarchives.org/OAI/openarchivesprotocol.html#DeletedRecords

It is up to you, the application, to decide which guarantees you give and set
the configuration accordingly.

### Flow Control (Resumption Tokens)

An OAI data provider can prevent any performance impact caused by harvesting by forcing
a harvester to receive data in time-separated chunks. If the data provider receives a
request for a lot of data, it can send part of the data with a resumption token.
The harvester can then return later with the resumption token and continue.

XOAI supports resumption tokens (as requested by the spec) for "ListRecords",
"ListIdentifiers" and "ListSets" OAI-PMH requests.

Each OAI-PMH token-enabled request will return a configurable amount of results,
see `RepositoryConfiguration` for tuning. Offsets and max. results per request are
also sent to the underlying `ItemRepository` implementations to enable tuning your
caches or loading data partially on demand.

When a resumption token is issued, the optional completeListSize and cursor attributes
can be included. OAI 2.0 resumption tokens are persistent, so expirationDate of the
resumption token is normally undefined, but may be included.

Resumption tokens contain all the state information required to continue a request and
it is encoded in Base64. (See `ResumptionToken.Value`. You may provide your own
format, if Base64 does not suit your needs.)

## GDCCs Fork Specific Changes

This fork contains a change to how metadata can be served to the OAI-PMH response.
Instead of always doing an XSLT transformation, we a) indicate the metadata
format when asking the item repository for items and b) provide the ability
to add a `CopyElement` to the `Metadata` model.

It happily accepts an `InputStream`, being read and written as a raw response to the
`<metadata>` section of the result. This allows us to pre-generate metadata
and serve from the main application (Dataverse) without jumping through the
loops of the XML transformer for each item, which is MUCH faster.

Using `DataProvider.getOaiXslt()` an implementing application can retrieve a local copy
of https://github.com/eprints/eprints/blob/3.3/lib/static/oai2.xsl to serve it
from a local (servlet) endpoint. It allows for a much nicer UI experience when
accessing the OAI-PMH endpoint with a browser. Applications may provide a hyperref
to the endpoint (or other static location) by using `xmlWriter.writeStylesheet()`
in their OAI servlet.

## Advanced Configuration

**NOTE: THE FOLLOWING HAS BEEN COPIED FROM THE DSPACE WIKI AND IS OF LIMITED USE
FOR DATAVERSE - IT'S HERE FOR FUTURE REFERENCES**

OAI 2.0 allows you to configure following advanced options:

    Contexts
    Transformers
    Metadata Formats
    Filters
    Sets

It's an XML file commonly located at: [dspace]/config/crosswalks/oai/xoai.xml
General options

These options influence the OAI interface globally. "per page" means per request, next page (if there is one) can be requested using resumptionToken provided in current page.

    identation [boolean] - whether the output XML should be indented to make it human-readable

    maxListIdentifiersSize [integer] - how many identifiers to show per page (verb=ListIdentifiers)
    maxListRecordsSize [integer] - how many records to show per page (verb=ListRecords)

    maxListSetsSize [integer] - how many sets to show per page (verb=ListSets)

    stylesheet [relative file path] - an xsl stylesheet used by client's web browser to transform the output XML into human-readable HTML

Their location and default values are shown in the following fragment:
<Configuration xmlns="http://www.lyncode.com/XOAIConfiguration"
identation="false"
maxListIdentifiersSize="100"
maxListRecordsSize="100"
maxListSetsSize="100"
stylesheet="static/style.xsl">
Add/Remove Metadata Formats

Each context could have its own metadata formats. So to add/remove metadata formats to/from it, just need add/remove its reference within xoai.xml, for example, imagine one need to remove the XOAI schema from:
<Context baseurl="request">
<Format refid="oaidc" />
<Format refid="mets" />
<Format refid="xoai" />
<Format refid="didl" />
<Format refid="dim" />
<Format refid="ore" />
<Format refid="rdf" />
<Format refid="etdms" />
<Format refid="mods" />
<Format refid="qdc" />
<Format refid="marc" />
<Format refid="uketd_dc" />
</Context>

Then one would have:
<Context baseurl="request">
<Format refid="oaidc" />
<Format refid="mets" />
<Format refid="didl" />
<Format refid="dim" />
<Format refid="ore" />
<Format refid="rdf" />
<Format refid="etdms" />
<Format refid="mods" />
<Format refid="qdc" />
<Format refid="marc" />
<Format refid="uketd_dc" />
</Context>

It is also possible to create new metadata format by creating a specific XSLT for it. All already defined XSLT for DSpace can be found in the [dspace]/config/crosswalks/oai/metadataFormats directory. So after producing a new one, add the following information (location marked using brackets) inside the <Formats> element in [dspace]/config/crosswalks/oai/xoai.xml:
<Format id="[IDENTIFIER]">
<Prefix>[PREFIX]</Prefix>
<XSLT>metadataFormats/[XSLT]</XSLT>
<Namespace>[NAMESPACE]</Namespace>
<SchemaLocation>[SCHEMA_LOCATION]</SchemaLocation>
</Format>

where:

IDENTIFIER


The identifier used within context configurations to reference this specific format, must be unique within all Metadata Formats available.

PREFIX


The prefix used in OAI interface (metadataPrefix=PREFIX).

XSLT


The name of the XSLT file within [dspace]/config/crosswalks/oai/metadataFormats directory

NAMESPACE


XML Default Namespace of the created Schema

SCHEMA_LOCATION


URI Location of the XSD of the created Schema

NOTE: Changes in [dspace]/config/crosswalks/oai/xoai.xml requires reloading/restarting the servlet container.
Add/Remove Metadata Fields

The internal DSpace fields (Dublin Core) are exposed in the internal XOAI format (xml). All other metadata formats exposed via OAI are mapped from this XOAI format using XSLT (xoai.xsl itself is just an identity transformation). These XSLT stylesheets are found in the [dspace]/config/crosswalks/oai/metadataFormats directory. So e.g. oai_dc.xsl is a transformation from the XOAI format to the oai_dc format (unqualified Dublin Core).

Therefore exposing any DSpace metadata field in any OAI format is just a matter of modifying the corresponding output format stylesheet (This assumes the general knowledge of how XSLT works. For a tutorial, see e.g. http://www.w3schools.com/xsl/).

For example, if you have a DC field "local.note.librarian" that you want to expose in oai_dc as <dc:note> (please note that this is not a valid DC field and thus breaks compatibility), then edit oai_dc.xsl and add the following lines just above the closing tag </oai_dc:dc>:
<xsl:for-each select="doc:metadata/doc:element[@name='local']/doc:element[@name='note']/doc:element/doc:element/doc:field[@name='librarian']">
<dc:note><xsl:value-of select="." /></dc:note>
</xsl:for-each>

If you need to add/remove metadata fields, you're changing the output format. Therefore it is recommended to create a new metadata format as a copy of the one you want to modify. This way the old format will remain available along with the new one and any upgrades to the original format during DSpace upgrades will not overwrite your customizations. If you need the format to have the same name as the original format (e.g. the default oai_dc format), you can create a new context in xoai.xsl containing your modified format with the original name, which will be available as /oai/context-name.

NOTE: Please, keep in mind that the OAI provider caches the transformed output, so you have to run [dspace]/bin/dspace oai clean-cache after any .xsl modification and reload the OAI page for the changes to take effect. When adding/removing metadata formats, making changes in [dspace]/config/crosswalks/oai/xoai.xml requires reloading/restarting the servlet container.
Driver/OpenAIRE compliance

The default OAI 2.0 installation provides two new contexts. They are:

    Driver context, which only exposes Driver compliant items;
    OpenAIRE context, which only exposes OpenAIRE compliant items;

However, in order to be exposed DSpace items must be compliant with Driver/OpenAIRE guide-lines.

#### Driver Compliance

DRIVER Guidelines for Repository Managers and Administrators on how to expose digital scientific resources using OAI-PMH and Dublin Core Metadata, creating interoperability by homogenizing the repository output. The OAI-PMH driver set is based on DRIVER Guidelines 2.0.

This set is used to expose items of the repository that are available for open access. It’s not necessary for all the items of the repository to be available for open access.

What specific metadata values are expected?

To have items in this set, you must configure your input-forms.xml file in order to comply with the DRIVER Guidelines:

    Must have a publication date - dc.date.issued (already configured in DSpace items)
    dc.language must use ISO639-3
    the value of dc.type must be one of the 16 types named in the guidelines

How do you easily add those metadata values?

As DRIVER guidelines use Dublin Core, all the needed items are already registered in DSpace. You just need to configure the deposit process.

#### OpenAIRE compliance

For OpenAIRE v4 compliance, see OpenAIRE4 Guidelines Compliancy

The OpenAIRE Guidelines 2.0 provide the OpenAIRE compatibility to repositories and aggregators. By implementing these Guidelines, repository managers are facilitating the authors who deposit their publications in the repository in complying with the EC Open Access requirements. For developers of repository platforms, the Guidelines provide guidance to add supportive functionalities for authors of EC-funded research in future versions.

The name of the set in OAI-PMH is "ec_fundedresources" and will expose the items of the repository that comply with these guidelines. These guidelines are based on top of DRIVER guidelines. See version 2.0 of the Guidelines.

See the  Application Profile of OpenAIRE.

What specific metadata values are expected?

These are the OpenAIRE metadata values only, to check these and driver metadata values check page 11 of the OpenAIRE guidelines 2.0.

    dc:relation with the project ID (see p.8)
    dc:rights with the access rights information from vocabulary (possible values here)

Optionally:

    dc:date with the embargo end date (recommended for embargoed items)

1

<dc:date>info:eu-repo/date/embargoEnd/2011-05-12<dc:date>

How do you easily add those metadata values?

    Have a dc:relation field in input-forms.xml with a list of the projects. You can also use the OpenAIRE Authority Control Addon to facilitate the process of finding the project.
    Just use a combo-box for dc:rights to input the 4 options:
        info:eu-repo/semantics/closedAccess
        info:eu-repo/semantics/embargoedAccess
        info:eu-repo/semantics/restrictedAccess
        info:eu-repo/semantics/openAccess
    Use an input-box for dc:date to insert the embargo end date

## Relevant Links

### Sanity check your OAI interface with the *OAI Validator*

There is a very useful validator for OAI interfaces available at
http://validator.oaipmh.com, we urge you to use this validator to confirm your OAI
interface is in fact usable.
