package ro.uaic.info.gitprov.utils;

import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import ro.uaic.info.gitprov.controllers.ProvController;

public class ControllerUtils {

    public static String getProvenanceNamespace(@PathVariable String owner, @PathVariable String name) {
        return ControllerLinkBuilder.linkTo(ProvController.class).slash("owner").slash(owner).slash(name).toString() + "#";
    }
}
