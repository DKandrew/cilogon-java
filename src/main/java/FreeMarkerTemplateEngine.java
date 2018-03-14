/*
 * Copyright 2013 - Per Wendel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import spark.ModelAndView;
import spark.TemplateEngine;

/**
 * FreeMarker configuration can be set with the {@link FreeMarkerTemplateEngine#setConfiguration(Configuration)} method.
 * If no configuration is set the default configuration will be used where ftl
 * files need to be put in directory resources/template/freemarker.
 *
 * @author Alex
 * @author Per Wendel
 */
public class FreeMarkerTemplateEngine extends TemplateEngine {

    /**
     * The FreeMarker configuration
     */
    private Configuration configuration;

    /**
     * Creates a FreeMarkerEngine
     */
    public FreeMarkerTemplateEngine() {
        this.configuration = createDefaultConfiguration();
    }

    /*
    Constructor: user-defined template path
    Input:
        path - the path of folder where the .ftl files are
     */
    public FreeMarkerTemplateEngine(String path) {
        this.configuration = createConfiguration(path);
    }

    /**
     * Creates a FreeMarkerEngine with the specified configuration
     *
     * @param configuration The Freemarker configuration
     */
    public FreeMarkerTemplateEngine(Configuration configuration) {
        this.configuration = configuration;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String render(ModelAndView modelAndView) {
        try {
            StringWriter stringWriter = new StringWriter();
            Template template = configuration.getTemplate(modelAndView.getViewName());
            template.process(modelAndView.getModel(), stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Sets FreeMarker configuration.
     * Note: If configuration is not set the default configuration will be used.
     *
     * @param configuration the configuration to set
     */
    @Deprecated // use constructor
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    private Configuration createDefaultConfiguration() {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_27);
        configuration.setClassForTemplateLoading(FreeMarkerTemplateEngine.class, "");
        return configuration;
    }

    /*
    Set the configuration given user-defined directory
     */
    private Configuration createConfiguration(String path) {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_27);
        try{
            configuration.setDirectoryForTemplateLoading(new File(path));
        }
        catch (IOException e){
            System.out.println("FreemarkerTemplateEngine: Cannot find the template directory " + path +"! Error:" + e);
        }
        return configuration;
    }
}