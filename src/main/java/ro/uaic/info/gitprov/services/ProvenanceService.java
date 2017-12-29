package ro.uaic.info.gitprov.services;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public String repositoryToDocument(Repository repository) {
        return "";
    }
}
