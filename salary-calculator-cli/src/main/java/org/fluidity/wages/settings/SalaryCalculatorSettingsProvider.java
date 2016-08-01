package org.fluidity.wages.settings;

import java.io.IOException;
import java.util.Properties;

import org.fluidity.composition.Component;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.spi.PropertyProvider;

@Component
final class SalaryCalculatorSettingsProvider implements PropertyProvider {

    private static final String RESOURCE = "salary-calculator.properties";

    private final Properties properties = new Properties();

    public SalaryCalculatorSettingsProvider() throws IOException {
        this.properties.load(ClassLoaders.findResource(getClass(), RESOURCE).openStream());
    }

    @Override
    public Object property(final String key) {
        return properties.getProperty(key);
    }
}
