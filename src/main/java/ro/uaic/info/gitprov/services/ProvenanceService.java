package ro.uaic.info.gitprov.services;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.*;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
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
     * The Commit service used to get all the information needed about a particular commit.
     */
    @Autowired
    private CommitService commitService;

    @Autowired
    private UserService userService;

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
    private String githubUserUrl;
    private String githubRepoUrl;

    /**
     * Instantiates a new Provenance service registering the used namespaces
     */
    public ProvenanceService() {
    }

    /**
     * Generates a provenance document from a Github repository
     *
     * @param repository   the repository object that references the targeted repository for provenance
     * @param provenanceNs the provenance namespace, the uri of the resource
     * @param contentType
     * @return the string
     * @throws IOException io exception
     */
    public String repositoryToDocument(Repository repository, String provenanceNs, String contentType) throws IOException {
        Namespace namespace = new Namespace();
        namespace.addKnownNamespaces();
        namespace.register(FOAF_PREFIX, FOAF_NS);

        namespace.register(PROVENANCE_PREFIX, provenanceNs);

        List<RepositoryCommit> repositoryCommits = commitService.getCommits(repository);
        List<CommitFile> commitFiles;

        ArrayList<Agent> agents = new ArrayList<>();
        ArrayList<Activity> activities = new ArrayList<>();
        ArrayList<Entity> entities = new ArrayList<>();
        ArrayList<Entity> baseEntities = new ArrayList<>();
        ArrayList<WasAssociatedWith> wasAssociatedWiths = new ArrayList<>();
        ArrayList<SpecializationOf> specializationOfs = new ArrayList<>();
        ArrayList<WasGeneratedBy> wasGeneratedBies = new ArrayList<>();
        ArrayList<WasInvalidatedBy> wasInvalidatedBies = new ArrayList<>();
        ArrayList<Used> used = new ArrayList<>();
        ArrayList<WasInformedBy> wasInformedBies = new ArrayList<>();
        ArrayList<WasDerivedFrom> wasDerivedFroms = new ArrayList<>();
        HashMap<String, List<String>> entityVersions = new HashMap<>();

        githubUserUrl = "https://github.com/" + repository.getOwner().getLogin();
        githubRepoUrl = githubUserUrl + "/" + repository.getName();

        processAllAgents(namespace, provenanceNs, repository, agents);

        Collections.reverse(repositoryCommits);
        for (RepositoryCommit repositoryCommit : repositoryCommits) {
            final String sha = repositoryCommit.getSha();

            Commit commit = repositoryCommit.getCommit();

            final String commitMessage = commit.getMessage();

            final CommitUser commitAuthor = commit.getAuthor();
            final Date authorDate = commitAuthor.getDate();
            final User author = repositoryCommit.getAuthor();
            String authorName = null;

            try {
                if (author == null) {
                    authorName = agents.stream()
                            .filter(agent -> {
                                List<Other> otherList = agent.getOther();
                                for (Other other : otherList) {
                                    if (other.getElementName().getLocalPart().equals("name")) {
                                        return String.valueOf(other.getValue()).equals(repositoryCommit.getCommit().getAuthor().getName());
                                    }
                                }
                                return true;
                            })
                            .map(agent -> agent.getLabel().get(0).getValue())
                            .collect(Collectors.toList()).get(0);

                } else {
                    authorName = author.getLogin();
                }
            } catch (Exception exc) {
                logger.info(String.format("Commit with sha %s does not have an author (%s) from contributor list!", repositoryCommit.getSha(), repositoryCommit.getCommit().getAuthor().getName()));
                continue;
            }


            Activity activity = processActivity(namespace, sha, authorDate, commitMessage);
            processWasAssociatedWith(namespace, sha, authorName, activity.getId(), wasAssociatedWiths);

            // TODO process startedBy
            // TODO process endedBy

            commitFiles = commitService.getCommit(repository, sha).getFiles();
            commitFiles.forEach((CommitFile commitFile) -> {
                final String filename = commitFile.getFilename();
                Entity newEntity = processEntity(namespace, getStandardizedSpecializedFilename(filename, sha), filename);
                processSpecializationOf(namespace, filename, newEntity, sha, baseEntities, specializationOfs);

                String status = commitFile.getStatus();
                switch (status) {
                    case "added":
                        processWasGeneratedBy(namespace, sha, filename, authorDate, newEntity, activity, wasGeneratedBies);
                        break;
                    case "removed":
                        processInvalidatedBy(namespace, sha, filename, authorDate, newEntity, activity, wasInvalidatedBies);
                        break;
                    case "modified":
                        processWasGeneratedBy(namespace, sha, filename, authorDate, newEntity, activity, wasGeneratedBies);
                        processUsed(namespace, sha, filename, authorDate, activity, used, entityVersions);
                        processWasDerivedFrom(namespace, provenanceNs, sha, filename, commitFile.getAdditions(), commitFile.getChanges(), commitFile.getDeletions(), wasDerivedFroms, entityVersions);
                        break;
                }
                entities.add(newEntity);
                registerVersion(filename, sha, entityVersions);
            });
            activities.add(activity);
            processWasInformedBy(namespace, sha, activity, repositoryCommit.getParents(), wasInformedBies);
        }

        return getDocument(namespace, contentType, activities, agents, wasAssociatedWiths, entities, baseEntities, specializationOfs, wasGeneratedBies, wasInvalidatedBies, used, wasInformedBies, wasDerivedFroms);
    }

    /**
     * Constructs the provenance document from the previously populated lists of provenance records objects
     *
     * @param namespace
     * @param contentType
     * @param activities
     * @param agents
     * @param wasAssociatedWiths
     * @param entities
     * @return the document
     */
    private String getDocument(Namespace namespace, String contentType, List<Activity> activities, List<Agent> agents,
                               List<WasAssociatedWith> wasAssociatedWiths, List<Entity> entities,
                               List<Entity> baseEntities, List<SpecializationOf> specializationOfs,
                               List<WasGeneratedBy> wasGeneratedBies, List<WasInvalidatedBy> wasInvalidatedBies,
                               List<Used> used, List<WasInformedBy> wasInformedBies, List<WasDerivedFrom> wasDerivedFroms
    ) {
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

        InteropFramework.ProvFormat provFormat = null;

        switch (contentType) {
            case "text/provenance-notation":
                provFormat = InteropFramework.ProvFormat.PROVN;
                break;
            case "application/x-turtle":
                provFormat = InteropFramework.ProvFormat.TURTLE;
                break;
            case "application/xml":
                provFormat = InteropFramework.ProvFormat.XML;
                break;
            case "application/rdf+xml":
                provFormat = InteropFramework.ProvFormat.RDFXML;
                break;
            case "application/pdf":
                provFormat = InteropFramework.ProvFormat.PDF;
                break;
            case "application/json":
                provFormat = InteropFramework.ProvFormat.JSON;
                break;
            case "application/msword":
                provFormat = InteropFramework.ProvFormat.DOT;
                break;
            case "image/svg+xml":
                provFormat = InteropFramework.ProvFormat.SVG;
                break;
            case "image/png":
                provFormat = InteropFramework.ProvFormat.PNG;
                break;
            case "image/jpeg":
                provFormat = InteropFramework.ProvFormat.JPEG;
                break;
            case "application/trig":
                provFormat = InteropFramework.ProvFormat.TRIG;
                break;
        }

        interopFramework.writeDocument(os, provFormat, document);
        return os.toString();
    }

    /**
     * Gets qualified name.
     *
     * @param namespace
     * @param name      the name
     * @param prefix    the prefix
     * @return the qualified name
     */
    public QualifiedName getQualifiedName(Namespace namespace, String name, String prefix) {
        return namespace.qualifiedName(prefix, qualifiedNameUtils.escapeToXsdLocalName(name), provFactory);
    }

    /**
     * Stores all the versions of a file
     *
     * @param filename       the name of the file
     * @param sha            the sha of the commit
     * @param entityVersions
     */
    private void registerVersion(String filename, String sha, HashMap<String, List<String>> entityVersions) {
        entityVersions.computeIfAbsent(filename, k -> new ArrayList<>());

        List<String> list = entityVersions.get(filename);
        list.add(sha);
    }


    private Activity processActivity(Namespace namespace, String sha, Date date, String commitMessage) {
        String commitUrl = githubRepoUrl + "/commit/" + sha;
        Activity result;

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(provFactory.newAttribute(FOAF_NS, "homepage", FOAF_PREFIX, commitUrl, provFactory.getName().XSD_ANY_URI));
        attributes.add(provFactory.newAttribute(Attribute.AttributeKind.PROV_LABEL, commitMessage, provFactory.getName().XSD_STRING));

        try {
            XMLGregorianCalendar time = getXmlGregorianCalendar(date);
            result = provFactory.newActivity(getQualifiedName(namespace, "commit-" + sha, PROVENANCE_PREFIX), time, null, attributes);

        } catch (DatatypeConfigurationException e) {
            result = provFactory.newActivity(getQualifiedName(namespace, "commit-" + sha, PROVENANCE_PREFIX), commitMessage);
        }

        return result;
    }

    /**
     * Generates an entity provenance record object
     *
     * @param namespace
     * @param name      the name of the entity
     * @param label     the label of the entity
     * @return the generated entity
     */
    private Entity processEntity(Namespace namespace, String name, String label) {
        return provFactory.newEntity(getQualifiedName(namespace, name.replace(' ', '-'), PROVENANCE_PREFIX), label);
    }

    private void processAllAgents(Namespace namespace, String provenanceNs, Repository repository, List<Agent> agents) throws IOException {
        String type, authorLogin, authorUrl;
        Agent agent;

        List<Contributor> contributors = repositoryService.getContributors(repository, false);

        for (Contributor contributor : contributors) {
            type = contributor.getType();
            authorLogin = contributor.getLogin();
            authorUrl = githubUserUrl;
            User user = userService.getUser(authorLogin);

            List<Attribute> attributes = new ArrayList<>();
            attributes.add(provFactory.newAttribute(Attribute.AttributeKind.PROV_TYPE, type, provFactory.getName().XSD_STRING));
            attributes.add(provFactory.newAttribute(FOAF_NS, "homepage", FOAF_PREFIX, authorUrl, provFactory.getName().XSD_ANY_URI));
            attributes.add(provFactory.newAttribute(Attribute.AttributeKind.PROV_LABEL, authorLogin, provFactory.getName().XSD_STRING));
            attributes.add(provFactory.newAttribute(provenanceNs, "contributions", PROVENANCE_PREFIX, contributor.getContributions(), provFactory.getName().XSD_INT));

            String email = user.getEmail();
            if (email != null) {
                attributes.add(provFactory.newAttribute(FOAF_NS, "mbox", FOAF_PREFIX, email, provFactory.getName().XSD_STRING));
            }

            attributes.add(provFactory.newAttribute(FOAF_NS, "img", FOAF_PREFIX, user.getAvatarUrl(), provFactory.getName().XSD_ANY_URI));

            String name = user.getName();
            if (name != null) {
                attributes.add(provFactory.newAttribute(FOAF_NS, "name", FOAF_PREFIX, name, provFactory.getName().XSD_STRING));
            }

            agent = provFactory.newAgent(getQualifiedName(namespace, getAuthorLoginLabel(authorLogin), PROVENANCE_PREFIX), attributes);
            agents.add(agent);
        }
    }

    private String getAuthorLoginLabel(String authorLogin) {
        return authorLogin.replace(' ', '-');
    }

    /**
     * Given a filename and a corresponding entity, registers it into the baseEntities if it is the case (if it does not
     * already exists in there) and into the specializationOfs
     *
     * @param namespace
     * @param filename          the name of the file
     * @param newEntity         the previously generated entity corresponding with the filename
     * @param baseEntities
     * @param specializationOfs
     */
    private void processSpecializationOf(Namespace namespace, String filename, Entity newEntity, String sha, List<Entity> baseEntities, List<SpecializationOf> specializationOfs) {
        SpecializationOf specializationOf;
        String label = newEntity.getLabel().get(0).getValue();
        Entity baseEntity;

        List<Entity> pastEntities = baseEntities.stream().filter(entity -> entity.getLabel().get(0).getValue().equals(label)).collect(Collectors.toList());
        if (pastEntities.size() > 0) {
            baseEntity = pastEntities.get(0);
        } else {
            baseEntity = processEntity(namespace, getStandardizedBaseFilename(filename), filename);
            baseEntities.add(baseEntity);
        }

        specializationOf = provFactory.newSpecializationOf(newEntity.getId(), baseEntity.getId());
        // specializationOf = provFactory.newQualifiedSpecializationOf(getQualifiedName("specialization-"+baseEntity.getLabel().get(0).getValue()+"-"+sha, PROVENANCE_PREFIX), newEntity.getId(), baseEntity.getId(), null);
        specializationOfs.add(specializationOf);
    }

    /**
     * Registers a WasGeneratedBy provenance record object
     *
     * @param namespace
     * @param sha              the sha of the commit
     * @param filename         the name of the file that was generated
     * @param date             the date of the commit
     * @param newEntity        the previously generated entity
     * @param activity         the corresponding activity that generated the entity
     * @param wasGeneratedBies
     */
    private void processWasGeneratedBy(Namespace namespace, String sha, String filename, Date date, Entity newEntity, Activity activity, List<WasGeneratedBy> wasGeneratedBies) {

        WasGeneratedBy wasGeneratedBy;
        try {
            XMLGregorianCalendar time = getXmlGregorianCalendar(date);
            wasGeneratedBy = provFactory.newWasGeneratedBy(getQualifiedName(namespace, "generation-" + getStandardizedBaseFilename(filename) + "-" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId(), time, null);
        } catch (DatatypeConfigurationException e) {
            wasGeneratedBy = provFactory.newWasGeneratedBy(getQualifiedName(namespace, "generation-" + getStandardizedBaseFilename(filename) + "-" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId());
        }

        wasGeneratedBies.add(wasGeneratedBy);
    }

    /**
     * Registers an invalidatedBy provenance record object
     *
     * @param namespace
     * @param sha                the sha of the commit
     * @param filename
     * @param date               the date of the commit
     * @param newEntity          the previously generated entity
     * @param activity           the corresponding activity that generated the entity
     * @param wasInvalidatedBies
     */
    private void processInvalidatedBy(Namespace namespace, String sha, String filename, Date date, Entity newEntity, Activity activity, List<WasInvalidatedBy> wasInvalidatedBies) {
        WasInvalidatedBy wasInvalidatedBy;
        try {
            XMLGregorianCalendar time = getXmlGregorianCalendar(date);
            wasInvalidatedBy = provFactory.newWasInvalidatedBy(getQualifiedName(namespace, "invalidation-" + getStandardizedBaseFilename(filename) + "-" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId(), time, null);
        } catch (DatatypeConfigurationException e) {
            wasInvalidatedBy = provFactory.newWasInvalidatedBy(getQualifiedName(namespace, "invalidation-" + getStandardizedBaseFilename(filename) + "-" + sha, PROVENANCE_PREFIX), newEntity.getId(), activity.getId());
        }

        wasInvalidatedBies.add(wasInvalidatedBy);
    }

    /**
     * Registers an used provenance record object
     *
     * @param namespace
     * @param sha       the sha of the commit
     * @param filename  the name of the file
     * @param date      the date of the commit
     * @param activity  the corresponding activity that generated the entity
     * @param used
     */
    private void processUsed(Namespace namespace, String sha, String filename, Date date, Activity activity, List<Used> used, HashMap<String, List<String>> entityVersions) {
        Used u;
        String parentCommitSha = null;
        try {
            parentCommitSha = getParentCommitSha(filename, entityVersions);
        } catch (Exception exc) {
            logger.info(String.format("%s hasn't a parent", filename));
            return;
        }
        try {
            QualifiedName parentEntityQualifiedName;
            XMLGregorianCalendar time = getXmlGregorianCalendar(date);
            parentEntityQualifiedName = getQualifiedName(namespace, getStandardizedSpecializedFilename(filename, parentCommitSha), PROVENANCE_PREFIX);
            u = provFactory.newUsed(getQualifiedName(namespace, "usage-" + getStandardizedBaseFilename(filename) + "-" + sha + "-" + parentCommitSha, PROVENANCE_PREFIX), activity.getId(), parentEntityQualifiedName, time, null);
        } catch (Exception e) {
            QualifiedName parentEntityQualifiedName;
            parentEntityQualifiedName = getQualifiedName(namespace, getStandardizedSpecializedFilename(filename, parentCommitSha), PROVENANCE_PREFIX);
            u = provFactory.newUsed(getQualifiedName(namespace, "usage-" + getStandardizedBaseFilename(filename) + "-" + sha + "-" + parentCommitSha, PROVENANCE_PREFIX), activity.getId(), parentEntityQualifiedName);
        }

        used.add(u);

    }

    /**
     * Registers an wasAssociated provenance record object
     *
     * @param namespace
     * @param sha                the sha of the commit
     * @param authorName         the author's name
     * @param activityId         the qualified name of the activity that generated the entity
     * @param wasAssociatedWiths
     */
    private void processWasAssociatedWith(Namespace namespace, String sha, String authorName, QualifiedName activityId, List<WasAssociatedWith> wasAssociatedWiths) {
        final QualifiedName agentQualifiedName = getQualifiedName(namespace, getAuthorLoginLabel(authorName), PROVENANCE_PREFIX);
        List<Attribute> attributes = new ArrayList<>();
        WasAssociatedWith wasAssociatedWithResult;
        attributes.add(provFactory.newAttribute(Attribute.AttributeKind.PROV_ROLE, "authorship", provFactory.getName().XSD_STRING));

        wasAssociatedWithResult = provFactory.newWasAssociatedWith(getQualifiedName(namespace, "association-" + sha, PROVENANCE_PREFIX), activityId, agentQualifiedName, null, attributes);
        wasAssociatedWiths.add(wasAssociatedWithResult);
    }

    /**
     * Registers an wasDerivedFrom provenance record object
     *
     * @param namespace
     * @param provenanceNs
     * @param sha             the sha of the commit
     * @param filename        the name of the file
     * @param additions
     * @param changes
     * @param deletions
     * @param wasDerivedFroms
     */
    private void processWasDerivedFrom(Namespace namespace, String provenanceNs, String sha, String filename, int additions, int changes, int deletions, List<WasDerivedFrom> wasDerivedFroms, HashMap<String, List<String>> entityVersions) {
        String parentCommitSha = null;
        try {
            parentCommitSha = getParentCommitSha(filename, entityVersions);
        } catch (Exception exc) {
            logger.info(String.format("%s hasn't a parent", filename));
            return;
        }
        List<Attribute> attributes = new ArrayList<>();
        QualifiedName generatedEntity = getQualifiedName(namespace, getStandardizedSpecializedFilename(filename, sha), PROVENANCE_PREFIX);
        QualifiedName usedEntity = getQualifiedName(namespace, getStandardizedSpecializedFilename(filename, parentCommitSha), PROVENANCE_PREFIX);
        QualifiedName activity = getQualifiedName(namespace, "commit-" + sha, PROVENANCE_PREFIX);
        QualifiedName used = getQualifiedName(namespace, "usage-" + sha + "-" + parentCommitSha, PROVENANCE_PREFIX);
        QualifiedName wasDerivedFromId = getQualifiedName(namespace, "derivation-" + getStandardizedSpecializedFilename(filename, sha) + "-" + parentCommitSha, PROVENANCE_PREFIX);
        QualifiedName generation = getQualifiedName(namespace, "generation-" + sha, PROVENANCE_PREFIX);

        attributes.add(provFactory.newAttribute(provenanceNs, "additions", PROVENANCE_PREFIX, additions, provFactory.getName().XSD_INT));
        attributes.add(provFactory.newAttribute(provenanceNs, "changes", PROVENANCE_PREFIX, changes, provFactory.getName().XSD_INT));
        attributes.add(provFactory.newAttribute(provenanceNs, "deletions", PROVENANCE_PREFIX, deletions, provFactory.getName().XSD_INT));

        wasDerivedFroms.add(provFactory.newWasDerivedFrom(wasDerivedFromId, generatedEntity, usedEntity, activity, generation, used, attributes));
    }

    /**
     * Generates an wasInformedBy provenance record object
     *
     * @param namespace
     * @param sha             the sha of the resource
     * @param activity        the activity that generated the entity
     * @param parents         the parent commits of the current commit
     * @param wasInformedBies
     */
    private void processWasInformedBy(Namespace namespace, String sha, Activity activity, List<Commit> parents, List<WasInformedBy> wasInformedBies) {
        String parentSha;
        for (Commit commit : parents) {
            parentSha = commit.getSha();
            wasInformedBies.add(provFactory.newWasInformedBy(getQualifiedName(namespace, "information-" + parentSha + "-" + sha, PROVENANCE_PREFIX), activity.getId(), getQualifiedName(namespace, "commit-" + parentSha, PROVENANCE_PREFIX)));
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
        return "file-" + filename.replaceAll("[/\\\\. ]", "-");
    }

    /**
     * Provides the sha of the last commit that modified the resource with the filename
     *
     * @param filename       the name of the file
     * @param entityVersions
     * @return the parent commit sha
     */
    private String getParentCommitSha(String filename, HashMap<String, List<String>> entityVersions) {
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
