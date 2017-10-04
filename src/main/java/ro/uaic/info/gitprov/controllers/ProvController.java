package ro.uaic.info.gitprov.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ProvController
{
    @RequestMapping("/")
    @ResponseBody
    String home()
    {
        return "Hello World!";
    }
}
