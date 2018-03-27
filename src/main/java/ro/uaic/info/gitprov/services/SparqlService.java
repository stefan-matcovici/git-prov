package ro.uaic.info.gitprov.services;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class SparqlService {

    private final Map<String, Lang> formatToLangs = new HashMap<>();

    public SparqlService() {
        formatToLangs.put("text/csv", Lang.CSV);
        formatToLangs.put("application/ld+json", Lang.JSONLD);
        formatToLangs.put("application/n-quads", Lang.NQUADS);
        formatToLangs.put("application/n-triples", Lang.NTRIPLES);
        formatToLangs.put("application/json", Lang.RDFJSON);
        formatToLangs.put("application/sparql-results+thrift", Lang.RDFTHRIFT);
        formatToLangs.put("application/trig", Lang.TRIG);
        formatToLangs.put("text/tab-separated-values", Lang.TSV);
        formatToLangs.put("application/x-turtle", Lang.TURTLE);
        formatToLangs.put("application/rdf+xml", Lang.RDFXML);
        formatToLangs.put("text/plain", Lang.RDFNULL);
    }

    public String getQueryResult(String document, String query, String format) throws IOException {
        Model model = buildModelFromString(document);

        return executeQuery(model, query, format);
    }

    private String executeQuery(Model model, String query, String format) {
        Query qry = QueryFactory.create(query);
        QueryExecution qe = QueryExecutionFactory.create(qry, model);

        ResultSet resultSet = qe.execSelect();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        String result = ResultSetFormatter.asXMLString(resultSet, null);
        qe.close();

        return result;
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
