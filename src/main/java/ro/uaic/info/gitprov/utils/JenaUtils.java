package ro.uaic.info.gitprov.utils;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.resultset.ResultSetWriterRegistry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class JenaUtils {

    private static Map<String, Lang> formatToLangs = new HashMap<>();

    static {
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

        ResultSetWriterRegistry.init();
    }

    public static String getDocumentFromModel(Model model, String contentType) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        RDFDataMgr.write(byteArrayOutputStream, model, formatToLangs.get(contentType));

        return byteArrayOutputStream.toString();
    }

    public static Model buildModelFromString(String document) throws IOException {
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
