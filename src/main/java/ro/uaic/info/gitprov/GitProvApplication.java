package ro.uaic.info.gitprov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * The git-prov application.
 */
@SpringBootApplication
@EnableSwagger2
public class GitProvApplication {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GitProvApplication.class, args);
    }
}
