package ro.uaic.info.gitprov.services;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
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

    public static final String PROVBOOK_NS = "http://www.provbook.org/";
    public static final String PROVBOOK_PREFIX = "provbook";
    public static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
    public static final String FOAF_PREFIX = "foaf";
    private final ProvFactory pFactory = InteropFramework.newXMLProvFactory();
    private final Namespace ns;
    @Autowired
    public CommitService commitService;
    InteropFramework intF = new InteropFramework();

    public ProvenanceService() {
        ns = new Namespace();
        ns.addKnownNamespaces();
        ns.register(PROVBOOK_PREFIX, PROVBOOK_NS);
        ns.register(FOAF_PREFIX, FOAF_NS);
    }

    public QualifiedName getQualifiedName(String name, String prefix) {
        return ns.qualifiedName(prefix, name, pFactory);
    }

    public String repositoryToDocument(Repository repository) throws IOException {
        List<RepositoryCommit> repositoryCommits = commitService.getCommits(repository);
        List<Agent> agents = new ArrayList<>();
        String authorName, authorEmail;

        for (RepositoryCommit repositoryCommit : repositoryCommits) {
            authorName = repositoryCommit.getCommit().getAuthor().getName();
            authorEmail = repositoryCommit.getCommit().getAuthor().getEmail();

            List<Attribute> attributes = new ArrayList<>();
            attributes.add(pFactory.newAttribute(FOAF_NS, "email", FOAF_PREFIX, authorEmail, getQualifiedName("string", "xsd")));

            Agent agent = pFactory.newAgent(getQualifiedName(authorName, PROVBOOK_PREFIX), attributes);
            agents.add(agent);
        }

        Document document = pFactory.newDocument();
        document.setNamespace(ns);

        OutputStream os = new ByteArrayOutputStream();
        document.getStatementOrBundle().addAll(agents);
        intF.writeDocument(os, InteropFramework.ProvFormat.RDFXML, document);

        System.out.println(os.toString());

        return os.toString();
    }
}
