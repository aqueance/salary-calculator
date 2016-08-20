/*
 * Copyright (c) 2016 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
