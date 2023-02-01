package io.gdcc.xoai.testapp;

import io.gdcc.xoai.dataprovider.DataProvider;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.model.InMemoryItem;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.repository.InMemoryItemRepository;
import io.gdcc.xoai.dataprovider.repository.InMemorySetRepository;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration.RepositoryConfigurationBuilder;
import io.gdcc.xoai.model.oaipmh.DeletedRecord;
import io.gdcc.xoai.model.oaipmh.OAIPMH;
import io.gdcc.xoai.xml.XmlWriter;
import java.io.OutputStream;
import java.time.Instant;
import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLStreamException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class OaiController {

    static final String adminEmail = "foo@bar.com";
    static final String repoName = "oai-pmh-tck";
    static final Instant earliestDate = Instant.now().minusSeconds(10);
    public static final int HOST_PORT = 7777;
    public static final String TC_HOSTNAME = "host.testcontainers.internal";
    public static final String BASE_URL = "http://" + TC_HOSTNAME + ":" + HOST_PORT + "/oai";

    private final RepositoryConfiguration config =
            new RepositoryConfigurationBuilder()
                    .withRepositoryName(repoName)
                    .withAdminEmail(adminEmail)
                    .withBaseUrl(BASE_URL)
                    .withEarliestDate(earliestDate)
                    .withDeleteMethod(DeletedRecord.NO)
                    .build();

    private final InMemoryItemRepository itemRepository = new InMemoryItemRepository();
    private final InMemorySetRepository setRepository = new InMemorySetRepository();
    private final Repository repository =
            new Repository(config)
                    .withSetRepository(setRepository)
                    .withItemRepository(itemRepository);

    private final Context context =
            new Context().withMetadataFormat("oai_dc", MetadataFormat.identity());
    private final DataProvider dataProvider = new DataProvider(context, repository);

    public OaiController() {
        final String set1 = "test1";
        final String set2 = "test2";

        // Create two sets in the sets repo
        this.setRepository.withSet("Test Set 1", set1);
        this.setRepository.withSet("Test Set 2", set2);

        // Create random items and make them part of the sets
        InMemoryItem[] items =
                new InMemoryItem[] {
                    InMemoryItem.randomItem().withSet(set1),
                    InMemoryItem.randomItem().withSet(set1),
                    InMemoryItem.randomItem().withSet(set1),
                    InMemoryItem.randomItem().withSet(set1),
                    InMemoryItem.randomItem().withSet(set2)
                };

        // Feed the repo with our items
        this.itemRepository.withItems(items);
    }

    @RequestMapping("/oai")
    public void oai(OutputStream responseStream, HttpServletRequest request) {

        OAIPMH handle = dataProvider.handle(request.getParameterMap());

        try (XmlWriter xmlWriter = new XmlWriter(responseStream, config); ) {
            xmlWriter.write(handle);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
}
