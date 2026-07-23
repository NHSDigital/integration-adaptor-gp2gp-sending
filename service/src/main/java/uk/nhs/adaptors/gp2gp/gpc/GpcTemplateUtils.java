package uk.nhs.adaptors.gp2gp.gpc;

import java.io.IOException;
import java.io.StringWriter;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gp2gp.gpc.exception.GpConnectException;

@Slf4j
public class GpcTemplateUtils {

    private static final String TEMPLATES_DIRECTORY = "templates";

    public static Mustache loadTemplate(String templateName) {
        LOGGER.debug("Loading GPC mustache template {}", templateName);
        MustacheFactory mf = new DefaultMustacheFactory(TEMPLATES_DIRECTORY);
        return mf.compile(templateName);
    }

    public static String fillTemplate(Mustache template, Object content) {
        try {
            StringWriter writer = new StringWriter();
            template.execute(writer, content).flush();
            return writer.toString();
        } catch (IOException e) {
            LOGGER.error("Unable to fill GPC mustache template for {}", content.getClass().getSimpleName(), e);
            throw new GpConnectException("Unable to create the JWT token for the Authorization header. Exception: ", e);
        }
    }
}
