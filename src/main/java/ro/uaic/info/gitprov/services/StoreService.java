package ro.uaic.info.gitprov.services;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.tdb.TDBFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

import static ro.uaic.info.gitprov.utils.JenaUtils.buildModelFromString;
import static ro.uaic.info.gitprov.utils.JenaUtils.getDocumentFromModel;

@Service
public class StoreService {
    public static final String DATABASE_DIRECTORY = "TDBStore";
    @Autowired
    SparqlService sparqlService;
    private Dataset dataset;

    public StoreService() {
        new File(DATABASE_DIRECTORY).mkdir();
        dataset = TDBFactory.createDataset(DATABASE_DIRECTORY);
    }

    public void storeDocument(String documentName, String document) throws IOException {
        Model model = buildModelFromString(document);
        dataset.begin(ReadWrite.WRITE);
        dataset.addNamedModel(documentName, model);
        dataset.commit();
        dataset.end();
    }

    public String getDocument(String namedModel, String contentType) throws IOException {
        String result;
        dataset.begin(ReadWrite.READ);
        if (dataset.containsNamedModel(namedModel)) {
            Model model = dataset.getNamedModel(namedModel);

            result = getDocumentFromModel(model, contentType);
        } else {
            throw new IOException("Repository not stored!");
        }
        dataset.end();

        return result;
    }
}
