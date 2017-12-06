package ro.uaic.info.gitprov.config;

import org.apache.log4j.Logger;
import org.eclipse.egit.github.core.client.RequestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;


/**
 * The type Global exception handler.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * The constant logger.
     */
    final static Logger logger = Logger.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle request exception.
     */
    @ResponseStatus(value=HttpStatus.NOT_FOUND)
    @ExceptionHandler(RequestException.class)
    public void handleRequestException(){
    }
}