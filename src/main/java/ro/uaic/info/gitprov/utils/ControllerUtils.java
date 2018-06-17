package ro.uaic.info.gitprov.utils;

import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import ro.uaic.info.gitprov.controllers.ProvController;
import ro.uaic.info.gitprov.controllers.StoreController;

public class ControllerUtils {

    public static String getProvControllerProvenanceNamespace(@PathVariable String owner, @PathVariable String name) {
        return ControllerLinkBuilder.linkTo(ProvController.class).slash("owner").slash(owner).slash(name).toString() + '#';
    }

    public static String getStoreControllerProvenanceNamespace(@PathVariable String owner, @PathVariable String name) {
        return ControllerLinkBuilder.linkTo(StoreController.class).slash("owner").slash(owner).slash(name).toString() + '#';
    }
}
