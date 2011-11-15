package org.apache.stanbol.reasoners.web.input.provider.impl;

import java.io.File;


public class FileInputProvider extends UrlInputProvider {

    public FileInputProvider(File file) {
        super(file.toURI().toString());
    }

}
