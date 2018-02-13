package ro.uaic.info.gitprov.services;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryContents;
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
import java.util.stream.Collectors;

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
        List<Activity> activities = new ArrayList<>();
        List<Entity> entities = new ArrayList<>();
        List<WasAssociatedWith> wasAssociatedWiths = new ArrayList<>();

        // TODO create agents from contributors list, it's faster!
        // List<Contributor> contributors = repositoryService.getContributors(repository, false);

        // TODO refactor at least or find a way of implementing with streams
        processAllEntities(repository, entities);

        for (RepositoryCommit repositoryCommit : repositoryCommits) {
            Agent agent = processAgent(repositoryCommit, agents);
            Activity activity = processActivity(repositoryCommit);
            processWasAssociatedWith(repositoryCommit, agent, activity, wasAssociatedWiths);
            activities.add(activity);
        }

        Document document = provFactory.newDocument();
        document.setNamespace(namespace);

        OutputStream os = new ByteArrayOutputStream();
        document.getStatementOrBundle().addAll(activities);
        document.getStatementOrBundle().addAll(agents);
        document.getStatementOrBundle().addAll(wasAssociatedWiths);
        document.getStatementOrBundle().addAll(entities);
        interopFramework.writeDocument(os, InteropFramework.ProvFormat.JSON, document);

        return os.toString();
    }

    private void processAllEntities(Repository repository, List<Entity> entities) throws IOException {
        List<RepositoryContents> contents = contentsService.getContents(repository);

        for (RepositoryContents content : contents) {
            String type = content.getType();
            switch (type) {
                case RepositoryContents.TYPE_FILE:
                    entities.add(processEntityFile(content));
                    break;
                case RepositoryContents.TYPE_DIR:
                    processEntityDirectory(content, repository, entities);
                    break;
                default:
                    logger.warn(content.getName());
                    break;
            }
        }
    }

    private void processEntityDirectory(RepositoryContents directory, Repository repository, List<Entity> entities) throws IOException {
        List<RepositoryContents> contents = contentsService.getContents(repository, directory.getPath());

        for (RepositoryContents content : contents) {
            String type = content.getType();
            switch (type) {
                case RepositoryContents.TYPE_FILE:
                    entities.add(processEntityFile(content));
                    break;
                case RepositoryContents.TYPE_DIR:
                    processEntityDirectory(content, repository, entities);
                    break;
                default:
                    logger.warn(content.getName() + " : " + content.getType());
                    break;
            }
        }

    }

    private Entity processEntityFile(RepositoryContents content) {
        // TODO find a way to register as much info into a entity record
        return provFactory.newEntity(getQualifiedName("file-" + content.getPath().replace('.', '-'), PROVENANCE_PREFIX), content.getSha());
    }

    private Activity processActivity(RepositoryCommit repositoryCommit) {
        return provFactory.newActivity(getQualifiedName("commit-" + repositoryCommit.getSha(), PROVENANCE_PREFIX), repositoryCommit.getCommit().getMessage());
    }

    private WasAssociatedWith processWasAssociatedWith(RepositoryCommit repositoryCommit, Agent agent, Activity activity, List<WasAssociatedWith> wasAssociatedWiths) {
        final QualifiedName activityQualifiedName = activity.getId();
        final QualifiedName agentQualifiedName = agent.getId();
        List<Attribute> attributes = new ArrayList<>();
        WasAssociatedWith wasAssociatedWithResult;

        wasAssociatedWithResult = provFactory.newWasAssociatedWith(getQualifiedName("association-" + repositoryCommit.getSha(), PROVENANCE_PREFIX), activityQualifiedName, agentQualifiedName, null, attributes);
        wasAssociatedWiths.add(wasAssociatedWithResult);

        return wasAssociatedWithResult;
    }

    private Agent processAgent(RepositoryCommit repositoryCommit, List<Agent> agents) {
        final String authorName, authorEmail, authorLogin, authorUrl;
        Agent result;

        authorLogin = repositoryCommit.getCommitter().getLogin();
        authorEmail = repositoryCommit.getCommit().getAuthor().getEmail();
        authorName = repositoryCommit.getCommit().getAuthor().getName();
        authorUrl = repositoryCommit.getCommitter().getHtmlUrl();

        List<Agent> filteredAgents = agents.stream().filter(agent -> agent.getId().getLocalPart().equals(authorLogin)).collect(Collectors.toList());
        if (filteredAgents.size() != 0) {
            result = filteredAgents.get(0);
        } else {
            List<Attribute> attributes = new ArrayList<>();
            attributes.add(provFactory.newAttribute(FOAF_NS, "name", FOAF_PREFIX, authorName, provFactory.getName().XSD_STRING));
            attributes.add(provFactory.newAttribute(FOAF_NS, "email", FOAF_PREFIX, authorEmail, provFactory.getName().XSD_STRING));
            attributes.add(provFactory.newAttribute(FOAF_NS, "homepage", FOAF_PREFIX, authorUrl, provFactory.getName().XSD_STRING));

            result = provFactory.newAgent(getQualifiedName(authorLogin, PROVENANCE_PREFIX), attributes);
            agents.add(result);
        }

        return result;

    }
}
