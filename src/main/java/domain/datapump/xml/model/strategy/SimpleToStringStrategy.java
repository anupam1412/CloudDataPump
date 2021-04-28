package domain.datapump.xml.model.strategy;

import org.jvnet.jaxb2_commons.lang.DefaultToStringStrategy;

public class SimpleToStringStrategy extends DefaultToStringStrategy {

    @Override
    public boolean isUseIdentityHashCode() {
        return false;
    }

    @Override
    protected void appendClassName(StringBuilder buffer, Object object) {
        if (object != null) {
            buffer.append(getShortClassName(object.getClass()));
        }
    }
}
