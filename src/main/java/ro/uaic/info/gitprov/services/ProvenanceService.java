package ro.uaic.info.gitprov.services;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.*;
import org.eclipse.egit.github.core.service.CommitService;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * The Provenance service.
 */
@Service
public class ProvenanceService {

    /**
     * The constant logger.
     */
    final static Logger logger = Logger.getLogger(ProvenanceService.class);

    /**
     * The constant PROVENANCE_PREFIX.
     */
    private static final String PROVENANCE_PREFIX = "gitprov";
    /**
     * The constant Friend of a Friend base uri.
     */
    private static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
    /**
     * The constant Friend of a Friend prefix.
     */
    private static final String FOAF_PREFIX = "foaf";
    /**
     * The factory object used to create all the provenance entries.
     */
    private final ProvFactory provFactory = InteropFramework.newXMLProvFactory();
    /**
     * The namespace object that stores all the namespaces used in the document.
     */
    private final Namespace namespace;
    /**
     * The Commit service used to get all the information needed about a particular commit.
     */
    @Autowired
    private CommitService commitService;

    /**
     * The Repository service that gets all the commits and repository information.
     */
    @Autowired
    private RepositoryService repositoryService;

    /**
     * The Interop framework that writes the document in different formats.
     */
    private InteropFramework interopFramework = new InteropFramework();

    /**
     * The Qualified name utils used to construct correctly formatted qualified names
     */
    private QualifiedNameUtils qualifiedNameUtils = new QualifiedNameUtils();

    /**
     * Lists containing all the provenance entries objects used in the document
     */
    private List<Agent> agents;
    private List<Activity> activities;
    private List<Entity> entities;
    private List<Entity> baseEntities;
    private List<WasAssociatedWith> wasAssociatedWiths;
    private List<SpecializationOf> specializationOfs;
    private List<WasGeneratedBy> wasGeneratedBies;
    private List<WasInvalidatedBy> wasInvalidatedBies;
    private List<Used> used;
    private List<WasInformedBy> wasInformedBies;
    private List<WasDerivedFrom> wasDerivedFroms;
    private Map<String, List<String>> entityVersions;

    /**
     * Instantiates a new Provenance service registering the used namespaces
     */
    public ProvenanceService() {
        namespace = new Namespace();
        namespace.addKnownNamespaces();
        namespace.register(FOAF_PREFIX, FOAF_NS);
    }

    /**
     * Generates a provenance document from a Github repository
     *
     * @param repository   the repository object that references the targeted repository for provenance
     * @param provenanceNs the provenance namespace, the uri of the resource
     * @return the string
     * @throws IOException io exception
     */
    public String repositoryToDocument(Repository repository, String provenanceNs) throws IOException {
        namespace.register(PROVENANCE_PREFIX, provenanceNs);

        List<RepositoryCommit> repositoryCommits = commitService.getCommits(repository);
        List<CommitFile> commitFiles;

        init();

        // TODO create agents from contributors list, it's faster!
//        processAllAgents(repository, agents);

        // Safer but more time consuming
//        Comparator<RepositoryCommit> byTime = (RepositoryCommit r1, RepositoryCommit r2)->r1.getCommit().getAuthor().getDate().compareTo(r2.getCommit().getAuthor().getDate());
//        repositoryCommits.sort(byTime);
        Collections.reverse(repositoryCommits);
        for (RepositoryCommit repositoryCommit : repositoryCommits) {
            Agent agent = processAgent(repositoryCommit);
            Activity activity = processActivity(repositoryCommit);
            processWasAssociatedWith(repositoryCommit, agent, activity);

            final String sha = repositoryCommit.getSha();
            commitFiles = commitService.getCommit(repository, sha).getFiles();
            commitFiles.forEach(commitFile -> {
                final String filename = commitFile.getFilename();
                Entity newEntity = processEntity(getStandardizedSpecializedFilename(filename, sha), filename);
                Entity baseEntity = processFile(filename, newEntity);

                String status = commitFile.getStatus();
                final Date date = repositoryCommit.getCommit().getAuthor().getDate();
                switch (status) {
                    case "added":
                        processGeneratedBy(sha, date, baseEntity, activity);
                        processGeneratedBy(sha, date, newEntity, activity);
                        break;
                    case "removed":
                        processInvalidatedBy(sha, date, newEntity, activity);
                        break;
                    case "modified":
                        processGeneratedBy(sha, date, newEntity, activity);
                        processUsed(sha, filename, activity, date);
                        processWasDerivedFrom(sha, filename);
                        break;
                }
                entities.add(newEntity);
                registerVersion(filename, sha);
            });
            activities.add(activity);
            processWasInformedBy(sha, activity, repositoryCommit.getParents());
        }

        return getDocument();
    }

    /**
     * Constructs the provenance document from the previously populated lists of provenance records objects
     *
     * @return the document
     */
    private String getDocument() {
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
        document.getStatementOrBundle().addAll(used);
        document.getStatementOrBundle().addAll(wasInformedBies);
        document.getStatementOrBundle().addAll(wasDerivedFroms);
        interopFramework.writeDocument(os, InteropFramework.ProvFormat.TURTLE, document);
        return os.toString();
    }

    /**
     * Gets qualified name.
     *
     * @param name   the name
     * @param prefix the prefix
     * @return the qualified name
     */
    public QualifiedName getQualifiedName(String name, String prefix) {
        return namespace.qualifiedName(prefix, qualifiedNameUtils.escapeToXsdLocalName(name), provFactory);
    }

    /**
     * Initializes the list of provenance records objects
     */
    private void init() {
        agents = new ArrayList<>();
        activities = new ArrayList<>();
        entities = new ArrayList<>();
        baseEntities = new ArrayList<>();
        wasAssociatedWiths = new ArrayList<>();
        specializationOfs = new ArrayList<>();
        wasGeneratedBies = new ArrayList<>();
        wasInvalidatedBies = new ArrayList<>();
        used = new ArrayList<>();
        wasInformedBies = new ArrayList<>();
        wasDerivedFroms = new ArrayList<>();
        entityVersions = new HashMap<>();
    }

    /**
     * Stores all the versions of a file
     *
     * @param filename the name of the file
     * @param sha the sha of the commit
     */
    private void registerVersion(String filename, String sha) {
        entityVersions.computeIfAbsent(filename, k -> new ArrayList<>());

        List<String> list = entityVersions.get(filename);
        list.add(sha);
    }

    /**
     * Generates an activity provenance record object
     *
     * @param repositoryCommit the repository object that references the targeted repository for provenance
     * @return the generated activity
     */
    private Activity processActivity(RepositoryCommit repositoryCommit) {
        return provFactory.newActivity(getQualifiedName("commit-" + repositoryCommit.getSha(), PROVENANCE_PREFIX), repositoryCommit.getCommit().getMessage());
    }

    /**
     * Generates an agent provenance record object
     *
     * @param repositoryCommit the commit object that references the targeted repository for provenance record
     * @return the generated agent
     */
    private Agent processAgent(RepositoryCommit repositoryCommit) {
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

    /**
     * Generates an entity provenance record object
     *
     * @param name  the name of the entity
     * @param label the label of the entity
     * @return the generated entity
     */
    private Entity processEntity(String name, String label) {
        return provFactory.newEntity(getQualifiedName(name, PROVENANCE_PREFIX), label);
    }

    private void processAllAgents(Repository repository) throws IOException {
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

    /**
     * Given a filename and a corresponding entity, registers it into the baseEntities if it is the case (if it does not
     * already exists in there) and into the specializationOfs
     *
     * @param filename the name of the file
     * @param newEntity the previously generated entity corresponding with the filename
     */
    private Entity processFile(String filename, Entity newEntity) {
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

        return baseEntity;
    }

    /**
     * Registers a generatedBy provenance record object
     *
     * @param sha the sha of the commit
     * @param date the date of the commit
     * @param newEntity the previously generated entity
     * @param activity the corresponding activity that generated the entity
     */
    private void processGeneratedBy(String sha, Date date, Entity newEntity, Activity activity) {

        WasGeneratedBy wasGeneratedBy;
        try {
            XMLGregorianCalendar time = getXmlGregorianCalendar(date);
            wasGeneratedBy = provFactory.newWasGeneratedBy(getQualifiedName("generation-" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId(), time, null);
        } catch (DatatypeConfigurationException e) {
            wasGeneratedBy = provFactory.newWasGeneratedBy(getQualifiedName("generation-" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId());
        }

        wasGeneratedBies.add(wasGeneratedBy);
    }

    /**
     * Registers an invalidatedBy provenance record object
     *
     * @param sha       the sha of the commit
     * @param date      the date of the commit
     * @param newEntity the previously generated entity
     * @param activity  the corresponding activity that generated the entity
     */
    private void processInvalidatedBy(String sha, Date date, Entity newEntity, Activity activity) {
        WasInvalidatedBy wasInvalidatedBy;
        try {
            XMLGregorianCalendar time = getXmlGregorianCalendar(date);
            wasInvalidatedBy = provFactory.newWasInvalidatedBy(getQualifiedName("invalidation-" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId(), time, null);
        } catch (DatatypeConfigurationException e) {
            wasInvalidatedBy = provFactory.newWasInvalidatedBy(getQualifiedName("invalidation-" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId());
        }

        wasInvalidatedBies.add(wasInvalidatedBy);
    }

    /**
     * Registers an used provenance record object
     *
     * @param sha      the sha of the commit
     * @param filename the name of the file
     * @param activity the corresponding activity that generated the entity
     * @param date     the date of the commit
     */
    private void processUsed(String sha, String filename, Activity activity, Date date) {
        Used u;
        String parentCommitSha = getParentCommitSha(filename);
        try {
            QualifiedName parentEntityQualifiedName;
            XMLGregorianCalendar time = getXmlGregorianCalendar(date);
            parentEntityQualifiedName = getQualifiedName(getStandardizedSpecializedFilename(filename, parentCommitSha), PROVENANCE_PREFIX);
            u = provFactory.newUsed(getQualifiedName("usage-" + sha + "-" + parentCommitSha, PROVENANCE_PREFIX), activity.getId(), parentEntityQualifiedName, time, null);
        } catch (Exception e) {
            QualifiedName parentEntityQualifiedName;
            parentEntityQualifiedName = getQualifiedName(getStandardizedSpecializedFilename(filename, parentCommitSha), PROVENANCE_PREFIX);
            u = provFactory.newUsed(getQualifiedName("usage-" + sha + "-" + parentCommitSha, PROVENANCE_PREFIX), activity.getId(), parentEntityQualifiedName);
        }

        used.add(u);
    }

    /**
     * Registers an wasAssociated provenance record object
     *
     * @param repositoryCommit the commit object that references the targeted repository for provenance record
     * @param agent            the agent that created the entity
     * @param activity         the activity that generated the entity
     */
    private void processWasAssociatedWith(RepositoryCommit repositoryCommit, Agent agent, Activity activity) {
        final QualifiedName activityQualifiedName = activity.getId();
        final QualifiedName agentQualifiedName = agent.getId();
        List<Attribute> attributes = new ArrayList<>();
        WasAssociatedWith wasAssociatedWithResult;

        // TODO add role
        wasAssociatedWithResult = provFactory.newWasAssociatedWith(getQualifiedName("association-" + repositoryCommit.getSha(), PROVENANCE_PREFIX), activityQualifiedName, agentQualifiedName, null, attributes);
        wasAssociatedWiths.add(wasAssociatedWithResult);
    }

    /**
     * Registers an wasDerivedFrom provenance record object
     *
     * @param sha      the sha of the commit
     * @param filename the name of the file
     */
    private void processWasDerivedFrom(String sha, String filename) {
        String parentCommitSha = getParentCommitSha(filename);
        QualifiedName generatedEntity = getQualifiedName(getStandardizedSpecializedFilename(filename, sha), PROVENANCE_PREFIX);
        QualifiedName usedEntity = getQualifiedName(getStandardizedSpecializedFilename(filename, parentCommitSha), PROVENANCE_PREFIX);
        QualifiedName activity = getQualifiedName("commit-" + sha, PROVENANCE_PREFIX);
        QualifiedName used = getQualifiedName("usage-" + sha + "-" + parentCommitSha, PROVENANCE_PREFIX);
        QualifiedName wasDerivedFromId = getQualifiedName("derivation-" + getStandardizedSpecializedFilename(filename, sha) + "-" + parentCommitSha, PROVENANCE_PREFIX);
        QualifiedName generation = getQualifiedName("generation-" + sha, PROVENANCE_PREFIX);
        wasDerivedFroms.add(provFactory.newWasDerivedFrom(wasDerivedFromId, generatedEntity, usedEntity, activity, generation, used, null));
    }

    /**
     * Generates an wasInformedBy provenance record object
     *
     * @param sha      the sha of the resource
     * @param activity the activity that generated the entity
     * @param parents  the parent commits of the current commit
     */
    private void processWasInformedBy(String sha, Activity activity, List<Commit> parents) {
        String parentSha;
        for (Commit commit : parents) {
            parentSha = commit.getSha();
            wasInformedBies.add(provFactory.newWasInformedBy(getQualifiedName("information-" + parentSha + "-" + sha, PROVENANCE_PREFIX), activity.getId(), getQualifiedName("commit-" + parentSha, PROVENANCE_PREFIX)));
        }
    }

    /**
     * Generates an identifier for a resource within a commit
     *
     * @param filename the name of the file
     * @param sha      the sha of the commit
     * @return the identifier
     */
    private String getStandardizedSpecializedFilename(String filename, String sha) {
        return getStandardizedBaseFilename(filename) + "_commit-" + sha;
    }

    /**
     * Generates an identifier for a generic file
     *
     * @param filename the name of the file
     * @return the identifier
     */
    private String getStandardizedBaseFilename(String filename) {
        return "file-" + filename.replaceAll("[/\\\\.]", "-");
    }

    /**
     * Provides the sha of the last commit that modified the resource with the filename
     *
     * @param filename the name of the file
     * @return the parent commit sha
     */
    private String getParentCommitSha(String filename) {
        List<String> list = entityVersions.get(filename);
        return list.get(list.size() - 1);
    }

    /**
     * Transforms a Date object into a XMLGregorianCalendar object
     *
     * @param date the Date object
     * @return the MLGregorianCalendar object
     */
    private XMLGregorianCalendar getXmlGregorianCalendar(Date date) throws DatatypeConfigurationException {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
    }

}
