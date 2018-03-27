package ro.uaic.info.gitprov.services;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class SparqlService {

    public String getQueryResult(String document, String query) throws IOException {
        Model model = buildModelFromString(document);

        return executeQuery(model, query);
    }

    private String executeQuery(Model model, String query) {
        Query qry = QueryFactory.create(query);
        QueryExecution qe = QueryExecutionFactory.create(qry, model);

        ResultSet rs = qe.execSelect();

        StringBuilder solutionBuilder = new StringBuilder();
        while (rs.hasNext()) {
            QuerySolution sol = rs.nextSolution();

            solutionBuilder.append(sol.toString()).append("\n");
        }

        qe.close();
        return solutionBuilder.toString();
    }

    private Model buildModelFromString(String document) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        InputStream inputStream = IOUtils.toInputStream(document, "UTF-8");

        try {
            model.read(inputStream, null, "TTL");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return model;
    }
}
