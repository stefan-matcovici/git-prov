package ro.uaic.info.gitprov.services;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.openprovenance.prov.interop.InteropFramework;
import org.openprovenance.prov.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProvenanceService {

    /**
     * The constant logger.
     */
    final static Logger logger = Logger.getLogger(ProvenanceService.class);

    @Autowired
    public ContentsService contentsService;

    @Autowired
    public DataService dataService;

    public static final String PROVENANCE_PREFIX = "gitprov";
    private final ProvFactory provFactory = InteropFramework.newXMLProvFactory();
    private final Namespace namespace;

    public static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
    public static final String FOAF_PREFIX = "foaf";
    @Autowired
    public CommitService commitService;
    @Autowired
    public RepositoryService repositoryService;
    InteropFramework interopFramework = new InteropFramework();
    QualifiedNameUtils qualifiedNameUtils = new QualifiedNameUtils();

    public ProvenanceService() {
        namespace = new Namespace();
        namespace.addKnownNamespaces();
        namespace.register(FOAF_PREFIX, FOAF_NS);
    }

    public QualifiedName getQualifiedName(String name, String prefix) {

        return namespace.qualifiedName(prefix, qualifiedNameUtils.escapeToXsdLocalName(name.replace(' ', '-')), provFactory);
    }

    public String repositoryToDocument(Repository repository, String provenanceNs) throws IOException {
        namespace.register(PROVENANCE_PREFIX, provenanceNs);

        List<RepositoryCommit> repositoryCommits = commitService.getCommits(repository);
        List<Agent> agents = new ArrayList<>();
        String authorName, authorEmail, authorLogin;

        for (RepositoryCommit repositoryCommit : repositoryCommits) {
            authorLogin = repositoryCommit.getCommitter().getLogin();
            authorEmail = repositoryCommit.getCommit().getAuthor().getEmail();
            authorName = repositoryCommit.getCommit().getAuthor().getName();

            List<Attribute> attributes = new ArrayList<>();
            attributes.add(provFactory.newAttribute(FOAF_NS, "name", FOAF_PREFIX, authorName, provFactory.getName().XSD_STRING));
            attributes.add(provFactory.newAttribute(FOAF_NS, "email", FOAF_PREFIX, authorEmail, provFactory.getName().XSD_STRING));

            Agent agent = provFactory.newAgent(getQualifiedName(authorLogin, PROVENANCE_PREFIX), attributes);
            agents.add(agent);
        }


        Document document = provFactory.newDocument();
        document.setNamespace(namespace);

        OutputStream os = new ByteArrayOutputStream();
        document.getStatementOrBundle().addAll(agents);
        interopFramework.writeDocument(os, InteropFramework.ProvFormat.RDFXML, document);

        System.out.println(os.toString());

        return os.toString();
    }
}
