package uk.nhs.adaptors.gp2gp.common.validation;

import java.util.ArrayList;
import java.util.List;

public class ValidatedProperties
{
    public List<String> invalidProperties;
    public List<String> missingProperties;

    public ValidatedProperties()
    {
        invalidProperties = new ArrayList<String>();
        missingProperties = new ArrayList<String>();
    }
}