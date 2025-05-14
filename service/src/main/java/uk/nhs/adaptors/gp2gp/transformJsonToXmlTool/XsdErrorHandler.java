package uk.nhs.adaptors.gp2gp.transformjsontoxmltool;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class XsdErrorHandler implements ErrorHandler {

    private final List<SAXParseException> exceptions = new ArrayList<>();

    public boolean isValid() {
        return exceptions.isEmpty();
    }

    @Override
    public void warning(SAXParseException exception) {
        exceptions.add(exception);
    }

    @Override
    public void error(SAXParseException exception) {
        exceptions.add(exception);
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXParseException {
        exceptions.add(exception);
        throw exception;
    }
}
