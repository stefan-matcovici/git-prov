package ro.uaic.info.gitprov.services;

import org.apache.commons.io.IOUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class SparqlService {

    public String executeQuery(String document, String query) throws IOException {
        Model model = buildModelFromString(document);
        return null;
    }

    private Model buildModelFromString(String document) throws IOException {
        Model md = ModelFactory.createDefaultModel();
        InputStream in = IOUtils.toInputStream(document, "UTF-8");
        System.out.println(in.available());
        try {
            md.read(in, null, "TTL");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("model size:" + md.size());

        String sparql = "PREFIX prov: <http://www.w3.org/ns/prov#>" +
                "SELECT ?commit WHERE { " +
                "?commit a prov:Activity .}";

        Query qry = QueryFactory.create(sparql);
        QueryExecution qe = QueryExecutionFactory.create(qry, md);

        ResultSet rs = qe.execSelect();

        while (rs.hasNext()) {
            QuerySolution sol = rs.nextSolution();
            RDFNode str = sol.get("commit");

            System.out.println(str.toString());
        }

        qe.close();

        return md;
    }
}
