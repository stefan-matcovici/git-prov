package ro.uaic.info.gitprov.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/")
public class ProvenanceController
{
    @RequestMapping(method = RequestMethod.GET)
    public String test()
    {
        return "It's running";
    }
}
