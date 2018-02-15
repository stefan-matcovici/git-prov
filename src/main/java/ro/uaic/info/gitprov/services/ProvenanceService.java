package ro.uaic.info.gitprov.services;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.Contributor;
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

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
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

    public static final String DCMI_NS = "http://purl.org/dc/terms/";
    public static final String DCMI_PREFIX = "dcterms";

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
        return namespace.qualifiedName(prefix, qualifiedNameUtils.escapeToXsdLocalName(name), provFactory);
    }

    public String repositoryToDocument(Repository repository, String provenanceNs) throws IOException {
        namespace.register(PROVENANCE_PREFIX, provenanceNs);

        List<RepositoryCommit> repositoryCommits = commitService.getCommits(repository);
        List<Agent> agents = new ArrayList<>();
        List<Activity> activities = new ArrayList<>();
        List<Entity> entities = new ArrayList<>();
        List<Entity> baseEntities = new ArrayList<>();
        List<WasAssociatedWith> wasAssociatedWiths = new ArrayList<>();
        List<SpecializationOf> specializationOfs = new ArrayList<>();
        List<CommitFile> commitFiles;
        List<WasGeneratedBy> wasGeneratedBies = new ArrayList<>();
        List<WasInvalidatedBy> wasInvalidatedBies = new ArrayList<>();

        // TODO create agents from contributors list, it's faster!
//        processAllAgents(repository, agents);

        for (RepositoryCommit repositoryCommit : repositoryCommits) {
            Agent agent = processAgent(repositoryCommit, agents);
            Activity activity = processActivity(repositoryCommit);
            processWasAssociatedWith(repositoryCommit, agent, activity, wasAssociatedWiths);

            commitFiles = commitService.getCommit(repository, repositoryCommit.getSha()).getFiles();
            commitFiles.forEach(commitFile -> {
                Entity newEntity = processEntity(getStandardizedSpecializedFilename(getStandardizedBaseFilename(commitFile.getFilename()), repositoryCommit.getSha()), commitFile.getFilename());
                processFile(commitFile.getFilename(), repositoryCommit.getSha(), newEntity, specializationOfs, baseEntities);
                String status = commitFile.getStatus();
                switch (status) {
                    case "added":
                        processGeneratedBy(repositoryCommit.getSha(), repositoryCommit.getCommit().getAuthor().getDate(), newEntity, activity, wasGeneratedBies);
                    case "removed":
                        processInvalidatedBy(repositoryCommit.getSha(), repositoryCommit.getCommit().getAuthor().getDate(), newEntity, activity, wasInvalidatedBies);
                }
                entities.add(newEntity);
            });

            activities.add(activity);
        }

        Document document = provFactory.newDocument();
        document.setNamespace(namespace);

        OutputStream os = new ByteArrayOutputStream();
        document.getStatementOrBundle().addAll(activities);
        document.getStatementOrBundle().addAll(agents);
        document.getStatementOrBundle().addAll(wasAssociatedWiths);
        document.getStatementOrBundle().addAll(entities);
        document.getStatementOrBundle().addAll(baseEntities);
        document.getStatementOrBundle().addAll(specializationOfs);
        document.getStatementOrBundle().addAll(wasGeneratedBies);
        document.getStatementOrBundle().addAll(wasInvalidatedBies);
        interopFramework.writeDocument(os, InteropFramework.ProvFormat.JSON, document);

        return os.toString();
    }

    private void processInvalidatedBy(String sha, Date date, Entity newEntity, Activity activity, List<WasInvalidatedBy> wasInvalidatedBies) {
        WasInvalidatedBy wasInvalidatedBy;
        try {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(date);
            XMLGregorianCalendar time = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
            wasInvalidatedBy = provFactory.newWasInvalidatedBy(getQualifiedName("generation" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId(), time, null);
        } catch (DatatypeConfigurationException e) {
            wasInvalidatedBy = provFactory.newWasInvalidatedBy(getQualifiedName("generation" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId());
        }

        wasInvalidatedBies.add(wasInvalidatedBy);
    }

    private void processGeneratedBy(String sha, Date date, Entity newEntity, Activity activity, List<WasGeneratedBy> wasGeneratedBies) {

        WasGeneratedBy wasGeneratedBy;
        try {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(date);
            XMLGregorianCalendar time = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
            wasGeneratedBy = provFactory.newWasGeneratedBy(getQualifiedName("generation" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId(), time, null);
        } catch (DatatypeConfigurationException e) {
            wasGeneratedBy = provFactory.newWasGeneratedBy(getQualifiedName("generation" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId());
        }

        wasGeneratedBies.add(wasGeneratedBy);
    }

    private String getStandardizedSpecializedFilename(String standardizedBaseFilename, String sha) {
        return standardizedBaseFilename + "_commit-" + sha;
    }

    private String getStandardizedBaseFilename(String filename) {
        return "file-" + filename.replaceAll("[/\\\\.]", "-");
    }

    private void processFile(String filename, String sha, Entity newEntity, List<SpecializationOf> specializationOfs, List<Entity> baseEntities) {
        SpecializationOf specializationOf;
        String label = newEntity.getLabel().get(0).getValue();
        Entity baseEntity;

        List<Entity> pastEntities = baseEntities.stream().filter(entity -> entity.getLabel().get(0).getValue().equals(label)).collect(Collectors.toList());
        if (pastEntities.size() > 0) {
            baseEntity = pastEntities.get(0);
        } else {
            baseEntity = processEntity(getStandardizedBaseFilename(filename), filename);
            baseEntities.add(baseEntity);
        }

        specializationOf = provFactory.newSpecializationOf(newEntity.getId(), baseEntity.getId());
        specializationOfs.add(specializationOf);
    }

    private void processAllAgents(Repository repository, List<Agent> agents) throws IOException {
        String authorName, authorImage, authorLogin, authorUrl;
        Agent agent;

        List<Contributor> contributors = repositoryService.getContributors(repository, false);

        for (Contributor contributor : contributors) {
            authorName = contributor.getName();
            authorLogin = contributor.getLogin();
            authorUrl = contributor.getUrl();

            List<Attribute> attributes = new ArrayList<>();
            attributes.add(provFactory.newAttribute(FOAF_NS, "name", FOAF_PREFIX, authorName, provFactory.getName().XSD_STRING));
            attributes.add(provFactory.newAttribute(FOAF_NS, "homepage", FOAF_PREFIX, authorUrl, provFactory.getName().XSD_STRING));

            agent = provFactory.newAgent(getQualifiedName(authorLogin.replace(' ', '-'), PROVENANCE_PREFIX), attributes);
            agents.add(agent);
        }
    }

    private Entity processEntity(String name, String label) {
        return provFactory.newEntity(getQualifiedName(name, PROVENANCE_PREFIX), label);
    }

    private Activity processActivity(RepositoryCommit repositoryCommit) {
        return provFactory.newActivity(getQualifiedName("commit-" + repositoryCommit.getSha(), PROVENANCE_PREFIX), repositoryCommit.getCommit().getMessage());
    }

    private WasAssociatedWith processWasAssociatedWith(RepositoryCommit repositoryCommit, Agent agent, Activity activity, List<WasAssociatedWith> wasAssociatedWiths) {
        final QualifiedName activityQualifiedName = activity.getId();
        final QualifiedName agentQualifiedName = agent.getId();
        List<Attribute> attributes = new ArrayList<>();
        WasAssociatedWith wasAssociatedWithResult;

        // TODO add role
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

            result = provFactory.newAgent(getQualifiedName(authorLogin.replace(' ', '-'), PROVENANCE_PREFIX), attributes);
            agents.add(result);
        }

        return result;

    }
}
