package org.fluidity.wages.settings;

import java.io.IOException;
import java.util.Properties;

import org.fluidity.composition.Component;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.spi.PropertyProvider;

/**
 * Loads a .properties file from the class path and makes it available to Fluid Tools' <a
 * href="https://github.com/aqueance/fluid-tools/wiki/User%20Guide%20-%20Foundation#configuration">configuration</a> facility.
 */
@Component
final class SalaryCalculatorSettingsProvider implements PropertyProvider {

    private static final String RESOURCE = "salary-calculator.properties";

    private final Properties properties = new Properties();

    public SalaryCalculatorSettingsProvider() throws IOException {
        this.properties.load(ClassLoaders.findResource(getClass(), RESOURCE).openStream());
    }

    @Override
    public String property(final String key) {
        return properties.getProperty(key);
    }
}
